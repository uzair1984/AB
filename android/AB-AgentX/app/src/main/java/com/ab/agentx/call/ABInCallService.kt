package com.ab.agentx.call

import android.telecom.Call
import android.telecom.InCallService

/**
 * Phase 1 diagnostic Telecom service.
 * AI, STT, translation and TTS are intentionally not connected yet.
 */
class ABInCallService : InCallService() {
    private var currentCall: Call? = null

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        currentCall = call
        // TODO: publish call lifecycle to the session controller.
    }

    override fun onCallRemoved(call: Call) {
        if (currentCall == call) currentCall = null
        super.onCallRemoved(call)
        // TODO: publish COMPLETED state to the session controller.
    }
}
