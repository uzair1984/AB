package com.ab.agentx

import android.app.Activity
import android.app.role.RoleManager
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.ab.agentx.call.ABMode
import com.ab.agentx.call.ABModeStore

class MainActivity : ComponentActivity() {
    companion object { private const val REQUEST_DEFAULT_DIALER = 1001 }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        ABModeStore.initialize(this)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    val mode = ABModeStore.getMode()
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("AB AgentX", style = MaterialTheme.typography.headlineMedium)
                        Text("AI Agentic Call Assistant")
                        Text("Current mode: $mode", Modifier.padding(top = 20.dp))
                        Button(onClick = { setMode(ABMode.NORMAL) }, Modifier.padding(top = 16.dp)) {
                            Text("Normal Mode — 20 sec")
                        }
                        Button(onClick = { setMode(ABMode.ACTIVE) }, Modifier.padding(top = 8.dp)) {
                            Text("Active Mode — Immediate")
                        }
                        Button(onClick = { setMode(ABMode.OFF) }, Modifier.padding(top = 8.dp)) {
                            Text("Turn AB Off")
                        }
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                            Button(onClick = ::requestDefaultDialer, Modifier.padding(top = 20.dp)) {
                                Text("Enable AB as Phone App")
                            }
                        }
                    }
                }
            }
        }
    }

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

    @Deprecated("Use Activity Result APIs in a later UI pass.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEFAULT_DIALER && resultCode == Activity.RESULT_OK) recreate()
    }
}
