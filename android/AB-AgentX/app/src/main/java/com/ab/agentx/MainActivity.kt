package com.ab.agentx

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ab.agentx.call.ABMode
import com.ab.agentx.call.ABModeStore

class MainActivity : ComponentActivity() {
    companion object {
        private const val REQUEST_DEFAULT_DIALER = 1001
        private const val REQUEST_CALL_PHONE = 1002
    }

    private var number = mutableStateOf("")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ABModeStore.initialize(this)

        intent?.data?.let { data ->
            if (data.scheme == "tel") number.value = data.schemeSpecificPart ?: ""
        }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val mode = ABModeStore.getMode()
                    val digits = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
                    Column(
                        Modifier.fillMaxSize().padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AB AgentX", style = MaterialTheme.typography.headlineMedium)
                        Text("AI Agentic Call Assistant")
                        Text("Current mode: $mode", Modifier.padding(top = 12.dp))
                        Text(
                            if (isDefaultDialer()) "Phone role: AB AgentX" else "Phone role: System Phone",
                            Modifier.padding(top = 8.dp)
                        )

                        Text(
                            if (number.value.isBlank()) "Enter number" else number.value,
                            style = MaterialTheme.typography.headlineSmall,
                            modifier = Modifier.padding(top = 24.dp)
                        )

                        digits.chunked(3).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                row.forEach { digit ->
                                    OutlinedButton(onClick = { number.value += digit }) {
                                        Text(digit)
                                    }
                                }
                            }
                        }

                        Row(
                            Modifier.fillMaxWidth().padding(top = 8.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            OutlinedButton(
                                onClick = {
                                    if (number.value.isNotEmpty()) {
                                        number.value = number.value.dropLast(1)
                                    }
                                }
                            ) { Text("Delete") }

                            Button(onClick = ::placeCall) { Text("Call") }
                        }

                        Spacer(Modifier.height(16.dp))

                        Button(onClick = { setMode(ABMode.NORMAL) }) {
                            Text("Normal Mode — 20 sec")
                        }
                        Button(onClick = { setMode(ABMode.ACTIVE) }, Modifier.padding(top = 8.dp)) {
                            Text("Active Mode — Immediate")
                        }
                        Button(onClick = { setMode(ABMode.OFF) }, Modifier.padding(top = 8.dp)) {
                            Text("Turn AB Off")
                        }

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Button(onClick = ::requestDefaultDialer, Modifier.padding(top = 16.dp)) {
                                Text("Enable AB as Phone App")
                            }
                        }
                    }
                }
            }
        }
    }

    private fun isDefaultDialer(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            getSystemService(RoleManager::class.java)
                .isRoleHeld(RoleManager.ROLE_DIALER)

    private fun setMode(mode: ABMode) {
        ABModeStore.setMode(mode)
        recreate()
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = getSystemService(RoleManager::class.java)
        if (!roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            startActivityForResult(
                roleManager.createRequestRoleIntent(RoleManager.ROLE_DIALER),
                REQUEST_DEFAULT_DIALER
            )
        }
    }

    private fun placeCall() {
        val target = number.value.trim()
        if (target.isBlank()) return

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.CALL_PHONE),
                REQUEST_CALL_PHONE
            )
            return
        }

        val telecomManager = getSystemService(TelecomManager::class.java)
        telecomManager.placeCall(Uri.fromParts("tel", target, null), null)
    }

    @Deprecated("Use Activity Result APIs in a later UI pass.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEFAULT_DIALER && resultCode == Activity.RESULT_OK) recreate()
        if (requestCode == REQUEST_CALL_PHONE && resultCode == Activity.RESULT_OK) recreate()
    }
}
