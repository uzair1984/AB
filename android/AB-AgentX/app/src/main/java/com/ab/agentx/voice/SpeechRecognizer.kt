package com.ab.agentx.voice

interface SpeechRecognizer {
    suspend fun recognize(audio: ByteArray, languageHint: String? = null): SpeechResult
}

data class SpeechResult(
    val text: String,
    val languageCode: String?,
    val confidence: Float?
)
