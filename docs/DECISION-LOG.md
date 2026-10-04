# AB AgentX — Engineering Decision Log

## D-001 — Android-first
**Status:** Accepted

AB AgentX starts with Android because the first product requirement is real incoming cellular call handling.

## D-002 — Architecture before provider selection
**Status:** Accepted

AI, speech, translation and telephony providers remain replaceable until the core architecture and feasibility constraints are validated.

## D-003 — Open-source / zero recurring-cost first
**Status:** Accepted

The design prioritizes local and open-source components where practical. Paid/cloud components remain optional adapters rather than architectural requirements.

## D-004 — Call-control feasibility is the first gate
**Status:** Accepted

The first real-device milestone is proving supported Android call handling and audio behavior before implementing the full voice agent.

## D-005 — 50+ language target
**Status:** Accepted

The multilingual interpreter is designed for 50+ languages. Actual supported languages will be validated through a capability matrix rather than assumed.

## D-006 — Explicit state machine
**Status:** Accepted

Call sessions use explicit states so UI, call control, speech processing, agent reasoning and summaries remain independently testable.

## Open decisions

- Exact Android minimum SDK / target SDK
- Default dialer vs call-screening architecture for the prototype
- Audio capture/routing strategy
- Local STT/TTS models
- Translation engine
- Agent runtime/model
- Optional backend and database
- Web dashboard scope
- Mac architecture
