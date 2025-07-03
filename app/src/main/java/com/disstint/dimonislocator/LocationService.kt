package com.disstint.dimonislocator

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.util.Log
import android.widget.Toast
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException


class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient

    override fun onBind(intent: Intent?): IBinder? = null



    override fun onCreate() {



        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }


    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("LocationService", "onStartCommand recibido: ${intent?.action}")

        when (intent?.action) {
            ACTION_START -> {
                Log.d("LocationService", "Iniciando seguimiento...")
                start()
            }
            ACTION_STOP -> {
                Log.d("LocationService", "Deteniendo seguimiento...")
                stop()
            }
        }
        return START_STICKY
    }



    private fun start() {
        Log.d("Ubicacion", "Solicitud de actualizaciones de ubicación iniciada")
        Log.d("Ubicacion", "Permiso FINE LOCATION: ${ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)}")
        Log.d("Ubicacion", "Permiso COARSE LOCATION: ${ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)}")


        createNotificationChannel()

        val notificationBuilder = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notificationBuilder.build())
        }

        Log.d("Ubicacion", "Solicitando actualizaciones de ubicación al FusedLocationProviderClient")

        locationClient
            .getLocationUpdates(5000L)
            .catch { e ->
                Log.e("Ubicacion", "Error en actualización de ubicación", e)
            }
            .onEach { location ->
                Log.d("Ubicacion", "Recibida nueva localización: ${location.latitude}, ${location.longitude}")

                val client = OkHttpClient()

                val json = """
                {
                    "lat": ${location.latitude},
                    "lon": ${location.longitude}
                }
            """.trimIndent()

                val request = Request.Builder()
                    .url("https://santantonimanacor.disstintbeta.com/update-location")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("Red", "Error enviando ubicación", e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d("Red", "Ubicación enviada correctamente: $json")
                    }
                })

                val lat = location.latitude
                val lon = location.longitude
                val updatedNotification = notificationBuilder
                    .setContentText("Localización: ($lat, $lon)")
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope)
    }


    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "location",
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(channel)
        }
    }

    private fun stop() {
        stopForeground(true)
        stopSelf()
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
    }

}



