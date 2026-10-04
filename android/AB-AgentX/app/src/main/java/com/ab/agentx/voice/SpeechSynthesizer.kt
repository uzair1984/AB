package com.ab.agentx.voice

interface SpeechSynthesizer {
    suspend fun synthesize(text: String, languageCode: String, voiceId: String? = null): ByteArray
}
