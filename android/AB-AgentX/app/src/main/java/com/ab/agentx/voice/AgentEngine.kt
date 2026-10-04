package com.ab.agentx.voice

interface AgentEngine {
    suspend fun respond(input: AgentInput): AgentResponse
}

data class AgentInput(
    val text: String,
    val callerLanguage: String?,
    val sessionId: String
)

data class AgentResponse(
    val text: String,
    val languageCode: String?
)
