package com.ab.agentx.call

import android.content.Context

enum class AssistantVoice { MALE, FEMALE }

object VoiceSettingsStore {
    private const val PREFS = "ab_agentx_voice"
    private const val KEY = "assistant_voice"

    fun getVoice(context: Context): AssistantVoice {
        val value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY, AssistantVoice.FEMALE.name)
        return runCatching { AssistantVoice.valueOf(value ?: AssistantVoice.FEMALE.name) }
            .getOrDefault(AssistantVoice.FEMALE)
    }

    fun setVoice(context: Context, voice: AssistantVoice) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(KEY, voice.name).apply()
    }
}
