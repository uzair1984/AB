import test from 'node:test';
import assert from 'node:assert/strict';
import { spawn } from 'node:child_process';
import { createHmac } from 'node:crypto';
import net from 'node:net';
import { setTimeout as delay } from 'node:timers/promises';

async function freePort() {
  const server = net.createServer();
  await new Promise((resolve, reject) => server.listen(0, '127.0.0.1', resolve).once('error', reject));
  const port = server.address().port;
  await new Promise(resolve => server.close(resolve));
  return port;
}

test('health endpoint never claims live media is verified from config alone', async t => {
  const port = await freePort();
  const child = spawn(process.execPath, ['server.js'], {
    cwd: new URL('..', import.meta.url),
    env: { ...process.env, PORT: String(port), OPENAI_API_KEY: 'test-only-not-a-real-key', AB_AGENTX_STREAM_TOKEN: 'test-only-token', TWILIO_AUTH_TOKEN: 'test-only-twilio-token', PUBLIC_WSS_URL: 'wss://example.invalid/media', PUBLIC_VOICE_URL: 'https://example.invalid/voice' },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  let output = '';
  child.stdout.on('data', chunk => { output += chunk.toString(); });
  child.stderr.on('data', chunk => { output += chunk.toString(); });
  t.after(() => { if (child.exitCode === null) child.kill('SIGTERM'); });

  let response;
  for (let attempt = 0; attempt < 40; attempt++) {
    try {
      response = await fetch(`http://127.0.0.1:${port}/healthz`);
      break;
    } catch {
      if (child.exitCode !== null) throw new Error(`server exited early: ${output}`);
      await delay(100);
    }
  }
  assert.ok(response, `server did not start: ${output}`);
  assert.equal(response.status, 200);
  const health = await response.json();
  assert.equal(health.processAlive, true);
  assert.equal(health.configurationPresent, true);
  assert.equal(health.liveMediaVerified, false);

  const invalidWebhook = await fetch(`http://127.0.0.1:${port}/voice`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded', 'x-twilio-signature': 'invalid-signature' },
    body: 'CallSid=CA-test&From=%2B15550000000&To=%2B15551111111'
  });
  assert.equal(invalidWebhook.status, 403);

  const publicVoiceUrl = 'https://example.invalid/voice';
  const params = { CallSid: 'CA-test', From: '+15550000000', To: '+15551111111' };
  const signedPayload = publicVoiceUrl + Object.keys(params).sort().map(key => key + params[key]).join('');
  const signature = createHmac('sha1', 'test-only-twilio-token').update(signedPayload).digest('base64');
  const validWebhook = await fetch(`http://127.0.0.1:${port}/voice`, {
    method: 'POST',
    headers: { 'content-type': 'application/x-www-form-urlencoded', 'x-twilio-signature': signature },
    body: new URLSearchParams(params).toString()
  });
  assert.equal(validWebhook.status, 200);
  assert.ok((await validWebhook.text()).includes('<Connect><Stream url="wss://example.invalid/media">'));
});
