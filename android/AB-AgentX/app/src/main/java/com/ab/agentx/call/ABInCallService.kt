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
    private lateinit var notificationController: CallNotificationController
    private val sessionController = CallSessionController()

    override fun onCreate() {
        super.onCreate()
        ABModeStore.initialize(this)
        notificationController = CallNotificationController(this)
        notificationController.ensureChannel()
        Log.i(TAG, "AB_SERVICE_READY mode=${ABModeStore.getMode()}")
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val key = callKey(call)
        calls[key] = call
        CallRegistry.set(call)
        call.registerCallback(callback)
        Log.i(TAG, "CALL_ADDED key=$key state=${call.state}")
        if (call.state == Call.STATE_RINGING) {
            sessionController.onIncomingCall(call)
            val notification = notificationController.buildIncomingCallNotification()
            getSystemService(android.app.NotificationManager::class.java)?.notify(NOTIFICATION_ID, notification)
            scheduleAnswer(call)
        }
    }

    override fun onCallRemoved(call: Call) {
        val key = callKey(call)
        answerTasks.remove(key)?.let(handler::removeCallbacks)
        call.unregisterCallback(callback)
        calls.remove(key)
        CallRegistry.clear(call)
        Log.i(TAG, "CALL_REMOVED key=$key")
        sessionController.onDisconnected()
        getSystemService(android.app.NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.i(TAG, "CALL_STATE key=${callKey(call)} state=$state")
            if (state == Call.STATE_RINGING) scheduleAnswer(call)
            if (state != Call.STATE_RINGING) {
                answerTasks.remove(callKey(call))?.let(handler::removeCallbacks)
            }
        }
    }

    private fun scheduleAnswer(call: Call) {
        val key = callKey(call)
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
                sessionController.onAnswered()
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
        private const val NOTIFICATION_ID = 1001
    }
}

private fun callKey(call: Call): String = call.hashCode().toString()
