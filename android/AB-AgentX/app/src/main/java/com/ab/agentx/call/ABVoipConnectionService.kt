package com.ab.agentx.call

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * AB-owned VoIP call boundary.
 * Cellular calls remain under ABInCallService; AB-owned calls can later attach
 * a real WebRTC/SIP bidirectional media transport here.
 */
class ABVoipConnectionService : ConnectionService() {
    override fun onCreateOutgoingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        return createConnection(request.address).also {
            it.setDialing()
            Log.i(TAG, "VOIP_OUTGOING_CREATED")
        }
    }

    override fun onCreateIncomingConnection(
        connectionManagerPhoneAccount: PhoneAccountHandle?,
        request: ConnectionRequest
    ): Connection {
        return createConnection(request.address).also {
            it.setRinging()
            Log.i(TAG, "VOIP_INCOMING_CREATED")
        }
    }

    private fun createConnection(address: Uri?): Connection =
        object : Connection() {
            init {
                setAddress(address ?: Uri.parse("sip:unknown"), PRESENTATION_ALLOWED)
                setConnectionProperties(PROPERTY_SELF_MANAGED)
                setAudioModeIsVoip(true)
            }

            override fun onAnswer() {
                Log.i(TAG, "VOIP_ANSWER")
                setActive()
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
