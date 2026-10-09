package com.ab.agentx.call

import android.content.ComponentName
import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.telecom.PhoneAccount
import android.telecom.PhoneAccountHandle
import android.telecom.TelecomManager
import android.util.Log

/**
 * Registers AB's self-managed Telecom account. This is the OS call-control
 * registration only; it does not provide network signaling or voice media.
 */
object ABVoipTelecom {
    private const val TAG = "AB_AGENTX_VOIP"
    private const val ACCOUNT_ID = "ab_agentx_self_managed"

    fun phoneAccountHandle(context: Context): PhoneAccountHandle =
        PhoneAccountHandle(
            ComponentName(context, ABVoipConnectionService::class.java),
            ACCOUNT_ID
        )

    fun register(context: Context): Boolean {
        val telecom = context.getSystemService(TelecomManager::class.java) ?: return false
        return runCatching {
            val handle = phoneAccountHandle(context)
            val account = PhoneAccount.builder(handle, "AB AgentX VoIP")
                .setCapabilities(PhoneAccount.CAPABILITY_SELF_MANAGED)
                .build()
            telecom.registerPhoneAccount(account)
            val registered = telecom.getPhoneAccount(handle) != null
            Log.i(TAG, "VOIP_ACCOUNT_REGISTERED=$registered")
            registered
        }.onFailure {
            Log.e(TAG, "VOIP_ACCOUNT_REGISTRATION_FAILED", it)
        }.getOrDefault(false)
    }

    /**
     * Starts an OS-managed AB VoIP call to a SIP URI after registration.
     * A reachable SIP endpoint and media/signaling implementation are still
     * required; this deliberately does not claim that a call is connected.
     */
    fun placeSipCall(context: Context, sipUri: Uri) {
        require(sipUri.scheme.equals("sip", ignoreCase = true) ||
            sipUri.scheme.equals("sips", ignoreCase = true)) {
            "AB VoIP destinations must be sip: or sips: URIs"
        }
        check(register(context)) { "AB AgentX VoIP account could not be registered" }
        val extras = Bundle().apply {
            putParcelable(TelecomManager.EXTRA_PHONE_ACCOUNT_HANDLE, phoneAccountHandle(context))
        }
        val telecom = context.getSystemService(TelecomManager::class.java)
            ?: error("Android Telecom is unavailable")
        telecom.placeCall(sipUri, extras)
    }
}
