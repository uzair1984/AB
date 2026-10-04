# AB AgentX — Master Directory

## Vision

AB AgentX is an AI Agentic Call Assistant + Real-Time Multilingual Interpreter.

## Product identity

- Product: **AB**
- Project / vision: **AB AgentX**
- Initial platform: **Android**
- Future platforms: Web/dashboard and Mac/desktop
- Architecture philosophy: open-source-first, zero recurring-cost objective, provider-independent

## Primary capabilities

### A. AI Call Assistant

When the user is unavailable, AB can handle an incoming call, identify the caller's need, communicate appropriately, and return a useful summary to the user.

### B. Active Mode

When enabled, AB is ready to handle the call immediately, subject to Android's supported call-control mechanisms.

### C. Automatic language handling

The caller should not need to select a language manually. AB should identify the language from the conversation and respond in the appropriate language.

### D. Real-time interpreter

Two people can speak different languages and AB acts as the intermediary in both directions.

Target: **50+ languages**, with quality and availability validated per language.

## Architecture principles

1. Android-first.
2. Feasibility before feature expansion.
3. Provider-independent interfaces.
4. Local/open-source processing where practical.
5. Optional cloud services behind replaceable adapters.
6. Minimal data retention.
7. Explicit permissions and user control.
8. Modular components.
9. Test on real devices early.
10. No recurring-cost dependency should become mandatory for the core architecture.

## Development phases

### Phase 0 — Foundation
- Repository setup
- Architecture baseline
- Decision log
- Android feasibility

### Phase 1 — Call foundation
- Android project
- Call state machine
- Supported call-role integration
- Permissions
- Basic call session UI
- Real-device test

### Phase 2 — Voice foundation
- Audio pipeline
- STT interface
- Language detection
- TTS interface
- Turn management

### Phase 3 — Agent
- AB personality/system policy
- Intent extraction
- Conversation memory per call
- Safety/fallback behavior
- Structured caller summary

### Phase 4 — Multilingual assistant
- Automatic language detection
- Translation
- Multilingual response generation
- CRM-style summary

### Phase 5 — Real-time interpreter
- Two-direction streaming pipeline
- Speaker/turn separation
- Low-latency translation
- 50+ language capability matrix

### Phase 6 — Productization
- Settings
- Call history
- Analytics
- Privacy controls
- Error reporting
- UI polish

### Phase 7 — Release
- Android release preparation
- Play Store compliance
- Optional web dashboard
- Mac/desktop strategy

## Current gate

**Do not move past Phase 0/Phase 1 until Android call-control feasibility is demonstrated on a real device.**

## Decision log

### D-001 — Android-first
Reason: the original product depends on handling real incoming cellular calls.

### D-002 — Provider independence
Reason: preserve the zero-recurring-cost/open-source-first objective and prevent vendor lock-in.

### D-003 — Feasibility-first
Reason: call control and audio access are foundational constraints; proving them early prevents wasted implementation.

## Immediate next actions

1. Create the Android project skeleton.
2. Select a minimum supported Android version based on the APIs required by the call-control prototype.
3. Implement a basic call/session state model.
4. Build a diagnostic screen for permissions, call role, and runtime state.
5. Test on a physical Android device.
6. Record findings in the decision log.
