# AB AgentX — Architecture Baseline

**Status:** Draft for implementation baseline  
**Phase:** 0 — Architecture & feasibility

## 1. Product boundary

AB AgentX has two closely related capabilities:

1. **AI Call Assistant** — handles calls on behalf of the user when the user is unavailable.
2. **Real-Time Multilingual Interpreter** — mediates a live conversation when the two participants speak different languages.

The call assistant is the first implementation target. The interpreter is designed as a core capability, not a separate product.

## 2. Android-first architecture

The Android client is the primary runtime for the first working prototype.

### Logical layers

```
Android UI
   │
   ├── User settings / modes / call history
   │
Call Control Layer
   │
   ├── Incoming call detection
   ├── Ring-state tracking
   ├── Answer / reject / end
   │
Conversation Orchestrator
   │
   ├── Session state
   ├── Turn management
   ├── Agent policy
   └── Tool/action routing
   │
Speech & Language Pipeline
   │
   ├── Speech-to-text
   ├── Language identification
   ├── Translation
   ├── Agent reasoning
   └── Text-to-speech
   │
Persistence / Sync
   │
   ├── Local session data
   └── Optional backend sync
```

## 3. Critical feasibility constraint

Android call handling is the first technical gate.

The prototype must establish what the target Android version/device permits for:

- incoming-call detection
- call screening/default dialer integration where required
- answering and ending calls
- audio routing
- microphone/speaker access
- background execution
- notification and permission behavior

We must not assume that a normal third-party Android application can silently control every cellular-call operation. The implementation will follow Android's supported APIs and user-granted roles/permissions.

## 4. Conversation state machine

The call session will use explicit states:

```
IDLE
  ↓
RINGING
  ↓
WAITING_FOR_AUTO_ANSWER
  ↓
ANSWERING
  ↓
GREETING
  ↓
LISTENING
  ↓
THINKING
  ↓
RESPONDING
  ↓
LISTENING ↔ THINKING ↔ RESPONDING
  ↓
ENDING
  ↓
SUMMARY
  ↓
COMPLETED
```

An interruption/error path can move to `FALLBACK` and then `ENDING`.

## 5. Modes

### Normal Mode

Default behavior:

```
Incoming call
→ observe ring state
→ configurable delay
→ answer when supported
→ AB greeting
→ conversation
→ summary
```

### Active Mode

```
Active Mode ON
→ incoming call
→ answer as soon as supported
→ AB greeting
→ conversation
```

The exact delay and call-control mechanism remain implementation decisions until device/API feasibility is tested.

## 6. Multilingual pipeline

The target pipeline is:

```
Caller speech
→ audio capture
→ language identification
→ speech recognition
→ semantic/agent processing
→ response generation
→ translation to caller language
→ text-to-speech
→ caller audio
```

Interpreter Mode extends this to two independent language directions:

```
Speaker A language
→ STT / language ID
→ translation
→ TTS
→ Speaker B

Speaker B language
→ STT / language ID
→ translation
→ TTS
→ Speaker A
```

The target is 50+ languages, subject to actual model/device quality and licensing.

## 7. Provider independence

No single AI, STT, translation or TTS provider should be hard-coded into the business logic.

Use interfaces such as:

- `SpeechRecognizer`
- `LanguageDetector`
- `Translator`
- `AgentEngine`
- `SpeechSynthesizer`
- `CallController`

This allows local/open-source implementations first and cloud providers later if required.

## 8. Privacy

Default principle:

- process locally where practical
- minimize retained audio
- make recording/transcription behavior explicit
- protect conversation summaries
- do not expose credentials in the Android client
- isolate optional cloud services behind backend/provider interfaces

## 9. First engineering milestone

Before building the full AI conversation stack:

**Prove the Android call-control and audio feasibility on a real device.**

Success criteria:

1. App installs.
2. Required call role/permissions can be granted.
3. Incoming cellular call is observable through supported APIs.
4. Ring state can be tracked.
5. The supported answer/control path works.
6. Call audio feasibility is documented.
7. A basic test screen can show the session state.

Only after this gate passes should we build the full voice-agent loop.

## 10. Architecture decision rule

Do not optimize for the most impressive demo first. Optimize for a working, legally/supportably deployable Android foundation that can later host the agent and interpreter.
