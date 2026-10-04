package com.ab.agentx.voice

import com.ab.agentx.call.CallSessionState
import java.util.UUID

/**
 * Provider-independent conversation coordinator.
 * Audio transport and concrete AI providers are intentionally injected later.
 */
class ConversationOrchestrator(
    private val speechRecognizer: SpeechRecognizer,
    private val languageDetector: LanguageDetector,
    private val agentEngine: AgentEngine,
    private val speechSynthesizer: SpeechSynthesizer
) {
    private var session: ConversationSession? = null

    fun startSession(): ConversationSession {
        val created = ConversationSession(
            sessionId = UUID.randomUUID().toString(),
            state = CallSessionState.GREETING
        )
        session = created
        return created
    }

    suspend fun processCallerAudio(audio: ByteArray): ByteArray? {
        val current = session ?: return null
        session = current.withState(CallSessionState.LISTENING)

        val recognized = speechRecognizer.recognize(audio, current.callerLanguage)
        if (recognized.text.isBlank()) return null

        val detected = languageDetector.detect(recognized.text)
        val updated = (session ?: current).copy(
            callerLanguage = detected.languageCode
        ).addTurn(
            ConversationTurn(
                speaker = Speaker.CALLER,
                text = recognized.text,
                languageCode = detected.languageCode
            )
        )
        session = updated.withState(CallSessionState.THINKING)

        val response = agentEngine.respond(
            AgentInput(
                text = recognized.text,
                callerLanguage = detected.languageCode,
                sessionId = updated.sessionId
            )
        )

        session = (session ?: updated).addTurn(
            ConversationTurn(
                speaker = Speaker.AB,
                text = response.text,
                languageCode = response.languageCode ?: detected.languageCode
            )
        ).withState(CallSessionState.RESPONDING)

        return speechSynthesizer.synthesize(
            response.text,
            response.languageCode ?: detected.languageCode
        )
    }

    fun endSession(): ConversationSession? =
        session?.withState(CallSessionState.COMPLETED)
}