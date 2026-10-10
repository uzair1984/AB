# AB AgentX — Deep Research: How to Get Real Two-Way Call Audio

**Research date:** 2026-10-10  
**Purpose:** stop repeating Android-only fixes that cannot provide remote caller audio. This is a technical feasibility decision, not a claim that a production transport is already deployed.

## Executive finding

The immediate blocker is not the TTS voice, answer delay, UI or phone model. It is the **media path**. The current app answers a carrier call and invokes Android TTS locally, but the remote party's uplink/downlink media is not exposed as a general-purpose PCM stream to an ordinary Android app. A default-dialer role lets AB own the calling UI and call lifecycle; it does not create a bidirectional cellular audio API.

Official Android sources:
- Default phone app requirements: https://developer.android.com/develop/connectivity/telecom/dialer-app
- Telecom framework overview: https://developer.android.com/develop/connectivity/telecom
- InCallService API: https://developer.android.com/reference/android/telecom/InCallService
- Android 15 compatibility definition (voice capture restriction): https://source.android.com/docs/compatibility/15/android-15-cdd

The current repository confirms this distinction:
- ABInCallService.kt logs cellular_tts_local_only=true raw_cellular_pcm=false.
- LocalStubProviders.kt returns fixed placeholder recognition, language and agent responses; the synthesizer returns text bytes, not encoded voice audio.
- ABVoipConnectionService.kt deliberately rejects answering while media transport is missing.
- ConversationOrchestrator.kt can coordinate interfaces, but there is no production audio source feeding it and no media sink delivering synthesized audio to the caller.

So the existing “answer + local greeting” route cannot be fixed into universal cellular audio merely by adding permissions or changing TTS settings.

## Architecture routes compared

### A. Carrier/PSTN ingress + duplex media service (recommended general compatibility route to qualify)

A caller dials a supported number or the customer's ordinary number is forwarded/routed to a telephony ingress. That ingress provides the call's media to an AI session, and the app receives session state and summaries.

Evidence that the media mechanism exists: Twilio Media Streams documents bidirectional WebSocket audio: inbound caller audio is received and outbound generated audio can be sent back into the call. The bidirectional mode uses Connect/Stream; outbound audio must be μ-law, 8 kHz, base64 encoded, and the server must validate Twilio's signature.
- Overview: https://www.twilio.com/docs/voice/media-streams
- WebSocket protocol and audio payload requirements: https://www.twilio.com/docs/voice/media-streams/websocket-messages

**What this proves:** server-side two-way media is technically viable once a call is actually on that telephony platform.

**What it does not prove:** that the service supports the customer's existing Pakistani mobile number, every target country, every forwarding behavior, or zero cost. Number availability, regulatory restrictions, caller-ID behavior, forwarding support, per-minute charges and regional latency must be verified before selecting a provider. Carrier forwarding is a product decision and must be explicit, reversible, and user-authorized.

**Mode implications:**
- ACTIVE: enable a verified routing/forwarding rule for immediate handling, if the chosen carrier/provider supports it.
- NORMAL: use conditional no-answer forwarding when supported; exact timeout/ring count is carrier-dependent and must be tested.
- OFF: remove/disable the AB route and verify normal calls return to the user's usual phone path.
- Never promise that these modes work across all carriers until the carrier matrix proves it.

### B. Direct audio on ordinary SIM calls from Android

This is not a viable universal public-API plan. The official Android compatibility documentation describes restrictions on call audio capture and privileged capture permissions. InCallService is a call UI/control surface, not a raw bidirectional PCM stream.

Use only as an optional, device-qualified adapter if a manufacturer/system-integrator/privileged deployment path is available. Rooting or a privileged module is not an acceptable universal requirement for mainstream users. A successful test on one rooted/modified handset would not establish general Android compatibility.

### C. PC / hardware audio bridge

Useful for a controlled lab proof of the speech pipeline if a supported physical audio route can deliver caller audio to the host and return generated speech. It adds a PC, cables/Bluetooth audio routing and per-device setup. It may help investigate, but it is not the default phone-only consumer architecture and should not be confused with a software-only solution.

### D. SIP/WebRTC through AB VoIP

A real SIP/WebRTC implementation can supply duplex media for calls placed/received through that VoIP system. Android Telecom ConnectionService is lifecycle integration, not the SIP/RTP media stack itself. This route can be an additional AB channel, but it does not automatically intercept incoming ordinary SIM calls. To preserve “any caller can dial my normal number,” a PSTN ingress/trunk/forwarding route is still needed.

Reference concepts:
- Android Telecom framework: https://developer.android.com/develop/connectivity/telecom
- Asterisk ARI External Media (only for calls bridged into Asterisk): https://docs.asterisk.org/Development/Reference-Information/Asterisk-Framework-and-API-Examples/External-Media-and-ARI/

## Recommended decision

1. **Stop trying to make Android local TTS speak into an ordinary cellular call.** The current platform API does not provide the needed universal media path.
2. Keep the Android app as the user-facing control plane: ACTIVE/NORMAL/OFF preferences, consent, setup, call state, notifications, voice/language settings and CRM/history.
3. Build the voice session as a separate media plane with a replaceable transport interface. The first production candidate to qualify is PSTN/carrier ingress with bidirectional media because it is compatible with arbitrary caller phones without requiring the caller to install AB.
4. In parallel, assess the exact user's country/number/carrier options and actual total costs. Do not commit to a vendor until Pakistan/target-country number support, inbound routing, outbound/forwarding behavior, fees, data residency, terms and exit strategy are verified.
5. Keep direct SIM audio as a device-specific research track, not a dependency for the global product.
6. Keep AB VoIP as a separate future channel; don't pretend it solves ordinary SIM call interception.
7. Only after a real media transport is chosen should the team connect streaming STT, automatic language detection, translation/agent orchestration and streaming TTS. The current stub providers must not be used in a release path.

## Minimum media contract

A transport adapter must expose:
- verified inbound audio frames from the remote caller;
- a way to send generated audio frames to the remote caller;
- call/session ID, caller/callee identity when available, timestamps and codec/sample-rate metadata;
- connected/disconnected/error callbacks and heartbeat/timeout behavior;
- authenticated sessions, bounded queues, cancellation and cleanup;
- an explicit capability result (SUPPORTED, UNSUPPORTED, NOT_CONFIGURED, FAILED) rather than a boolean that conflates “socket exists” with “audio works.”

The speech pipeline must not enter AI_READY until both inbound and outbound media are confirmed. TTS preview on the handset is only a local voice preview.

## Proof-of-function test (before saying done)

1. A second phone from an independent carrier calls through the selected route.
2. AB receives actual caller audio; logs show real inbound frames, not placeholder strings.
3. Caller hears synthesized AB audio through the call, not the phone speaker.
4. Complete a two-way exchange with at least two languages, then reverse the direction.
5. Verify interruption/barge-in, silence/noise, timeout, reconnect, call end, repeated calls and no stale audio crossing sessions.
6. Verify ACTIVE/NORMAL/OFF routing and restoration behavior with that carrier.
7. Verify a failure leaves the user with a clear state and safe fallback, never a false ONLINE/AI_READY label.
8. Repeat with multiple caller phones/carriers and multiple supported Android models before claiming broad compatibility.

## Current status

- Research-backed finding: **the blocker is the missing cellular media transport**, not an isolated TTS or permission bug.
- Candidate mechanism with documented duplex audio: telephony-provider media streams after calls enter that provider.
- Not yet verified: specific provider/number support for the user's ordinary Pakistani mobile number, carrier forwarding behavior, full cost, live server deployment, or real end-to-end audio.
- No APK should be called fully functional until the proof-of-function test passes.
