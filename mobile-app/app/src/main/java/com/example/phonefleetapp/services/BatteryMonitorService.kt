package com.example.phonefleetapp.services

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.SharedPreferences
import android.content.pm.ServiceInfo
import android.os.BatteryManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.edit
import com.example.phonefleetapp.BuildConfig
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException

class BatteryMonitorService : Service() {

    private val sharedPrefs: SharedPreferences by lazy {
        getSharedPreferences("DevicePrefs", MODE_PRIVATE)
    }

    private var previousChargingState: Boolean? = null
    private var batteryPercentage: Float = -1f

    private val batteryReceiver = object : BroadcastReceiver() {

        // Called when the BroadcastReceiver receives an Intent broadcast.
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.let {
                val level = it.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = it.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                val status = it.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
                sharedPrefs.edit {
                    putBoolean("is_charging", isCharging)
                }
                // Check if charging state has changed
                if (previousChargingState == null) {
                    previousChargingState = isCharging
                } else if (previousChargingState != isCharging) {
                    val json = prepareJsonPayLoad()
                    sendToBackend(json)
                    previousChargingState = isCharging
                }
                // Calculate battery percentage
                if (level != -1 && scale != -1 && scale != 0) {
                    batteryPercentage = (level.toFloat() / scale.toFloat()) * 100f
                    sharedPrefs.edit {
                        putFloat("battery_level", batteryPercentage)
                    }
                }
                // Check if battery level is low or high
                if (batteryPercentage <= 20 || batteryPercentage >= 80) {
                    val json = prepareJsonPayLoad()
                    sendToBackend(json)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()

        val batteryFilter = IntentFilter(Intent.ACTION_BATTERY_CHANGED)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            /* Required for Android 13 (API 33) and 14+ */
            registerReceiver(batteryReceiver, batteryFilter, RECEIVER_NOT_EXPORTED)
        } else {
            // For older devices
            registerReceiver(batteryReceiver, batteryFilter)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val notification = createNotification()

        // Check for Android 14 (API 34 / Upside Down Cake)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
            )
        } else {
            ServiceCompat.startForeground(
                this,
                1,
                notification,
                0
            )
        }
        return START_STICKY
    }

    override fun onDestroy() {
        try {
            unregisterReceiver(batteryReceiver)
        } catch (e: Exception) {
            // Receiver might not be registered
            Log.e("BatteryMonitorService", "Error unregistering receiver", e)
        }
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotification(): Notification {
        val channelId = "battery_monitor_id"
        val channelName = "Battery Monitor Service"

        // Create the NotificationChannel
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(channelId, channelName, importance).apply {
                description = "Monitoring battery.."
            }
            val manager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }

        return NotificationCompat.Builder(this, channelId)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Battery Monitor")
            .setContentText("Monitoring battery..")
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setOngoing(true)
            .build()
    }

    private fun prepareJsonPayLoad(): String {
        // Prepare JSON payload
        val gson = Gson()
        val batteryLevel = sharedPrefs.getFloat("battery_level", -1f)
        val chargingStatus = sharedPrefs.getBoolean("is_charging", false)

        val data = DeviceData(
            deviceId = sharedPrefs.getString("device_id", "") ?: "",
            nickname = sharedPrefs.getString("nickname", "") ?: "",
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            sim1 = sharedPrefs.getString("sim_1", "") ?: "",
            sim2 = sharedPrefs.getString("sim_2", "") ?: "",
            batteryLevel = batteryLevel,
            isCharging = chargingStatus,
            plugSlot = sharedPrefs.getInt("plug_slot", 1),
            ipAddress = sharedPrefs.getString("ip_address", "") ?: ""
        )

        return gson.toJson(data)
    }

    private fun sendToBackend(json: String) {
        // Send data to backend
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()
        val requestBody = json.toRequestBody(mediaType)

        val request = Request.Builder()
            .url(BuildConfig.BACKEND_URL) // Backend server ip
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("BatteryMonitorService", "Error sending data to backend", e)
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("BatteryMonitorService", "Data successfully sent to backend")
            }
        })
    }
}


data class DeviceData(
    @SerializedName("device_id")
    val deviceId: String,
    @SerializedName("nickname")
    val nickname: String,
    @SerializedName("device_model")
    val deviceModel: String,
    @SerializedName("sim_1")
    val sim1: String,
    @SerializedName("sim_2")
    val sim2: String,
    @SerializedName("battery_level")
    val batteryLevel: Float,
    @SerializedName("is_charging")
    val isCharging: Boolean,
    @SerializedName("plug_slot")
    val plugSlot: Int,
    @SerializedName("ip_address")
    val ipAddress: String,
)