# AB AgentX

**AB AgentX** is an AI Agentic Call Assistant and Real-Time Multilingual Interpreter.

## Project mission

Build an Android-first call assistant with an open-source/zero-recurring-cost-first architecture, while keeping the system extensible for future web, desktop and advanced real-time interpretation capabilities.

## Current phase

**Phase 0 — Architecture & feasibility foundation**

We are intentionally validating the Android call-handling architecture before committing to specific paid AI/telephony providers.

## Core product modes

- **Normal Mode:** AB handles an incoming call after the configured ring delay.
- **Active Mode:** AB answers immediately when the user enables it.
- **Assistant Mode:** AB identifies the caller's intent, handles the conversation, and produces a structured summary.
- **Interpreter Mode:** AB enables two-way communication between speakers using different languages.

## Repository structure

- `docs/` — architecture, master directory and engineering decisions
- `android/` — Android application
- `backend/` — optional backend services and APIs
- `web/` — web dashboard/testing surface

## Engineering principle

Architecture first. Provider selection second. Implementation third. We will avoid unnecessary recurring-cost dependencies and keep replaceable interfaces around speech, language detection, translation, AI reasoning and text-to-speech.
