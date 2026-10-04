package com.ab.agentx.voice

interface LanguageDetector {
    suspend fun detect(text: String): LanguageResult
}

data class LanguageResult(
    val languageCode: String,
    val confidence: Float?
)
