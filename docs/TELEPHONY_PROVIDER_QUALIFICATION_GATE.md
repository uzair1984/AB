# AB AgentX — Telephony Provider Qualification Gate

**Status:** research shortlist only; no provider account, number, forwarding route, or live media has been provisioned.

## Decision

Test **Telnyx first** as the candidate for the PSTN media bridge, with **Twilio as fallback**. This is a test order, not a claim that either is already configured or guaranteed to work with a Pakistani mobile number.

Why:
- Telnyx publicly advertises Pakistan virtual numbers and documents inbound DID-to-SIP connections.
- Telnyx's international-number requirements list Pakistan-specific documentation requirements; number availability, exact number type, voice capabilities and approval must be confirmed in the account before implementation is treated as viable.
- Twilio documents bidirectional Media Streams, but its general Pakistan voice pricing does not prove that a suitable Pakistani inbound number can be purchased or that the user's carrier supports the required forwarding.

References:
- Telnyx Pakistan numbers: https://telnyx.com/phone-numbers/pakistan
- Telnyx inbound DID/SIP assignment: https://support.telnyx.com/en/articles/1177115-how-to-setup-a-did-to-sip-connection
- Telnyx international number documentation: https://support-v2.telnyx.com/en/articles/5469551-international-numbers-required-documents
- Telnyx number search: https://support.telnyx.com/en/articles/4380325-search-and-buy-numbers
- Twilio bidirectional Media Streams: https://www.twilio.com/docs/voice/media-streams
- Twilio number availability caveat: https://help.twilio.com/articles/223182988
- Android default dialer and InCallService limitations: https://developer.android.com/develop/connectivity/telecom/dialer-app and https://developer.android.com/reference/android/telecom/InCallService

## Required go/no-go checks before coding provider-specific integration

1. Can the provider provision a Pakistan number suitable for inbound voice to this account, and what documents are required?
2. Can a call from an ordinary Pakistani mobile caller reach that number reliably?
3. Can the user's existing mobile carrier forward calls to it? Confirm conditional no-answer forwarding, forwarding charges, caller ID preservation, and cancel/deactivation codes with the carrier.
4. Confirm current inbound/outbound per-minute prices, number rental, minimum spend, taxes, and payment method requirements.
5. Prove a two-way media session: remote caller speech reaches the media server; generated audio returns to the caller; both directions remain intelligible during a 60-second test.
6. Only then wire streaming STT/language detection/translation-or-agent/TTS into the live media stream and add call-state mapping for ACTIVE/NORMAL/OFF.

## Architecture contract

- Android app controls settings and status; it must never display caller-audio ONLINE until a real media session is verified.
- Carrier/PSTN calls enter a provider with duplex media. A server-side session connects incoming audio to streaming speech recognition, automatic language detection, agent/translation logic, and synthesized audio returned over the same call.
- ACTIVE/NORMAL/OFF remain exactly as documented in `AB_AGENTX_PRODUCT_ARCHITECTURE.md`. If carrier forwarding cannot be switched automatically and reliably, UI must state the limitation rather than imply it works.
- Keep a transport interface so another SIP/PSTN provider can replace the first without rewriting the Android app.
- Do not store credentials in the repository. Use server environment secrets.
- Open-source STT/TTS/agent components can reduce model costs, but PSTN numbers, trunks, forwarding, bandwidth and hosting may still cost money. Do not promise universal zero recurring cost.

## Acceptance gates

**Gate A — provider:** a test number and inbound route are actually provisioned and reachable.

**Gate B — media:** caller and agent can hear each other through the provider's live bidirectional stream, with logs proving both directions.

**Gate C — conversation:** auto-detect a test language, interpret both directions, handle interruptions/timeouts, and play real synthesized audio to the remote caller.

**Gate D — modes:** ACTIVE immediate handling; NORMAL lets the handset ring before configured takeover; OFF leaves normal phone behavior. Verify behavior on at least two caller networks/devices where possible.

**Gate E — Android:** default phone role state, labels, scroll, voice choice, outbound cancel/end control, notifications and failure states are validated on-device.

A successful APK build or a local TextToSpeech greeting does not pass Gates B or C. Until the gates pass, this remains unvalidated.
