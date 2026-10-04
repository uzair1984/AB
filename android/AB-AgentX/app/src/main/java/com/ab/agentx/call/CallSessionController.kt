package com.ab.agentx.call

import android.telecom.Call
import android.util.Log

class CallSessionController {
    fun onIncomingCall(call: Call): CallSessionState =
        if (call.state == Call.STATE_RINGING) {
            Log.i(TAG, "SESSION_TRANSITION IDLE -> RINGING")
            CallSessionState.RINGING
        } else {
            CallSessionState.IDLE
        }

    fun onAnswered(): CallSessionState {
        Log.i(TAG, "SESSION_TRANSITION RINGING -> GREETING")
        return CallSessionState.GREETING
    }

    fun onDisconnected(): CallSessionState {
        Log.i(TAG, "SESSION_TRANSITION -> ENDING -> SUMMARY")
        return CallSessionState.SUMMARY
    }

    companion object {
        private const val TAG = "AB_AGENTX_SESSION"
    }
}
