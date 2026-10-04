package com.ab.agentx.voice

/**
 * Deterministic development providers.
 * These are placeholders for integration tests only; no network calls or paid services.
 */
class StubSpeechRecognizer : SpeechRecognizer {
    override suspend fun recognize(audio: ByteArray, languageHint: String?) =
        SpeechResult("stub caller input", languageHint ?: "en", 1f)
}

class StubLanguageDetector : LanguageDetector {
    override suspend fun detect(text: String) = LanguageResult("en", 1f)
}

class StubAgentEngine : AgentEngine {
    override suspend fun respond(input: AgentInput) =
        AgentResponse("AB AgentX received your message. The voice engine is not connected yet.", input.callerLanguage ?: "en")
}

class StubSpeechSynthesizer : SpeechSynthesizer {
    override suspend fun synthesize(text: String, languageCode: String, voiceId: String?) =
        text.toByteArray(Charsets.UTF_8)
}
