# AB AgentX — Telephony route and cost findings

Research checked: 2026-10-10. These are published price/coverage claims, not an account provisioning test.

## Lower-cost Pakistan DID candidate: DIDHub

DIDHub's Pakistan product page currently advertises:
- Geographic Pakistan DID from **US$5/month**
- **US$30 one-time setup**
- Published inbound usage from **US$0.04/minute**
- Full KYC, with the page stating business incorporation documents, an authorized in-country contact and use-case justification; activation is estimated at 10–15 business days.

Sources:
- https://didhub.io/numbers/pakistan
- https://didhub.io/buy/pakistan

This is the lowest published Pakistan DID option found in this pass, but it is **not free** and may not be available to an individual account because of its stated KYC requirements. Confirm the exact number type, inbound voice, current pricing, documents, caller ID, and whether a local carrier can forward the user's mobile calls to it before choosing it.

## Telnyx Pakistan DID

Telnyx advertises Pakistani virtual numbers and documents assigning them to a SIP connection:
- https://telnyx.com/phone-numbers/pakistan
- https://support-v2.telnyx.com/en/articles/5469551-international-numbers-required-documents
- https://support.telnyx.com/en/articles/4380325-search-and-buy-numbers

Its product page currently states Pakistan local numbers start at **US$42.50/month**, so Telnyx is a more expensive fallback based on published pricing. Exact number type, inbound voice feature, documents and current quote still need confirmation.

## Twilio

Twilio documents bidirectional Media Streams and sending audio both into and out of a live call:
- https://www.twilio.com/docs/voice/media-streams
- https://www.twilio.com/docs/voice/media-streams/websocket-messages
- https://www.twilio.com/docs/api/errors/31920 (Media Stream URLs cannot contain query strings; use nested `<Parameter>` values instead)
- https://www.twilio.com/docs/global-infrastructure/firewall-configurations/media-streams-configuration (validate `X-Twilio-Signature`)

The documentation reviewed here does not prove a suitable Pakistani inbound number is available to this account. Do not assume a US/UK Twilio number can replace the user's normal Pakistani mobile number without international forwarding charges and carrier compatibility checks.

## Local carrier/SIP option

PTCL and other Pakistani telecom operators advertise business SIP trunk products, but pricing and technical provisioning are quote/contract based:
- PTCL: https://ptcl.com.pk/
- Wateen voice/SIP: https://wateen.com/voice/
- Vision Telecom SIP trunk: https://www.visiontelecom.com.pk/sip-trunk/

This may be worth investigating only if the user's current mobile operator can route missed calls to a supported local destination, or if a business SIP line is available. It does not automatically let an Android app take raw audio from a normal mobile SIM.

## Architecture decision

1. Keep the live media bridge provider-adapter based; do not lock Android into one carrier.
2. First qualify DIDHub as the lower-cost hosted DID option, but do not purchase until account/KYC eligibility and conditional mobile forwarding are confirmed.
3. In parallel, check whether the user's mobile carrier supports conditional no-answer forwarding to a local landline/DID, including per-minute forwarding charges, caller ID preservation and the deactivation code. This is not inherently free.
4. If DIDHub KYC is not possible, ask a local licensed operator for a SIP trunk quote; keep Telnyx as a higher-cost fallback.
5. Keep Android `InCallService` as the UI/call-state layer only; the current app implementation cannot supply reliable raw cellular PCM to a general third-party app.
6. Release gates remain: confirmed inbound route, confirmed total prices, real 60-second two-way call, then language/voice and ACTIVE/NORMAL/OFF tests.

## Current implementation state

- The backend prototype is in `services/live-media-bridge/` on draft PR #5.
- Protocol, TwiML generation, Twilio signature, inbound webhook and server smoke tests are being validated by GitHub Actions.
- No provider account, number, API secret, carrier forwarding or live call has been configured. This is not yet an end-to-end working feature or APK release.
