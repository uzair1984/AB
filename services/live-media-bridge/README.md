# AB AgentX live media bridge — qualification prototype

This backend proof-of-concept bridges Twilio Media Streams audio to the OpenAI Realtime API. It is separate from the Android app: it does not claim Android local TTS is audible to the remote cellular caller.

## Scope
- Handles Twilio `connected`, `start`, `media`, `mark`, and `stop` events.
- Configures bidirectional G.711 μ-law audio (`g711_ulaw`) and automatic turn detection.
- Forwards caller audio to Realtime and returned audio deltas to the same Twilio stream.
- `/healthz` stays not-ready until `OPENAI_API_KEY` is set.
- Required `AB_AGENTX_STREAM_TOKEN` gate on the media WebSocket.

## Run locally
Requires Node.js 22+.

```sh
npm install
npm test
OPENAI_API_KEY=... AB_AGENTX_STREAM_TOKEN='use-a-long-random-secret' npm start
```

Do not put credentials in source control. Deploy behind a public TLS WebSocket endpoint (`wss://`) and configure a Twilio Voice webhook with a `<Connect><Stream>` route:

```xml
<Response>
  <Connect>
    <Stream url="wss://YOUR_HOST/media?token=YOUR_LONG_RANDOM_SECRET" />
  </Connect>
</Response>
```

## Release gates / limitations
- This is a backend prototype, **not an APK fix** and not wired to Android cellular call handling.
- A provider account, usable inbound route, forwarding, secrets and live call are required. Pakistani number availability and carrier forwarding are not proven by this code.
- Twilio signature validation, rate limits, operational metrics, transcript-retention policy and a live 60-second two-way audio test are still required before production.
- A successful unit test or APK build is not proof that a real caller hears the agent. Do not show AI audio as online until both audio directions are verified.
- Realtime API, telephony and hosting can incur charges; this is not a zero-recurring-cost claim.
