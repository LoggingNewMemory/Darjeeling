package com.kanagawa.yamada.darjeeling

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.kanagawa.yamada.darjeeling.ui.theme.DarjeelingTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DarjeelingTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    CaffeineSettingsScreen(modifier = Modifier.padding(innerPadding))
                }
            }
        }
    }
}

@Composable
fun CaffeineSettingsScreen(modifier: Modifier = Modifier) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val prefs = context.getSharedPreferences("caffeine_prefs", Context.MODE_PRIVATE)
    
    var useRoot by remember { mutableStateOf(prefs.getBoolean("use_root", false)) }
    
    Column(modifier = modifier.padding(16.dp).fillMaxSize()) {
        Text(text = "Caffeine Tile Settings", style = MaterialTheme.typography.headlineMedium)
        Spacer(modifier = Modifier.height(16.dp))
        
        Row {
            RadioButton(
                selected = !useRoot,
                onClick = { 
                    useRoot = false
                    prefs.edit().putBoolean("use_root", false).apply()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Non-Root Method (Requires WRITE_SETTINGS permission)", modifier = Modifier.padding(top=12.dp))
        }
        
        Button(
            onClick = {
                val intent = Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS)
                intent.data = Uri.parse("package:${context.packageName}")
                context.startActivity(intent)
            },
            enabled = !useRoot
        ) {
            Text("Grant WRITE_SETTINGS Permission")
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Row {
            RadioButton(
                selected = useRoot,
                onClick = { 
                    useRoot = true
                    prefs.edit().putBoolean("use_root", true).apply()
                }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text("Root Method", modifier = Modifier.padding(top=12.dp))
        }
        
        Button(
            onClick = {
                try {
                    Runtime.getRuntime().exec(arrayOf("su", "-c", "echo 'Root access granted'")).waitFor()
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            },
            enabled = useRoot
        ) {
            Text("Test Root Access")
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text("Usage:", style = MaterialTheme.typography.titleLarge)
        Text("1. Add the Caffeine tile to your Quick Settings panel.")
        Text("2. Tap to cycle: 5m, 10m, 15m, 30m.")
        Text("3. Tapping the tile while a timer is running will turn it off.")
        Text("4. Long press the tile to set to Unlimited.")
        Text("5. If the screen turns off naturally or via power button, caffeine will be automatically disabled.")
    }
}