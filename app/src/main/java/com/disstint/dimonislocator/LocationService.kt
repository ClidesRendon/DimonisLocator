package com.disstint.dimonislocator

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

        locationClient
            .getLocationUpdates(5000L)
            .catch { it.printStackTrace() }
            .onEach { location ->

                println("📍 Recibida nueva localización: ${location.latitude}, ${location.longitude}")
                Toast.makeText(this, "Ubicación: ${location.latitude}, ${location.longitude}", Toast.LENGTH_SHORT).show()

                val client = OkHttpClient()

                /*val jsonObj = JSONObject().apply {
                    put("lat", location.latitude)
                    put("lon", location.longitude)
                }
                val json = jsonObj.toString()*/

                val jsonObj = JSONObject().apply {
                    put("lat", location.latitude)
                    put("lon", location.longitude)
                }
                val json = jsonObj.toString()

                println("📦 JSON generado: $json")  // <-- Log para depuración



                /*val request = Request.Builder()
                    .url("http://10.0.2.2:3001/update-location") // ✅ URL correcta para emulador
                    .post(json.toRequestBody("application/json".toMediaType())) // ✅ Content-Type correcto
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        e.printStackTrace()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("✅ Ubicación enviada: $json")
                    }
                })*/

                val request = Request.Builder()
                    .url("http://10.0.2.2:3001/update-location")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                client.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        println("❌ Error al enviar ubicación: ${e.message}")
                    }

                    override fun onResponse(call: Call, response: Response) {
                        println("✅ Código de respuesta: ${response.code}")
                        println("✅ Cuerpo de respuesta: ${response.body?.string()}")
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



