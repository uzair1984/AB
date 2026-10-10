import test from 'node:test';
import assert from 'node:assert/strict';
import { buildSessionUpdate, parseTwilioMessage, twilioMediaFrame } from '../protocol.js';

test('session requests bidirectional G.711 mu-law audio and automatic turn detection', () => {
  const update = buildSessionUpdate({ voice: 'alloy' });
  assert.equal(update.type, 'session.update');
  assert.equal(update.session.audio.input.format.type, 'audio/pcmu');
  assert.equal(update.session.audio.output.format.type, 'audio/pcmu');
  assert.equal(update.session.audio.input.turn_detection.type, 'server_vad');
  assert.match(update.session.instructions, /Detect the caller's spoken language automatically/);
});

test('validates Twilio start and media events', () => {
  assert.equal(parseTwilioMessage(JSON.stringify({ event: 'start', start: { streamSid: 'MZ123' } })).start.streamSid, 'MZ123');
  assert.equal(parseTwilioMessage({ event: 'media', streamSid: 'MZ123', media: { payload: 'abc' } }).media.payload, 'abc');
  assert.throws(() => parseTwilioMessage({ event: 'start', start: {} }), /streamSid/);
  assert.throws(() => parseTwilioMessage({ event: 'media', streamSid: 'MZ123', media: {} }), /payload/);
});

test('formats outbound Twilio audio frame without modifying payload', () => {
  assert.deepEqual(twilioMediaFrame('MZ123', 'AQID'), { event: 'media', streamSid: 'MZ123', media: { payload: 'AQID' } });
  assert.throws(() => twilioMediaFrame('', 'AQID'), /required/);
});
