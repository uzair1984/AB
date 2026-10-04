package com.ab.agentx.call

import android.os.Build
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * Phase 1 Telecom diagnostic service.
 * No AI/STT/TTS is connected yet.
 */
class ABInCallService : InCallService() {
    private val calls = linkedMapOf<String, Call>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val key = call.key()
        calls[key] = call
        call.registerCallback(callback)
        Log.i(TAG, "CALL_ADDED key=${key} state=${call.state}")
        updateState(call)
    }

    override fun onCallRemoved(call: Call) {
        val key = call.key()
        call.unregisterCallback(callback)
        calls.remove(key)
        Log.i(TAG, "CALL_REMOVED key=${key}")
        super.onCallRemoved(call)
    }

    override fun onAvailableCallEndpointsChanged(endpoints: MutableList<android.telecom.CallEndpoint>) {
        super.onAvailableCallEndpointsChanged(endpoints)
        if (Build.VERSION.SDK_INT >= 34) {
            Log.i(TAG, "AUDIO_ENDPOINTS_CHANGED count=${endpoints.size}")
        }
    }

    override fun onCallEndpointChanged(endpoint: android.telecom.CallEndpoint) {
        super.onCallEndpointChanged(endpoint)
        if (Build.VERSION.SDK_INT >= 34) {
            Log.i(TAG, "AUDIO_ENDPOINT_CHANGED type=${endpoint.endpointType}")
        }
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.i(TAG, "CALL_STATE key=${call.key()} state=$state")
            updateState(call)
        }
    }

    private fun updateState(call: Call) {
        when (call.state) {
            Call.STATE_RINGING -> Log.i(TAG, "AB_STATE=RINGING")
            Call.STATE_DIALING -> Log.i(TAG, "AB_STATE=DIALING")
            Call.STATE_ACTIVE -> Log.i(TAG, "AB_STATE=ACTIVE")
            Call.STATE_DISCONNECTED -> Log.i(TAG, "AB_STATE=DISCONNECTED")
        }
    }

    private fun Call.key(): String =
        details?.telecomCallId ?: hashCode().toString()

    companion object {
        private const val TAG = "AB_AGENTX_CALL"
    }
}