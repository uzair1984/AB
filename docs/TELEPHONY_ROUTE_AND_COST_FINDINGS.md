# AB AgentX — Telephony route and cost findings

Research checked: 2026-10-10. This is provider qualification, not a live account test.

## Findings

### Telnyx Pakistan DID
Telnyx advertises Pakistani virtual numbers and documents assigning them to a SIP connection:
- Product page: https://telnyx.com/phone-numbers/pakistan
- Number requirements: https://support-v2.telnyx.com/en/articles/5469551-international-numbers-required-documents
- Number purchase/activation workflow: https://support.telnyx.com/en/articles/4380325-search-and-buy-numbers

The product page currently states Pakistan local numbers start at **US$42.50/month**. This alone means a Telnyx Pakistan DID does **not** satisfy a strict zero-recurring-cost goal. The exact available number type, inbound voice feature, documents, activation and current quote still need confirmation inside an account.

### Twilio
Twilio documents bidirectional Media Streams and supports sending caller audio to a WebSocket and audio back into a call:
- Media Streams overview: https://www.twilio.com/docs/voice/media-streams
- WebSocket messages and outbound audio format: https://www.twilio.com/docs/voice/media-streams/websocket-messages
- General number availability caveat: https://help.twilio.com/articles/223182988

The documentation reviewed here does not prove that a suitable Pakistani inbound number is available to this account. Do not assume that a Twilio US/UK number can replace the user's normal Pakistani mobile number without international forwarding costs and carrier compatibility checks.

## Architecture decision

1. Keep the live media bridge provider-adapter based; do not lock Android into one carrier.
2. Do not provision or advertise a paid Pakistan DID as the default path until Boss explicitly accepts the monthly DID charge.
3. First investigate the user's existing mobile carrier's conditional no-answer forwarding to a reachable provider number, including forwarding charges, caller ID preservation and deactivation code. This still requires a provider number and is not inherently free.
4. Keep local Android InCallService as the UI/call-state layer only; the current implementation cannot supply reliable raw cellular PCM to a general third-party app.
5. Release gates remain: confirmed inbound route, confirmed prices, two-way 60-second real call, then language/voice and ACTIVE/NORMAL/OFF tests.

## Current implementation state

- The backend prototype is in `services/live-media-bridge/` on PR #5.
- GitHub Actions protocol tests passed for commit `820ed62762224083f436b5bea90c2e530925df32`; a follow-up commit changed the default model to the documented `gpt-realtime` model and has its own CI run.
- No provider account, number, API secret, carrier forwarding or live call has been configured. This is not yet an end-to-end working feature or APK release.
