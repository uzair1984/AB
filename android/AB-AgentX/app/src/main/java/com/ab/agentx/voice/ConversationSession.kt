package com.ab.agentx.voice

import com.ab.agentx.call.CallSessionState

data class ConversationSession(
    val sessionId: String,
    val state: CallSessionState = CallSessionState.IDLE,
    val callerLanguage: String? = null,
    val turns: List<ConversationTurn> = emptyList()
) {
    fun withState(next: CallSessionState) = copy(state = next)

    fun addTurn(turn: ConversationTurn) =
        copy(turns = turns + turn)
}

data class ConversationTurn(
    val speaker: Speaker,
    val text: String,
    val languageCode: String? = null
)

enum class Speaker { CALLER, AB }
