import http from 'node:http';
import { WebSocketServer, WebSocket } from 'ws';
import { buildSessionUpdate, parseTwilioMessage, twilioMediaFrame } from './protocol.js';

const PORT = Number(process.env.PORT || 8080);
const API_KEY = process.env.OPENAI_API_KEY;
const REALTIME_MODEL = process.env.OPENAI_REALTIME_MODEL || 'gpt-realtime-2.1';
const DEFAULT_VOICE = process.env.AB_AGENTX_VOICE || 'alloy';
const AUTH_TOKEN = process.env.AB_AGENTX_STREAM_TOKEN;

const server = http.createServer((req, res) => {
  if (req.url === '/healthz') {
    const ready = Boolean(API_KEY);
    res.writeHead(ready ? 200 : 503, { 'content-type': 'application/json' });
    res.end(JSON.stringify({ service: 'ab-agentx-live-media-bridge', ready, reason: ready ? 'api_key_configured' : 'OPENAI_API_KEY_missing' }));
    return;
  }
  res.writeHead(404, { 'content-type': 'application/json' });
  res.end(JSON.stringify({ error: 'not_found' }));
});

const wss = new WebSocketServer({ noServer: true, maxPayload: 1024 * 1024 });
server.on('upgrade', (req, socket, head) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`);
  if (url.pathname !== '/media') return socket.destroy();
  if (!AUTH_TOKEN || url.searchParams.get('token') !== AUTH_TOKEN) return socket.destroy();
  if (!API_KEY) return socket.destroy();
  wss.handleUpgrade(req, socket, head, ws => wss.emit('connection', ws, req));
});

wss.on('connection', (twilio) => {
  let streamSid = null;
  let openai = null;
  let started = false;
  let closed = false;
  const pendingAudio = [];
  const log = (event, extra = {}) => console.log(JSON.stringify({ at: new Date().toISOString(), event, streamSid, ...extra }));

  function closeBoth(code = 1000, reason = 'session ended') {
    if (closed) return;
    closed = true;
    try { if (openai && openai.readyState < WebSocket.CLOSING) openai.close(code, reason); } catch {}
    try { if (twilio.readyState < WebSocket.CLOSING) twilio.close(code, reason); } catch {}
  }
  function sendTwilio(frame) {
    if (twilio.readyState === WebSocket.OPEN) twilio.send(JSON.stringify(frame));
  }
  function connectRealtime() {
    if (openai || started) return;
    started = true;
    const endpoint = `wss://api.openai.com/v1/realtime?model=${encodeURIComponent(REALTIME_MODEL)}`;
    openai = new WebSocket(endpoint, { headers: { Authorization: `Bearer ${API_KEY}`, 'OpenAI-Beta': 'realtime=v1' }, maxPayload: 8 * 1024 * 1024 });
    openai.on('open', () => {
      openai.send(JSON.stringify(buildSessionUpdate({ voice: DEFAULT_VOICE })));
      log('realtime_connected', { model: REALTIME_MODEL });
      while (pendingAudio.length && openai.readyState === WebSocket.OPEN) {
        openai.send(JSON.stringify({ type: 'input_audio_buffer.append', audio: pendingAudio.shift() }));
      }
    });
    openai.on('message', raw => {
      let event;
      try { event = JSON.parse(raw.toString()); } catch { log('realtime_invalid_json'); return; }
      switch (event.type) {
        case 'session.updated': log('session_configured'); break;
        case 'input_audio_buffer.speech_started':
          sendTwilio({ event: 'clear', streamSid });
          log('caller_speech_started');
          break;
        case 'conversation.item.input_audio_transcription.completed':
          log('caller_transcript', { text: (event.transcript || '').slice(0, 400), language: 'auto' });
          break;
        case 'response.output_audio.delta':
          if (streamSid && event.delta) sendTwilio(twilioMediaFrame(streamSid, event.delta));
          break;
        case 'response.output_audio.done':
          log('agent_audio_complete');
          if (streamSid) sendTwilio({ event: 'mark', streamSid, mark: { name: `agent-audio-${Date.now()}` } });
          break;
        case 'response.done': log('agent_response_complete', { status: event.response?.status }); break;
        case 'error': log('realtime_error', { code: event.error?.code, message: event.error?.message }); break;
        default: break;
      }
    });
    openai.on('error', err => log('realtime_socket_error', { message: err.message }));
    openai.on('close', (code, reason) => {
      log('realtime_disconnected', { code, reason: reason.toString() });
      if (!closed && twilio.readyState === WebSocket.OPEN) closeBoth(1011, 'AI media session disconnected');
    });
  }

  twilio.on('message', raw => {
    let event;
    try { event = parseTwilioMessage(raw.toString()); }
    catch (err) { log('invalid_twilio_event', { message: err.message }); closeBoth(1008, 'invalid media event'); return; }
    if (event.event === 'connected') { log('twilio_connected'); return; }
    if (event.event === 'start') {
      streamSid = event.start.streamSid;
      log('twilio_stream_started', { tracks: event.start.tracks, mediaFormat: event.start.mediaFormat });
      connectRealtime();
      return;
    }
    if (event.event === 'media') {
      if (!started) connectRealtime();
      const payload = event.media.payload;
      if (openai?.readyState === WebSocket.OPEN) openai.send(JSON.stringify({ type: 'input_audio_buffer.append', audio: payload }));
      else if (pendingAudio.length < 30) pendingAudio.push(payload);
      return;
    }
    if (event.event === 'stop') { log('twilio_stream_stopped'); closeBoth(); }
    if (event.event === 'mark') log('twilio_mark', { mark: event.mark?.name });
  });
  twilio.on('error', err => log('twilio_socket_error', { message: err.message }));
  twilio.on('close', () => { log('twilio_disconnected'); closeBoth(); });
});

server.listen(PORT, '0.0.0.0', () => console.log(JSON.stringify({ event: 'server_listening', port: PORT, apiKeyConfigured: Boolean(API_KEY), model: REALTIME_MODEL })));
