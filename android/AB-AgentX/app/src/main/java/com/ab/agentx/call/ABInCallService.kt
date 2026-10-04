package com.ab.agentx.call

import android.os.Handler
import android.os.Looper
import android.telecom.Call
import android.telecom.InCallService
import android.util.Log

/**
 * Phase 1 call-control prototype.
 * Normal mode answers after the configured delay; Active mode answers immediately.
 * AI/STT/TTS are intentionally not connected yet.
 */
class ABInCallService : InCallService() {
    private val calls = linkedMapOf<String, Call>()
    private val handler = Handler(Looper.getMainLooper())
    private val answerTasks = mutableMapOf<String, Runnable>()

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val key = call.key()
        calls[key] = call
        call.registerCallback(callback)
        Log.i(TAG, "CALL_ADDED key=$key state=${call.state}")
        if (call.state == Call.STATE_RINGING) scheduleAnswer(call)
    }

    override fun onCallRemoved(call: Call) {
        val key = call.key()
        answerTasks.remove(key)?.let(handler::removeCallbacks)
        call.unregisterCallback(callback)
        calls.remove(key)
        Log.i(TAG, "CALL_REMOVED key=$key")
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.i(TAG, "CALL_STATE key=${call.key()} state=$state")
            if (state == Call.STATE_RINGING) scheduleAnswer(call)
            if (state != Call.STATE_RINGING) {
                answerTasks.remove(call.key())?.let(handler::removeCallbacks)
            }
        }
    }

    private fun scheduleAnswer(call: Call) {
        val key = call.key()
        if (answerTasks.containsKey(key)) return

        val mode = ABModeStore.getMode()
        if (mode == ABMode.OFF) {
            Log.i(TAG, "AB_AUTO_ANSWER disabled")
            return
        }

        val delayMs = if (mode == ABMode.ACTIVE) 0L else NORMAL_DELAY_MS
        Log.i(TAG, "AB_AUTO_ANSWER mode=$mode delayMs=$delayMs")

        val task = Runnable {
            if (call.state == Call.STATE_RINGING) {
                Log.i(TAG, "AB_ANSWERING mode=$mode")
                call.answer(0)
            }
            answerTasks.remove(key)
        }
        answerTasks[key] = task
        handler.postDelayed(task, delayMs)
    }

    companion object {
        private const val TAG = "AB_AGENTX_CALL"
        private const val NORMAL_DELAY_MS = 20_000L
    }
}
