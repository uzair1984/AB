package com.ab.agentx.call

import android.content.Context

object ABModeStore {
    private const val PREFS = "ab_agentx"
    private const val MODE = "mode"
    private var appContext: Context? = null

    fun initialize(context: Context) {
        appContext = context.applicationContext
    }

    fun getMode(): ABMode {
        val value = appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.getString(MODE, ABMode.OFF.name) ?: ABMode.OFF.name
        return runCatching { ABMode.valueOf(value) }.getOrDefault(ABMode.OFF)
    }

    fun setMode(mode: ABMode) {
        appContext?.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            ?.edit()?.putString(MODE, mode.name)?.apply()
    }
}
