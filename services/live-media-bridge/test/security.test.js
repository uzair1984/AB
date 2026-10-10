import test from 'node:test';
import assert from 'node:assert/strict';
import { createHmac } from 'node:crypto';
import twilio from 'twilio';

test('Twilio request validation accepts only a signature for the exact public WSS URL', () => {
  const authToken = 'test-auth-token';
  const publicUrl = 'wss://agentx.example.test/media';
  const signature = createHmac('sha1', authToken).update(publicUrl).digest('base64');

  assert.equal(twilio.validateRequest(authToken, signature, publicUrl, {}), true);
  assert.equal(twilio.validateRequest(authToken, signature, 'wss://attacker.example.test/media', {}), false);
});
