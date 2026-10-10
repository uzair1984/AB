# AB AgentX — Product Architecture Baseline

**Status:** Decision baseline for implementation planning; not a claim of working caller-facing AI audio.  
**Last updated:** 2026-10-10

## 1. Product vision (must not be narrowed)

AB AgentX is a global, Android-first AI Agentic Call Assistant and Real-Time Multilingual Interpreter. It should help users manage unavailable/missed calls, understand callers speaking other languages, have two-way interpreted conversations, capture caller intent, and deliver useful summaries/notifications/CRM records. It is not merely an auto-answer demo or a single-language interpreter.

Product targets include:
- Incoming ordinary phone calls, with the caller using any phone, SIM, carrier or country; the caller must not need to install AB.
- Automatic caller-language detection and two-way speech interpretation (e.g. caller speaks Chinese, user speaks Urdu, each hears the other in their own language).
- Assistant/agent conversation for unavailable users, caller request capture, call summary and notification, with caller-language CRM summary.
- Broad language coverage (launch target 50+ languages), configurable male/female voice where the selected speech engine actually supports it.
- Android-first user experience, with future web/desktop/admin and additional communication channels kept possible.
- Open-source-first and zero-recurring-cost-conscious design. Never describe it as guaranteed zero-cost if real-time inference, cloud telephony, forwarding, data or carrier fees apply.

## 2. Caller vs. app-user compatibility

These are two different compatibility questions:
1. **Caller side:** arbitrary external phone, SIM, carrier and country. No AB installation or special caller hardware should be required.
2. **AB-installed side:** the customer's Android device, OS version, manufacturer, dialer role and carrier. These affect whether the app can control the cellular call and access its audio.

Phone A and Phone B are test labels only, not product architecture. A51 is one possible qualification device, not the product's target boundary. A successful test on one model does not establish support for every Android model.

## 3. Locked modes (preserve exactly)

- **ACTIVE:** user manually enables AB; the desired behavior is immediate handling of incoming calls.
- **NORMAL:** the phone rings normally first; AB takes over after the user-configured delay (current target about 20 seconds / 4–5 rings, but carrier and device behavior must be tested).
- **OFF:** AB is inactive and normal phone behavior remains in effect.

The UI must show real state, not optimistic state. "Online", "AI audio ready" or "Enabled as phone app" may be shown only after the corresponding platform role, call route and media path have been verified. The default-dialer role alone does not grant raw cellular call audio.

## 4. Core technical constraint

A standard third-party Android app cannot rely on a universal, public API that supplies both directions of raw PCM audio for an ordinary SIM cellular call. Android Telecom can expose call lifecycle/control, but it does not automatically let an app inject synthesized speech into the remote caller's uplink or read remote audio. Local Text-to-Speech on the phone is not evidence that the caller can hear AB. A normal default-dialer permission is not a fix for this.

Therefore, do not build more UI around a fake caller-facing audio path, and do not release an APK claiming two-way AI voice until a real remote caller hears AB and AB receives caller speech.

## 5. Product-wide transport decision: compare and qualify routes

The media transport must be a replaceable subsystem; the app's user experience and conversation engine must not be coupled to one phone model.

### Route A — Carrier/PSTN forwarding to a cloud telephony/voice agent
- **Strength:** caller can use any ordinary phone/SIM/network; caller needs no app. Full-duplex media is handled by the telephony service after the call is routed there.
- **Trade-offs:** depends on supported countries/numbers, forwarding availability, regulatory requirements, service and carrier charges, and reliable routing configuration. It may change when or where the user's handset rings. This is the strongest general-purpose route to qualify for a broad consumer product, but it conflicts with a literal promise of zero recurring costs.
- **Mode mapping to validate:** ACTIVE can route/forward immediately; NORMAL can use conditional forwarding/no-answer where the carrier supports it; OFF must restore normal routing. These controls are carrier-specific and must never be toggled silently or represented as successful before confirmation.

### Route B — Direct native SIM-call media on the Android handset
- **Strength:** potentially keeps the call on the user's SIM and may avoid forwarding fees.
- **Trade-offs:** requires device/OS/manufacturer-specific privileged capability or a qualified hardware/firmware path. It is not generally available to ordinary Play-distributed apps and must be qualified per supported device family. Rooting or privileged modules are not a universal consumer solution.
- **Use:** investigate as an optional device-qualified adapter, never assume it works on every Android phone.

### Route C — Local PC/audio hardware bridge
- **Strength:** useful for lab experiments and for proving the conversation pipeline against real audio without pretending Android has a public raw-cellular PCM API.
- **Trade-offs:** requires a computer, audio hardware/routing, setup and likely per-device validation. It is a prototype/advanced integration, not the default global consumer architecture unless product requirements explicitly accept those dependencies.

### Route D — AB app-to-app VoIP
- **Strength:** a real SIP/WebRTC stack can provide two-way media for calls made through AB VoIP.
- **Trade-offs:** does not automatically intercept an ordinary incoming SIM call. If callers must dial the user's ordinary number from any phone, PSTN/carrier ingress or forwarding is still needed.

**Architecture policy:** keep all four routes behind a replaceable CallTransport/media-adapter boundary. Qualify Route A as the general compatibility path; investigate Route B for cost-conscious device-qualified use; use Route C only as a controlled feasibility prototype; use Route D for an explicit future AB VoIP channel. Do not state a route is implemented until its signaling, media, billing, failure and recovery paths exist and tests pass.

## 6. Target logical architecture

    Any caller phone / SIM / carrier / country (no AB app)
                             |
                 ordinary PSTN / carrier route
                             |
            [Qualified ingress / forwarding adapter]
                             |
              authenticated call session + duplex media
                             |
      [Transport boundary] <----> [Speech pipeline]
            |                    VAD / streaming STT
            |                    auto language identification
            |                    turn-taking / echo handling
            |                    translation + agent policy
            |                    streaming TTS in target language
            |                    bounded queues / interruption
            |
      [Android AB AgentX]
       call state, ACTIVE/NORMAL/OFF policy, UI, caller identity,
       user consent, voice/language preferences, notifications
            |
      [Session + CRM service, optional/local-first]
       summaries, caller-language notes, action items, history

The Android app, call transport and speech/agent engine are separate modules. Use explicit session states such as IDLE, RINGING, ROUTING, MEDIA_CONNECTING, AI_READY, DEGRADED, and ENDED. AI_READY requires verified inbound and outbound media, not merely a connected socket or an answered call.

## 7. Speech pipeline requirements

- Stream caller audio into VAD/STT; do not use hard-coded stub text.
- Detect the caller's language automatically and allow correction/fallback when confidence is low.
- Translate or reason in a canonical internal representation while preserving intent and names.
- Generate speech in the caller's language and route it through the actual call transport.
- Support barge-in, interruption, timeouts, partial transcripts, bounded queues, backpressure, retry and deterministic cleanup.
- Save a caller-language summary and, where useful, a user-language summary; protect personal data and offer retention controls.
- Provider adapters must be replaceable. Local/open-source options should be evaluated first, but latency, device compute, language quality and cloud costs must be measured honestly.
- Male/female voice setting must select a verified available voice; if unavailable, disclose fallback rather than pretending the requested voice is active.

## 8. UI/product requirements

- Preserve the supplied AB robot image as-is for the app icon; never substitute an invented robot.
- Professional agentic visual design, dialpad and clear ACTIVE/NORMAL/OFF controls with visible on/off sliders; vertical scrolling on smaller screens.
- No redundant global PHONE ON/OFF control that conflicts with the three modes.
- “Enable AB as Phone App” must launch the real Android role/permission flow and reflect actual state; label and status update only after verified success.
- Show the currently selected transport and capability status in understandable language. Do not show “Online” if only UI setup is complete.
- Include a working outbound-call cancel/end control and reliable in-call hang-up.
- Respect user choice, Android role requirements, call notifications and permissions.

## 9. Cost and privacy guardrails

- No paid provider is mandatory until an explicit provider decision; compare local/open-source and hosted options.
- Distinguish free/open-source software from paid compute, phone numbers, PSTN minutes, forwarding, data and API quotas.
- No secrets in source code, logs or committed config. Encrypt transport sessions, minimize retained audio, protect call summaries and explain third-party processing.
- Any carrier-forwarding configuration must be user-authorized, reversible and verified; provide clear status and rollback instructions.

## 10. Engineering sequence and release gates

1. Finish architecture decision and capability matrix by Android version/manufacturer/carrier and target countries.
2. Build a transport feasibility matrix for Route A and Route B; document cost, legal/numbering, supported regions, and ACTIVE/NORMAL/OFF mapping. Run Route C only as a prototype where physical audio validation is possible.
3. Select the first production transport based on global caller compatibility, user experience, cost transparency and operational feasibility—not based solely on the test phone.
4. Implement transport state machine and authenticated media contract before integrating speech providers.
5. Replace all release-path stubs with actual streaming speech/language/agent/TTS adapters.
6. Add automated tests for modes, state transitions, timeouts, errors, repeated calls, permission denial, no-answer forwarding, transport loss and cleanup.
7. Hardware/network acceptance: callers on multiple independent phones/carriers can hear AB; AB receives their speech; two-way interpreted turns work; interruption/echo tested; call routing restored in OFF; failure states truthful.
8. Test UI scrolling, exact robot icon, dialer role state, voice selection, outbound cancel and end-call.
9. Publish APK only after build artifact checks and relevant functional tests. Clearly separate CI/build success from real-world media validation.

## 11. Current implementation status (known limitations)

- Existing Android call-control code can answer calls according to ACTIVE/NORMAL timing, but that does not mean it can provide remote caller-facing speech.
- Existing TTS greeting is explicitly local-only; no universal raw cellular PCM path is implemented.
- Speech recognizer/language detector/agent/TTS stub providers are placeholders, not production AI.
- The AB VoIP Telecom service is lifecycle scaffolding and must reject/fail closed while media transport is missing.
- No hardware qualification or worldwide carrier qualification is implied by a successful Gradle build.

## 12. Non-negotiable acceptance principle

The project may have staged releases, but the product vision must remain intact. Any limitation must be described as a technical/product trade-off, not used to silently shrink the vision. Never mark caller-facing AI audio complete until a real remote caller has heard it and AB has received the caller's speech through the chosen transport.
