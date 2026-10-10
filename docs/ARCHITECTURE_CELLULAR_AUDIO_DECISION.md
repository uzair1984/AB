# AB AgentX — Cellular Audio Feasibility Notes

**Status:** Research notes only. No hardware audio qualification is claimed.  
**Canonical product architecture:** [AB_AGENTX_PRODUCT_ARCHITECTURE.md](./AB_AGENTX_PRODUCT_ARCHITECTURE.md)

## Scope correction

This document must not be interpreted as limiting AB AgentX to the Samsung A51, a particular caller phone, SIM or network. Phone A/Phone B are test labels only:
- The AB customer installs the app on their supported Android phone.
- The caller may use any ordinary phone, SIM, carrier or country and must not need to install AB.
- Compatibility qualification is primarily about the AB-installed handset, OS/manufacturer/carrier and the selected call-media transport. Testing one handset does not establish universal Android support.

The product-wide transport decision must compare carrier/PSTN forwarding to a voice agent, direct privileged native SIM audio on qualified devices, PC/audio hardware as a lab prototype, and AB VoIP for a separate VoIP channel. The caller's global compatibility requirement strongly favors a carrier/PSTN ingress path, but this can incur telephony/service fees and depends on country, numbering, carrier features and regulations. It must not be marketed as guaranteed zero recurring cost. Direct raw audio from an ordinary SIM call is not exposed through a universal public Android API.

## Confirmed repository limitations

- Android Telecom call control and answering do not imply access to bidirectional cellular PCM.
- The current Text-to-Speech greeting path is local-only; it is not proof that the remote caller hears AB.
- Stub speech/language/agent/TTS providers are not a production live conversation pipeline.
- AB VoIP Telecom scaffolding is not a complete SIP/WebRTC media implementation and must fail closed while media is missing.

## Reference projects previously investigated

- AgentCall: https://github.com/sidinsearch/AgentCall — architecture reference for a privileged, hardware-qualified two-way telephony audio bridge; not a universal Android API or drop-in solution. Review its AGPLv3 license before any code reuse.
- SIMVox-AI: https://github.com/Magraa/SIMVox-AI — PC/audio-device-based prototype architecture; needs hardware/audio routing and provider configuration.
- Linphone SDK: https://www.linphone.org/en/liblinphone-voip-sdk/ — useful for a distinct SIP/VoIP path, but does not by itself capture ordinary SIM-call audio.
- Asterisk ARI External Media: https://docs.asterisk.org/ — useful after calls enter a supported telephony bridge; does not magically connect to a handset's SIM audio.

## Engineering rule

Do not build a caller-facing AI voice feature around local phone TTS or placeholder providers. Do not claim the product is ready because an APK compiles or a call auto-answers. Before release, prove both directions on the chosen route: AB receives the remote caller's voice, and the remote caller hears AB's generated speech. Verify ACTIVE/NORMAL/OFF behavior, interruption/echo, end-call, repeated calls, failure handling and restoration of normal routing.

All mode semantics, global caller compatibility, language/interpreter/assistant scope, cost guardrails, UI requirements and staged release gates are defined in the canonical product architecture document above.