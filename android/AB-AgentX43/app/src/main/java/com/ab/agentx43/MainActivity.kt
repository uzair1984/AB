package com.ab.agentx43

import android.Manifest
import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    private var phoneRole by mutableStateOf(false)
    private var number by mutableStateOf("")
    private var mode by mutableStateOf("NORMAL")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        phoneRole = isDefaultDialer()
        intent?.data?.let { if (it.scheme == "tel") number = it.schemeSpecificPart.orEmpty() }
        setContent { AgentXScreen() }
    }

    override fun onResume() { super.onResume(); phoneRole = isDefaultDialer() }

    @Composable
    private fun AgentXScreen() {
        val bg = Brush.verticalGradient(listOf(Color(0xFF07111F), Color(0xFF0A1D31), Color(0xFF06101B)))
        Column(Modifier.fillMaxSize().background(bg).padding(horizontal = 18.dp)) {
            Row(Modifier.fillMaxWidth().padding(top = 18.dp), verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(52.dp).clip(CircleShape).background(Color(0xFF102D45)), contentAlignment = Alignment.Center) {
                    Text("AB", color = Color(0xFF20C7FF), fontWeight = FontWeight.Bold, fontSize = 19.sp)
                }
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("AB AgentX", color = Color.White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("AI AGENTIC CALL ASSISTANT", color = Color(0xFF7FA6BF), fontSize = 10.sp, letterSpacing = 1.4.sp)
                }
                Surface(shape = RoundedCornerShape(20.dp), color = if (phoneRole) Color(0xFF123C2B) else Color(0xFF2A2430)) {
                    Text(if (phoneRole) "PHONE ON" else "PHONE OFF", color = Color.White, fontSize = 10.sp, modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp))
                }
            }

            Spacer(Modifier.height(18.dp))
            Surface(Modifier.fillMaxWidth(), shape = RoundedCornerShape(28.dp), color = Color(0xFF0B2033)) {
                Column(Modifier.padding(18.dp)) {
                    Text(if (number.isEmpty()) "Ready for a call" else number, color = Color.White, fontSize = 27.sp, fontWeight = FontWeight.SemiBold)
                    Text("AB AgentX", color = Color(0xFF6D8EA6), fontSize = 12.sp, modifier = Modifier.padding(top = 5.dp))
                    Spacer(Modifier.height(12.dp))
                    val keys = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
                    keys.chunked(3).forEach { row ->
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                            row.forEach { k ->
                                FilledTonalButton(
                                    onClick = { number += k },
                                    modifier = Modifier.padding(4.dp).size(78.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.filledTonalButtonColors(containerColor = Color(0xFF102C43), contentColor = Color.White)
                                ) { Text(k, fontSize = 22.sp) }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
                        TextButton(onClick = { if (number.isNotEmpty()) number = number.dropLast(1) }) { Text("⌫", color = Color.White, fontSize = 22.sp) }
                        Button(onClick = { callNumber() }, modifier = Modifier.size(66.dp), shape = CircleShape, colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF20C7FF), contentColor = Color(0xFF03111D))) { Text("☎", fontSize = 25.sp) }
                        TextButton(onClick = { number = "" }) { Text("Clear", color = Color.White) }
                    }
                }
            }

            Spacer(Modifier.height(12.dp))
            Text("AB MODE", color = Color(0xFF7FA6BF), fontSize = 11.sp, letterSpacing = 1.8.sp, fontWeight = FontWeight.Bold)
            ModeRow("Normal Mode", "20 sec", mode == "NORMAL") { mode = "NORMAL" }
            ModeRow("Active Mode", "Immediate", mode == "ACTIVE") { mode = "ACTIVE" }
            ModeRow("Turn AB Off", "Disabled", mode == "OFF") { mode = "OFF" }

            Spacer(Modifier.height(8.dp))
            Button(
                onClick = { togglePhoneRole() },
                Modifier.fillMaxWidth().height(52.dp),
                shape = RoundedCornerShape(18.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF12344D), contentColor = Color.White)
            ) {
                Text(if (phoneRole) "Disable AB as Phone App" else "Enable AB as Phone App", fontWeight = FontWeight.SemiBold)
            }
        }
    }

    @Composable
    private fun ModeRow(title: String, detail: String, selected: Boolean, onClick: () -> Unit) {
        Surface(Modifier.fillMaxWidth().padding(top = 6.dp), shape = RoundedCornerShape(16.dp), color = if (selected) Color(0xFF12364E) else Color(0xFF0B1C2B)) {
            Row(Modifier.padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) { Text(title, color = Color.White, fontWeight = FontWeight.Medium); Text(detail, color = Color(0xFF7FA6BF), fontSize = 11.sp) }
                Switch(checked = selected, onCheckedChange = { if (it) onClick() })
            }
        }
    }

    private fun isDefaultDialer(): Boolean =
        android.os.Build.VERSION.SDK_INT >= 29 && getSystemService(RoleManager::class.java).isRoleHeld(RoleManager.ROLE_DIALER)

    private fun togglePhoneRole() {
        if (android.os.Build.VERSION.SDK_INT < 29) return
        val rm = getSystemService(RoleManager::class.java)
        if (rm.isRoleHeld(RoleManager.ROLE_DIALER)) startActivity(Intent(Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS))
        else startActivity(rm.createRequestRoleIntent(RoleManager.ROLE_DIALER))
    }

    private fun callNumber() {
        if (number.isBlank()) return
        if (checkSelfPermission(Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(arrayOf(Manifest.permission.CALL_PHONE), 700)
            return
        }
        getSystemService(TelecomManager::class.java).placeCall(Uri.parse("tel:$number"), null)
    }
}
