package com.ab.agentx.call

import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.tts.TextToSpeech
import android.telecom.Call
import android.telecom.CallAudioState
import android.telecom.CallEndpoint
import android.telecom.InCallService
import android.util.Log
import java.util.Locale

/**
 * AB AgentX cellular call controller.
 *
 * The Android Telecom InCallService owns the live call. After answer, AB
 * explicitly routes the call to the speaker and speaks its first greeting.
 * This is the platform-supported path; raw cellular PCM is not exposed to
 * ordinary third-party apps.
 */
class ABInCallService : InCallService() {
    private val calls = linkedMapOf<String, Call>()
    private val handler = Handler(Looper.getMainLooper())
    private val answerTasks = mutableMapOf<String, Runnable>()
    private lateinit var notificationController: CallNotificationController
    private val sessionController = CallSessionController()
    private var tts: TextToSpeech? = null
    private var availableEndpoints: List<CallEndpoint> = emptyList()

    override fun onCreate() {
        super.onCreate()
        ABModeStore.initialize(this)
        notificationController = CallNotificationController(this)
        notificationController.ensureChannel()
        tts = TextToSpeech(this) { status ->
            Log.i(TAG, "TTS_INIT status=$status")
            if (status == TextToSpeech.SUCCESS) {
                tts?.language = Locale.ENGLISH
            }
        }
        Log.i(TAG, "AB_SERVICE_READY mode="+ABModeStore.getMode())
    }

    override fun onAvailableCallEndpointsChanged(availableEndpoints: MutableList<CallEndpoint>) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            this.availableEndpoints = availableEndpoints.toList()
            availableEndpoints.forEach { endpoint ->
                Log.i(TAG, "AVAILABLE_ENDPOINT type="+endpoint.endpointType+" name="+endpoint.endpointName)
            }
        }
    }

    override fun onCallEndpointChanged(callEndpoint: CallEndpoint) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.i(TAG, "CALL_ENDPOINT_CHANGED type="+callEndpoint.endpointType+" name="+callEndpoint.endpointName)
        }
    }

    override fun onMuteStateChanged(isMuted: Boolean) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            Log.i(TAG, "CALL_MUTE_CHANGED muted="+isMuted)
        }
    }

    override fun onCallAdded(call: Call) {
        super.onCallAdded(call)
        val key = callKey(call)
        calls[key] = call
        CallRegistry.set(call)
        call.registerCallback(callback)
        Log.i(TAG, "CALL_ADDED key="+key+" state="+call.state)
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
        stopTts()
        restoreAudioRoute()
        Log.i(TAG, "CALL_REMOVED key="+key)
        sessionController.onDisconnected()
        getSystemService(android.app.NotificationManager::class.java)?.cancel(NOTIFICATION_ID)
        super.onCallRemoved(call)
    }

    private val callback = object : Call.Callback() {
        override fun onStateChanged(call: Call, state: Int) {
            Log.i(TAG, "CALL_STATE key="+callKey(call)+" state="+state)
            if (state == Call.STATE_RINGING) scheduleAnswer(call)
            if (state != Call.STATE_RINGING) {
                answerTasks.remove(callKey(call))?.let(handler::removeCallbacks)
            }
            if (state == Call.STATE_ACTIVE) {
                startAssistantGreeting()
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
        Log.i(TAG, "AB_AUTO_ANSWER mode="+mode+" delayMs="+delayMs)

        val task = Runnable {
            if (call.state == Call.STATE_RINGING) {
                Log.i(TAG, "AB_ANSWERING mode="+mode)
                sessionController.onAnswered()
                routeCallToSpeaker()
                call.answer(0)
                handler.postDelayed({ startAssistantGreeting() }, GREETING_DELAY_MS)
            }
            answerTasks.remove(key)
        }
        answerTasks[key] = task
        handler.postDelayed(task, delayMs)
    }

    private fun routeCallToSpeaker() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                val speaker = availableEndpoints.firstOrNull {
                    it.endpointType == CallEndpoint.TYPE_SPEAKER
                }
                if (speaker != null) {
                    requestCallEndpointChange(speaker)
                    Log.i(TAG, "AB_AUDIO_ROUTE speaker endpoint requested")
                } else {
                    Log.w(TAG, "AB_AUDIO_ROUTE speaker endpoint unavailable")
                }
            } else {
                @Suppress("DEPRECATION")
                setAudioRoute(CallAudioState.ROUTE_SPEAKER)
                Log.i(TAG, "AB_AUDIO_ROUTE speaker requested")
            }
        } catch (e: Exception) {
            Log.e(TAG, "AB_AUDIO_ROUTE failed", e)
        }
    }

    private fun restoreAudioRoute() {
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                @Suppress("DEPRECATION")
                setAudioRoute(CallAudioState.ROUTE_EARPIECE)
            }
        } catch (_: Exception) {
        }
    }

    private fun startAssistantGreeting() {
        val engine = tts ?: return
        if (!engine.isSpeaking) {
            val greeting = "Hello, this is AB AgentX. The person you are calling is currently unavailable. Please tell me how I can help."
            Log.i(TAG, "AB_GREETING_START")
            engine.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "ab-greeting")
        }
    }

    private fun stopTts() {
        try {
            tts?.stop()
        } catch (_: Exception) {
        }
    }

    override fun onDestroy() {
        stopTts()
        tts?.shutdown()
        tts = null
        super.onDestroy()
    }

    companion object {
        private const val TAG = "AB_AGENTX_CALL"
        private const val NORMAL_DELAY_MS = 20_000L
        private const val GREETING_DELAY_MS = 700L
        private const val NOTIFICATION_ID = 1001
    }
}

private fun callKey(call: Call): String = call.hashCode().toString()
