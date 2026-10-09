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
import android.content.Intent

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
    private val greetedCalls = mutableSetOf<String>()
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
                configureAssistantVoice()
                // A call can become active before TTS initialization finishes.
                calls.values.filter { it.state == Call.STATE_ACTIVE }.forEach { startAssistantGreeting(it) }
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
        greetedCalls.remove(key)
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
                openInCallUi()
                startAssistantGreeting(call)
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
                openInCallUi()
                handler.postDelayed({ startAssistantGreeting(call) }, GREETING_DELAY_MS)
            }
            answerTasks.remove(key)
        }
        answerTasks[key] = task
        handler.postDelayed(task, delayMs)
    }

    private fun configureAssistantVoice() {
        val engine = tts ?: return
        val preferredGender = VoiceSettingsStore.getVoice(this)
        val voices = engine.voices ?: emptySet()
        val english = voices.filter { it.locale.language == Locale.ENGLISH.language }
        val genderTokens = when (preferredGender) {
            AssistantVoice.MALE -> listOf("male", "man", "m1", "m2", "david", "mark", "daniel")
            AssistantVoice.FEMALE -> listOf("female", "woman", "f1", "f2", "samantha", "susan", "karen")
        }
        val selected = (english + voices).distinctBy { it.name }.firstOrNull { voice ->
            val name = voice.name.lowercase(Locale.US)
            val femaleLabel = name.contains("female") || name.contains("woman")
            when (preferredGender) {
                AssistantVoice.MALE ->
                    (genderTokens.any { token -> name.contains(token) } && !femaleLabel)
                AssistantVoice.FEMALE ->
                    femaleLabel || genderTokens.any { token ->
                        name.contains(token) && (token == "samantha" || token == "susan" || token == "karen")
                    }
            }
        } ?: english.firstOrNull() ?: voices.firstOrNull()
        if (selected != null) {
            engine.voice = selected
            Log.i(TAG, "AB_TTS_VOICE_SELECTED name=${selected.name} requested=$preferredGender locale=${selected.locale}")
        } else {
            engine.language = Locale.ENGLISH
            Log.w(TAG, "AB_TTS_VOICE_FALLBACK requested=$preferredGender")
        }
    }

    private fun routeCallToSpeaker() {
        try {
            @Suppress("DEPRECATION")
            setAudioRoute(CallAudioState.ROUTE_SPEAKER)
            Log.i(TAG, "AB_AUDIO_ROUTE speaker requested")
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

    private fun startAssistantGreeting(call: Call) {
        val key = callKey(call)
        if (call.state != Call.STATE_ACTIVE || greetedCalls.contains(key)) return
        val engine = tts
        if (engine == null) {
            Log.w(TAG, "AB_GREETING_DEFERRED reason=tts_not_initialized call=$key")
            return
        }
        greetedCalls.add(key)
        Log.i(TAG, "AB_VOICE_PIPELINE cellular_tts_local_only=true raw_cellular_pcm=false")
        val greeting = "Hello, this is AB AgentX. The person you are calling is currently unavailable. Please tell me how I can help."
        Log.i(TAG, "AB_GREETING_START call=$key")
        val result = engine.speak(greeting, TextToSpeech.QUEUE_FLUSH, null, "ab-greeting-$key")
        if (result == TextToSpeech.ERROR) {
            greetedCalls.remove(key)
            Log.e(TAG, "AB_GREETING_FAILED call=$key")
        }
    }

    private fun openInCallUi() {
        try {
            startActivity(Intent(this, InCallActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Log.i(TAG, "AB_INCALL_UI_OPENED")
        } catch (e: Exception) {
            Log.e(TAG, "AB_INCALL_UI_OPEN_FAILED", e)
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
