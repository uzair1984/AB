package com.ab.agentx.call

import android.telecom.Call
import android.telecom.InCallService

/**
 * Phase 1 diagnostic Telecom service.
 * AI, STT, translation and TTS are intentionally not connected yet.
 */
class ABInCallService : InCallService() {
    private val calls = linkedMapOf<String, Call>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        calls[call.key()] = call
        call.registerCallback(callback)
    }

    override fun onCallRemoved(call: Call) {
        call.unregisterCallback(callback)
        calls.remove(call.key())
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            // Phase 1: observe lifecycle only. No automatic answering yet.
        }
    }

    private fun Call.key(): String =
        details?.telecomCallId ?: hashCode().toString()
}
