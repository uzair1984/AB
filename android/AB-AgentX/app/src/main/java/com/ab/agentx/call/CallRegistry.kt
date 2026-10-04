package com.ab.agentx.call

import android.telecom.Call

/**
 * In-process bridge between the Telecom service and the in-call UI.
 * The authoritative call lifecycle remains inside InCallService.
 */
object CallRegistry {
    @Volatile
    private var currentCall: Call? = null

    fun set(call: Call) { currentCall = call }
    fun clear(call: Call) {
        if (currentCall === call) currentCall = null
    }

    fun answer() { currentCall?.takeIf { it.state == Call.STATE_RINGING }?.answer(0) }
    fun end() { currentCall?.disconnect() }
    fun state(): Int? = currentCall?.state
}
