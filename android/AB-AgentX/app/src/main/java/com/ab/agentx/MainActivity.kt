package com.ab.agentx

import android.Manifest
import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
    private var phoneRoleEnabled = mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ABModeStore.initialize(this)
        phoneRoleEnabled.value = isDefaultDialer()

        intent?.data?.let { data ->
            if (data.scheme == "tel") number.value = data.schemeSpecificPart ?: ""
        }

        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val mode = ABModeStore.getMode()
                    val isNormal = mode == ABMode.NORMAL
                    val isActive = mode == ABMode.ACTIVE
                    val isOff = mode == ABMode.OFF
                    val isPhoneApp = phoneRoleEnabled.value
                    val digits = listOf("1","2","3","4","5","6","7","8","9","*","0","#")

                    Column(
                        Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("AB AgentX", style = MaterialTheme.typography.headlineMedium)
                        Text("AI Agentic Call Assistant")
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
                                    if (number.value.isNotEmpty()) number.value = number.value.dropLast(1)
                                }
                            ) { Text("Delete") }
                            Button(onClick = ::placeCall) { Text("Call") }
                        }

                        Spacer(Modifier.height(20.dp))

                        Text(
                            "AB Mode",
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier.fillMaxWidth().padding(bottom = 6.dp)
                        )

                        ModeSwitchRow(
                            label = "Normal Mode — 20 sec",
                            checked = isNormal,
                            onCheckedChange = { if (it) setMode(ABMode.NORMAL) }
                        )
                        ModeSwitchRow(
                            label = "Active Mode — Immediate",
                            checked = isActive,
                            onCheckedChange = { if (it) setMode(ABMode.ACTIVE) }
                        )
                        ModeSwitchRow(
                            label = "Turn AB Off",
                            checked = isOff,
                            onCheckedChange = { if (it) setMode(ABMode.OFF) }
                        )

                        Spacer(Modifier.height(16.dp))

                        Button(
                            onClick = ::requestDefaultDialer,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isPhoneApp) "Disable AB as Phone App"
                                else "Enable AB as Phone App"
                            )
                        }

                        Text(
                            if (isPhoneApp)
                                "✓ AB AgentX is set as the default phone app."
                            else
                                "AB AgentX is not the default phone app.",
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        phoneRoleEnabled.value = isDefaultDialer()
    }

    @androidx.compose.runtime.Composable
    private fun ModeSwitchRow(
        label: String,
        checked: Boolean,
        onCheckedChange: (Boolean) -> Unit
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, modifier = Modifier.weight(1f))
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }

    private fun isDefaultDialer(): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q &&
            getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)

    private fun setMode(mode: ABMode) {
        ABModeStore.setMode(mode)
        recreate()
    }

    private fun requestDefaultDialer() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return
        val roleManager = getSystemService(RoleManager::class.java)
        if (roleManager.isRoleHeld(RoleManager.ROLE_DIALER)) {
            // Android does not expose a normal third-party API to silently relinquish
            // ROLE_DIALER. Hand control back to the system Default Phone App settings.
            startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        } else {
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

        getSystemService(TelecomManager::class.java)
            .placeCall(Uri.fromParts("tel", target, null), null)
    }

    @Deprecated("Use Activity Result APIs in a later UI pass.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEFAULT_DIALER) {
            phoneRoleEnabled.value = isDefaultDialer()
        }
        if (requestCode == REQUEST_CALL_PHONE && resultCode == Activity.RESULT_OK) recreate()
    }
}