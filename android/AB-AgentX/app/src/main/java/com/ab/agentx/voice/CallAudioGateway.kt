package com.ab.agentx.voice

/**
 * Abstraction for call audio integration.
 *
 * Important: Android Telecom exposes call endpoints/routing through InCallService,
 * but does not expose a generic raw two-way cellular-call PCM stream to an app.
 * A concrete implementation must use only platform-supported audio paths.
 */
interface CallAudioGateway {
    fun start()
    fun stop()
    fun isAvailable(): Boolean
}
