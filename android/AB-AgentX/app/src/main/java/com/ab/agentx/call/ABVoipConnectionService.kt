package com.ab.agentx.call

import android.net.Uri
import android.telecom.Connection
import android.telecom.ConnectionRequest
import android.telecom.ConnectionService
import android.telecom.DisconnectCause
import android.telecom.PhoneAccountHandle
import android.util.Log

/**
 * AB-owned VoIP connection surface.
 * Separate from ABInCallService: cellular calls remain platform-managed,
 * while AB-owned VoIP calls can use AB's own media transport.
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
                setAddress(address ?: Uri.parse("sip:unknown"), Connection.PRESENTATION_ALLOWED)
                setConnectionProperties(PROPERTY_SELF_MANAGED)
                setAudioModeIsVoip(true)
                setConnectionCapabilities(0)
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
