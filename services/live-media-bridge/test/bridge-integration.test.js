import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import { spawn } from 'node:child_process';
import net from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';
import { WebSocket, WebSocketServer } from 'ws';

async function freePort() {
  const server = net.createServer();
  await new Promise((resolve, reject) => server.listen(0, '127.0.0.1', resolve).once('error', reject));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

function waitForMessage(socket, predicate, timeoutMs = 5000) {
  return new Promise((resolve, reject) => {
    const timer = setTimeout(() => { cleanup(); reject(new Error('timed out waiting for WebSocket message')); }, timeoutMs);
    const onMessage = raw => {
      let value;
      try { value = JSON.parse(raw.toString()); } catch { return; }
      if (predicate(value)) { cleanup(); resolve(value); }
    };
    const onClose = () => { cleanup(); reject(new Error('WebSocket closed before expected message')); };
    function cleanup() {
      clearTimeout(timer);
      socket.off('message', onMessage);
      socket.off('close', onClose);
    }
    socket.on('message', onMessage);
    socket.on('close', onClose);
  });
}

test('bridges caller μ-law audio to Realtime and returned audio back to Twilio', async t => {
  const appPort = await freePort();
  const realtimeServer = new WebSocketServer({ port: 0, host: '127.0.0.1' });
  await new Promise(resolve => realtimeServer.once('listening', resolve));
  const realtimePort = realtimeServer.address().port;
  const publicMediaUrl = 'wss://agentx.example.test/media';
  const authToken = 'test-only-twilio-token';
  const streamToken = 'test-only-stream-token';
  let realtimeSocket;
  realtimeServer.on('connection', socket => {
    realtimeSocket = socket;
    socket.on('message', raw => {
      const event = JSON.parse(raw.toString());
      if (event.type === 'session.update') {
        socket.send(JSON.stringify({ type: 'session.updated', session: { type: 'realtime' } }));
      } else if (event.type === 'input_audio_buffer.append') {
        socket.send(JSON.stringify({ type: 'response.output_audio.delta', delta: 'AQIDBA==', response_id: 'resp-test' }));
      }
    });
  });

  const child = spawn(process.execPath, ['server.js'], {
    cwd: new URL('..', import.meta.url),
    env: {
      ...process.env,
      PORT: String(appPort),
      OPENAI_API_KEY: 'test-only-not-a-real-key',
      OPENAI_REALTIME_WS_URL: `ws://127.0.0.1:${realtimePort}`,
      AB_AGENTX_STREAM_TOKEN: streamToken,
      TWILIO_AUTH_TOKEN: authToken,
      PUBLIC_WSS_URL: publicMediaUrl,
      PUBLIC_VOICE_URL: 'https://agentx.example.test/voice'
    },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  let logs = '';
  child.stdout.on('data', chunk => { logs += chunk.toString(); });
  child.stderr.on('data', chunk => { logs += chunk.toString(); });
  t.after(async () => {
    if (child.exitCode === null) child.kill('SIGTERM');
    await new Promise(resolve => realtimeServer.close(resolve));
  });

  let ready = false;
  for (let i = 0; i < 60; i++) {
    try {
      const response = await fetch(`http://127.0.0.1:${appPort}/healthz`);
      if (response.ok) { ready = true; break; }
    } catch {}
    if (child.exitCode !== null) throw new Error(`bridge exited early: ${logs}`);
    await delay(100);
  }
  assert.equal(ready, true, `bridge did not start: ${logs}`);

  const signature = createHmac('sha1', authToken).update(publicMediaUrl).digest('base64');
  const twilioSocket = new WebSocket(`ws://127.0.0.1:${appPort}/media`, {
    headers: { 'x-twilio-signature': signature }
  });
  t.after(() => { if (twilioSocket.readyState < WebSocket.CLOSING) twilioSocket.close(); });
  await new Promise((resolve, reject) => {
    twilioSocket.once('open', resolve);
    twilioSocket.once('error', reject);
  });

  twilioSocket.send(JSON.stringify({ event: 'connected', protocol: 'Call', version: '1.0.0' }));
  twilioSocket.send(JSON.stringify({
    event: 'start',
    start: {
      streamSid: 'MZ-test',
      tracks: ['inbound'],
      mediaFormat: { encoding: 'audio/x-mulaw', sampleRate: 8000, channels: 1 },
      customParameters: { token: streamToken }
    }
  }));

  await waitForMessage(realtimeSocket || await new Promise((resolve, reject) => {
    const until = Date.now() + 5000;
    const poll = async () => {
      while (!realtimeSocket && Date.now() < until) await delay(20);
      if (realtimeSocket) resolve(realtimeSocket); else reject(new Error('Realtime socket was not created'));
    };
    poll();
  }), event => event.type === 'session.updated');

  const callerAudio = 'AAECAwQFBgcICQ==';
  const audioForwarded = waitForMessage(realtimeSocket, event => event.type === 'input_audio_buffer.append' && event.audio === callerAudio);
  twilioSocket.send(JSON.stringify({ event: 'media', streamSid: 'MZ-test', media: { payload: callerAudio } }));
  await audioForwarded;

  const returnedAudio = waitForMessage(twilioSocket, event => event.event === 'media' && event.media?.payload === 'AQIDBA==');
  await waitForMessage(realtimeSocket, event => event.type === 'response.output_audio.delta');
  const mediaFrame = await returnedAudio;
  assert.equal(mediaFrame.streamSid, 'MZ-test');
  assert.equal(mediaFrame.media.payload, 'AQIDBA==');
});
