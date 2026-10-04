package com.ab.agentx.voice

interface Translator {
    suspend fun translate(text: String, sourceLanguage: String, targetLanguage: String): String
}
