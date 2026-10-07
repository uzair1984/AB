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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.foundation.Image
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ab.agentx.call.ABMode
import com.ab.agentx.call.ABModeStore

private val Ink = Color(0xFF06111F)
private val Panel = Color(0xFF0B1B2B)
private val Panel2 = Color(0xFF10263A)
private val Cyan = Color(0xFF29C7FF)
private val Muted = Color(0xFF91A8BB)
private val White = Color(0xFFF4FAFF)

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
                Surface(color = Ink, modifier = Modifier.fillMaxSize()) {
                    AgentXHome()
                }
            }
        }
    }

    @androidx.compose.runtime.Composable
    private fun AgentXHome() {
        val mode = ABModeStore.getMode()
        val isPhoneApp = phoneRoleEnabled.value
        val scroll = rememberScrollState()

        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF06111F), Color(0xFF081827), Color(0xFF04101C))
                    )
                )
                .verticalScroll(scroll)
                .padding(horizontal = 18.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(com.ab.agentx.R.drawable.ab_agentx_icon),
                    contentDescription = "AB AgentX",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(52.dp).clip(RoundedCornerShape(16.dp))
                )
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text("AB AgentX", color = White, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                    Text("AI AGENTIC CALL ASSISTANT", color = Muted, fontSize = 10.sp, letterSpacing = 1.6.sp)
                }
                StatusDot(active = isPhoneApp)
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                modifier = Modifier.fillMaxWidth().border(1.dp, Color(0xFF18364D), RoundedCornerShape(24.dp))
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("AGENT STATUS", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 7.dp)) {
                        Text(
                            if (isPhoneApp) "Ready to assist" else "Setup required",
                            color = White, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Text(if (isPhoneApp) "ONLINE" else "OFFLINE", color = Cyan, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (isPhoneApp) "AB can receive and manage incoming calls." else "Set AB AgentX as the default phone app to activate call handling.",
                        color = Muted, fontSize = 13.sp, modifier = Modifier.padding(top = 5.dp)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel2),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("AB MODE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text("Choose how AB handles calls", color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(10.dp))
                    ModeCard("Normal Mode", "Answers after 20 seconds", mode == ABMode.NORMAL) { setMode(ABMode.NORMAL) }
                    ModeCard("Active Mode", "Answers immediately", mode == ABMode.ACTIVE) { setMode(ABMode.ACTIVE) }
                    ModeCard("AB Off", "Do not auto-answer calls", mode == ABMode.OFF) { setMode(ABMode.OFF) }
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("PHONE APP CONTROL", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text(
                        if (isPhoneApp) "AB AgentX is your default phone app" else "AB AgentX is not the default phone app",
                        color = White, fontSize = 16.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                    Button(
                        onClick = ::requestDefaultDialer,
                        modifier = Modifier.fillMaxWidth().padding(top = 12.dp),
                        shape = RoundedCornerShape(15.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                    ) {
                        Text(if (isPhoneApp) "Manage Default Phone App" else "Enable AB as Phone App", fontWeight = FontWeight.Bold)
                    }
                    Text(
                        if (isPhoneApp) "To disable AB, choose another phone app in Android's system settings."
                        else "Android will confirm the default-phone-app change.",
                        color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 7.dp)
                    )
                }
            }

            Card(
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = Panel),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(Modifier.padding(18.dp)) {
                    Text("PHONE", color = Muted, fontSize = 10.sp, letterSpacing = 1.5.sp)
                    Text(
                        if (number.value.isBlank()) "Enter a number" else number.value,
                        color = White, fontSize = 22.sp, fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 7.dp)
                    )
                    DialPad()
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedButton(
                            onClick = { if (number.value.isNotEmpty()) number.value = number.value.dropLast(1) },
                            modifier = Modifier.weight(1f), shape = RoundedCornerShape(14.dp)
                        ) { Text("Delete") }
                        Button(
                            onClick = ::placeCall, modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Cyan, contentColor = Ink)
                        ) { Text("Call", fontWeight = FontWeight.Bold) }
                    }
                }
            }

            Text(
                "AB AgentX • Android-first AI call control",
                color = Color(0xFF60788C), fontSize = 11.sp,
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp)
            )
        }
    }

    @androidx.compose.runtime.Composable
    private fun StatusDot(active: Boolean) {
        Box(
            modifier = Modifier.size(12.dp).clip(CircleShape)
                .background(if (active) Cyan else Color(0xFF455B6B))
        )
    }

    @androidx.compose.runtime.Composable
    private fun ModeCard(title: String, subtitle: String, selected: Boolean, onClick: () -> Unit) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 5.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(if (selected) Color(0xFF123B52) else Color(0xFF0A1927))
                .border(1.dp, if (selected) Cyan else Color(0xFF173249), RoundedCornerShape(16.dp))
                .clickable(onClick = onClick)
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(subtitle, color = Muted, fontSize = 11.sp, modifier = Modifier.padding(top = 2.dp))
            }
            Switch(checked = selected, onCheckedChange = { if (it) onClick() })
        }
    }

    @androidx.compose.runtime.Composable
    private fun DialPad() {
        val digits = listOf("1","2","3","4","5","6","7","8","9","*","0","#")
        Column(verticalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(bottom = 10.dp)) {
            digits.chunked(3).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { digit ->
                        OutlinedButton(
                            onClick = { number.value += digit },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(13.dp)
                        ) { Text(digit, fontSize = 16.sp) }
                    }
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        phoneRoleEnabled.value = isDefaultDialer()
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
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CALL_PHONE), REQUEST_CALL_PHONE)
            return
        }
        getSystemService(TelecomManager::class.java).placeCall(Uri.fromParts("tel", target, null), null)
    }

    @Deprecated("Use Activity Result APIs in a later UI pass.")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_DEFAULT_DIALER || (requestCode == REQUEST_CALL_PHONE && resultCode == Activity.RESULT_OK)) {
            phoneRoleEnabled.value = isDefaultDialer()
        }
    }
}
