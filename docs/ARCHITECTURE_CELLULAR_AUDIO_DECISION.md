# AB AgentX — Cellular Call Audio Architecture Decision (Draft)

Date: 2026-10-10  
Status: **Architecture investigation complete; hardware audio qualification NOT RUN**  
Scope: Android-first AB AgentX, with the existing ACTIVE / NORMAL / OFF mode contract preserved.

## Executive decision

Do not try to solve cellular caller audio by adding more local Android Text-to-Speech calls to `ABInCallService`. That speaks on the phone's local audio route; it does not provide a bidirectional PCM path to the remote cellular caller.

Keep the Android app responsible for the dialer experience, call state, mode policy, caller identity, call controls and status UI. Make cellular audio a separate, explicitly qualified transport. Before a new APK is requested, validate the transport independently on the actual Samsung Galaxy A51 (SM-A515F).

## Findings from comparable open-source implementations

### 1. AgentCall

Repository: https://github.com/sidinsearch/AgentCall  
Install/porting guide: https://github.com/sidinsearch/AgentCall/blob/main/docs/INSTALL.md  
Device porting: https://github.com/sidinsearch/AgentCall/blob/main/docs/DEVICE_PORTING.md

AgentCall separates the Android default dialer and privileged telephony audio bridge from a desktop gateway. The bridge handles 20 ms PCM frames in both directions, uses independent receive/transmit paths, checks actual telephony routing and fails closed when the route is not proven. The desktop side hosts policy, speech providers and agent integration.

Important limitation: its current documented, hardware-qualified tuple is a rooted Xiaomi POCO M2 Pro (Qualcomm atoll) on the specified Android 15/Magisk setup. The ordinary APK is explicitly for UI/development validation and does not provide the protected full-duplex cellular audio path. The Samsung A51 is not listed as qualified. Its architecture is a strong reference, not a drop-in library or proof that A51 audio injection works.

License note: AgentCall is AGPLv3; copying or integrating its code may impose obligations. Treat it as an architectural reference unless licensing is reviewed.

### 2. SIMVox-AI

Repository: https://github.com/Magraa/SIMVox-AI

SIMVox separates Android call control (ADB) from desktop audio capture/playback. Its Python desktop process routes audio through PC audio devices and then through a Bluetooth Hands-Free Profile (HFP), TRRS splitter, or virtual audio cable. Speech recognition, the AI response, text-to-speech and SQLite call history run on the desktop.

This avoids requiring a privileged Android audio injector, but it requires a PC, a working two-way audio route and a configured AI provider. Its README describes Gemini API-key setup; “zero VoIP cost” does not mean no API quotas or that the entire system runs standalone on the phone. Hardware performance on the AB test phone remains unverified.

### 3. AB AgentX current repository

The existing app has working call-mode/call-state scaffolding and a local TTS greeting path, but:
- `CallAudioGateway.kt` is only an interface.
- `LocalStubProviders.kt` returns placeholder transcription, language, agent text and UTF-8 bytes rather than live PCM speech.
- `ConversationOrchestrator.kt` is provider-independent coordination, not a live telephony media loop.
- `ABVoipConnectionService.kt` correctly rejects calls while media transport is not implemented.

Therefore successful Android compilation or a call being auto-answered is not proof that a caller can hear AB.

## Architecture target

```text
Cellular network
      ↕
Samsung A51 SIM + Android Telecom
      ↕ call state / answer / end
AB AgentX Android app
      ↕ authenticated control + two-way PCM audio (transport TBD)
Desktop audio gateway (first prototype if available)
      ↕
VAD / streaming STT → language detection → AB agent → translation as needed → TTS
      ↕
bounded PCM playback queue → qualified audio route → remote caller
```

The existing three modes stay unchanged:
- **ACTIVE:** answer immediately, then start the audio gateway only after a transport reports ready.
- **NORMAL:** ring normally; answer after the configured delay, then start the gateway only after ready.
- **OFF:** do not take over the call.

A call must never show “AI audio ready” unless both receive and transmit routes have been verified. If audio transport fails, expose a clear degraded/error state and stop pretending that local TTS reaches the caller.

## Recommended implementation sequence

1. **Transport feasibility spike (no APK rebuild yet):** determine whether the A51 can expose reliable two-way call audio using a PC audio route (Bluetooth HFP or supported USB audio hardware) without rooting. Confirm that the remote caller can hear a test tone/phrase and that the PC receives caller speech. Do not assume speakerphone loopback is reliable.
2. **Build a desktop gateway prototype:** bounded 20 ms PCM frames, explicit device selection, health checks, VAD/STT/TTS adapter interfaces, logs and deterministic cleanup on call end, cable disconnect, app crash and provider failure. Keep secrets out of logs.
3. **Implement a real Android-to-gateway control/audio protocol:** authenticated pairing, session IDs, heartbeat, bounded queues, backpressure, explicit READY/FAILED states and hang-up cleanup. No “connected” status until the gateway and both audio directions are confirmed.
4. **Only if the non-root route fails:** assess an AgentCall-like privileged path as a separate device-porting project. This may require bootloader/root/Magisk and device-specific qualification; do not install another project's privileged module on the user's phone as a shortcut. A51 compatibility must be researched and tested first.
5. **Wire the live speech pipeline:** replace stubs with actual streaming STT, automatic language detection, agent response/translation, and TTS. Start with an offline/local provider where practical; document any external API quotas, latency and privacy implications.
6. **Release gates:** Android build/lint/unit tests; protocol/audio unit tests; physical A51 ↔ second-phone inbound and outbound call tests; verify remote intelligibility both ways, mode timing, end-call, reconnect, repeated calls, and cleanup. Report software tests separately from hardware tests.

## Acceptance checklist (currently NOT RUN)

- [ ] A51 identity/build/Android version recorded.
- [ ] ADB pairing works and is authenticated.
- [ ] Audio input receives the remote caller's voice.
- [ ] Audio output is actually heard by the remote caller.
- [ ] Full-duplex conversation works without feedback or echo.
- [ ] ACTIVE answers immediately and starts audio only when ready.
- [ ] NORMAL rings first and takeover timing is honored.
- [ ] OFF leaves normal phone behavior untouched.
- [ ] End call and repeated-call lifecycle are reliable.
- [ ] Disconnect/failure surfaces a truthful state and releases resources.
- [ ] Live STT → agent → TTS path is tested; no stubs in the release path.
- [ ] UI status, default-dialer role, and caller-facing behavior all verified.

## Decision boundary

**Do not ship another APK claiming to fix caller audio until a real, bidirectional A51 audio transport has passed the physical checks above.** This document records architecture and a test plan; it does not claim that hardware audio has been tested.
