package com.example.phonefleetapp

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.edit
import com.example.phonefleetapp.services.BatteryMonitorService
import com.example.phonefleetapp.ui.theme.PhoneFleetAppTheme
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID


class MainActivity : ComponentActivity() {

    lateinit var sharedPrefs: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        sharedPrefs = getSharedPreferences("DevicePrefs", MODE_PRIVATE)

        if (sharedPrefs.getString("device_id", "") == "") {
            // Generate UUID if it doesn't exist
            val uniqueID = UUID.randomUUID().toString()
            sharedPrefs.edit { putString("device_id", uniqueID) }
        }

        if (sharedPrefs.getString("ip_address", "") == "") {
            // Generate IP address if it doesn't exist
            val ipAddress = getLocalIpAddress()
            sharedPrefs.edit { putString("ip_address", ipAddress) }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Check for POST_NOTIFICATIONS permission
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                )
                != PackageManager.PERMISSION_GRANTED
            ) {
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                    101 // Unique request code
                )
            }
        }

        val intent = Intent(this, BatteryMonitorService::class.java)
        ContextCompat.startForegroundService(this, intent)

        enableEdgeToEdge()
        setContent {
            PhoneFleetAppTheme {
                Scaffold(
                    modifier = Modifier.fillMaxSize()
                ) { innerPadding ->
                    AppLayout(
                        modifier =
                            Modifier.padding(innerPadding), prefs = sharedPrefs
                    )
                }
            }
        }
    }
}

private fun getLocalIpAddress(): String {
    try {
        // Get all network interfaces
        val interfaces = Collections.list(NetworkInterface.getNetworkInterfaces())
        for (intf in interfaces) {
            val addresses = Collections.list(intf.inetAddresses)
            for (addr in addresses) {
                // Check if it's IPv4 and NOT the loopback (127.0.0.1)
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    val sAddr = addr.hostAddress ?: continue // Skip if null safely

                    // Check if it's a private IP address
                    if (sAddr.startsWith("192.168.")) {
                        return sAddr
                    }
                }
            }
        }
    } catch (ex: Exception) {
        // Log the error so you can see it in Android Studio Logcat
        Log.e("MainActivity", "Error getting IP: ${ex.message}")
    }
    return "0.0.0.0"
}
@Composable
fun AppLayout(modifier: Modifier, prefs: SharedPreferences) {
    // Device information
    val deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}"
    var nickname by rememberSaveable {
        mutableStateOf(
            prefs.getString("nickname", "") ?: ""
        )
    }
    var sim1 by rememberSaveable {
        mutableStateOf(
            prefs.getString("sim_1", "") ?: ""
        )
    }
    var sim2 by rememberSaveable {
        mutableStateOf(
            prefs.getString("sim_2", "") ?: ""
        )
    }
    var plugSlot by rememberSaveable {
        mutableIntStateOf(
            prefs.getInt("plug_slot", 1)
        )
    }
    var expanded by rememberSaveable { mutableStateOf(false) }

    Surface(modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        )
        {
            Text(
                deviceModel,
                fontSize = 20.sp
            )
            OutlinedTextField(
                value = nickname,
                onValueChange = {
                    nickname = it
                    prefs.edit { putString("nickname", nickname) }
                },
                label = { Text("Nickname") },
                singleLine = true
            )
            OutlinedTextField(
                value = sim1,
                onValueChange = {
                    sim1 = it
                    prefs.edit { putString("sim_1", sim1) }
                },
                label = { Text("SIM #1") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )
            OutlinedTextField(
                value = sim2,
                onValueChange = {
                    sim2 = it
                    prefs.edit { putString("sim_2", sim2) }
                },
                label = { Text("SIM #2") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                singleLine = true
            )

            Box(
                modifier = Modifier
                    .padding(16.dp)
            ) {
                Button(onClick = { expanded = !expanded }) {
                    Text("Plug Slot $plugSlot")
                }
                DropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }) {
                    DropdownMenuItem(
                        text = { Text("Plug 1") },
                        onClick = {
                            plugSlot = 1
                            expanded = false
                            prefs.edit {
                                putInt("plug_slot", 1)
                            }
                        })
                    DropdownMenuItem(
                        text = { Text("Plug 2") },
                        onClick = {
                            plugSlot = 2
                            expanded = false
                            prefs.edit {
                                putInt("plug_slot", 2)
                            }
                        })
                    DropdownMenuItem(
                        text = { Text("Plug 3") },
                        onClick = {
                            plugSlot = 3
                            expanded = false
                            prefs.edit {
                                putInt("plug_slot", 3)
                            }
                        })
                }
            }
        }
    }
}


@Preview
@Composable
fun AppLayoutPreview() {
    val context = LocalContext.current
    // This creates a temporary, empty set of prefs just for the preview
    val fakePrefs = context.getSharedPreferences("preview_prefs", Context.MODE_PRIVATE)
    AppLayout(prefs = fakePrefs, modifier = Modifier)
}