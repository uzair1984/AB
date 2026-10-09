package com.ab.agentx.call

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * Telecom lifecycle boundary only. No SIP signaling or voice-media transport
 * is implemented yet, so this service must never report a call as connected.
 */
class ABVoipConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        return newConnection(request.address).also {
            it.setDialing()
            Log.i(TAG, "VOIP_OUTGOING_CREATED address=" + request.address)
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        return newConnection(request.address).also {
            it.setRinging()
            Log.i(TAG, "VOIP_INCOMING_CREATED address=" + request.address)
        }
    }

    private fun newConnection(address: Uri?): Connection =
        object : Connection() {
            init {
                setAddress(address ?: Uri.parse("sip:unknown"), android.telecom.TelecomManager.PRESENTATION_ALLOWED)
                setConnectionProperties(PROPERTY_SELF_MANAGED)
                setAudioModeIsVoip(true)
                setConnectionCapabilities(0)
            }

            override fun onAnswer() {
                Log.e(TAG, "VOIP_ANSWER_REJECTED reason=media_transport_not_implemented")
                setDisconnected(
                    DisconnectCause(
                        DisconnectCause.ERROR,
                        "AB AgentX VoIP media transport is not configured"
                    )
                )
                destroy()
            }

            override fun onDisconnect() {
                setDisconnected(DisconnectCause(DisconnectCause.LOCAL))
                destroy()
            }
        }

    companion object {
        private const val TAG = "AB_AGENTX_VOIP"
    }
}
