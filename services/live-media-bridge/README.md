# AB AgentX live media bridge — qualification prototype

This backend proof-of-concept bridges Twilio Media Streams audio to the OpenAI Realtime API. It is separate from the Android app: it does not claim Android local TTS is audible to the remote cellular caller.

## Scope
- Handles Twilio `connected`, `start`, `media`, `mark`, and `stop` events.
- Configures bidirectional G.711 μ-law audio (`audio/pcmu`) and automatic turn detection.
- Forwards caller audio to Realtime and returned audio deltas to the same Twilio stream.
- Sends an initial AI-disclosed greeting and clears buffered agent audio on caller interruption.
- `GET /healthz` reports process/configuration status and always keeps `liveMediaVerified: false`; credentials alone never mark live audio as verified.
- Validates the `X-Twilio-Signature` header and requires a per-stream token passed via Twilio `customParameters`.

## Run locally
Requires Node.js 22+.

```sh
npm install
npm test
OPENAI_API_KEY=... \
AB_AGENTX_STREAM_TOKEN='use-a-long-random-secret' \
TWILIO_AUTH_TOKEN='your-twilio-auth-token' \
PUBLIC_WSS_URL='wss://YOUR_HOST/media' npm start
```

Do not put credentials in source control. Deploy behind a public HTTPS endpoint and public TLS WebSocket endpoint. Configure the Twilio phone number's Voice webhook as **POST** `https://YOUR_HOST/voice`. Set `PUBLIC_VOICE_URL` to that exact HTTPS URL and `PUBLIC_WSS_URL` to the exact `wss://YOUR_HOST/media` stream URL; no query strings are allowed. The signed `/voice` endpoint generates the TwiML and passes the stream token as a nested `<Parameter>`.

## Container build

Build the image from this directory:

```sh
docker build -t ab-agentx-live-media-bridge .
docker run --rm -p 8080:8080 \
  -e OPENAI_API_KEY \
  -e AB_AGENTX_STREAM_TOKEN \
  -e TWILIO_AUTH_TOKEN \
  -e PUBLIC_WSS_URL \
  -e PUBLIC_VOICE_URL \
  ab-agentx-live-media-bridge
```

The host/reverse proxy must provide valid public HTTPS and WSS, forward `/voice` as HTTP POST and `/media` as WebSocket upgrade, and preserve the exact public URLs in `PUBLIC_VOICE_URL` and `PUBLIC_WSS_URL`. Store secrets in the hosting platform's secret manager; do not bake them into the image. Do not expose the bridge publicly until signatures, limits, and provider routing are configured.

## Release gates / limitations
- This is a backend prototype, **not an APK fix** and not wired to Android cellular call handling.
- A provider account, usable inbound route, forwarding, secrets and live call are required. Pakistani number availability and carrier forwarding are not proven by this code.
- The server validates Twilio's WebSocket signature against `PUBLIC_WSS_URL` and validates the inbound voice webhook signature against `PUBLIC_VOICE_URL`, both using `TWILIO_AUTH_TOKEN`; the stream token is checked against the authenticated `start.customParameters`.
- Rate limits, operational metrics, transcript-retention policy and a live 60-second two-way audio test are still required before production.
- A successful unit test or APK build is not proof that a real caller hears the agent. Do not show AI audio as online until both audio directions are verified.
- Realtime API, telephony and hosting can incur charges; this is not a zero-recurring-cost claim.
