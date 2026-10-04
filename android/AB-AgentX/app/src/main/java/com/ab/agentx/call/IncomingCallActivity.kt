package com.ab.agentx.call

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

class IncomingCallActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(Modifier.fillMaxSize()) {
                    Column(
                        Modifier.fillMaxSize().padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text("AB AgentX", style = MaterialTheme.typography.headlineMedium)
                        Text("Incoming call", Modifier.padding(top = 8.dp))
                        Button(
                            onClick = {
                                CallRegistry.answer()
                                finish()
                            },
                            Modifier.padding(top = 24.dp)
                        ) { Text("Answer") }
                        Button(
                            onClick = {
                                CallRegistry.end()
                                finish()
                            },
                            Modifier.padding(top = 8.dp)
                        ) { Text("Reject") }
                    }
                }
            }
        }
    }
}
