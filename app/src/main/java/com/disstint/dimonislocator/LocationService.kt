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
import kotlinx.coroutines.Job // Importar Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException


class LocationService : Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private lateinit var locationClient: LocationClient
    private val httpClient = OkHttpClient() // Instancia única del cliente HTTP para todas las peticiones

    private var locationUpdateJob: Job? = null // Job para controlar las actualizaciones de ubicación

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
            ACTION_CLEAR_LOCATION -> { // Acción para borrar coordenadas
                Log.d("LocationService", "Solicitud de borrado de coordenadas...")
                clearLocation()
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
            .setSmallIcon(R.drawable.ic_launcher_background) // Asegúrate de que este recurso exista
            .setOngoing(true)

        val notificationManager =
            getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(1, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION)
        } else {
            startForeground(1, notificationBuilder.build())
        }

        Log.d("Ubicacion", "Solicitando actualizaciones de ubicación al FusedLocationProviderClient")

        // Cancelar cualquier Job de actualización anterior antes de iniciar uno nuevo
        locationUpdateJob?.cancel()
        locationUpdateJob = locationClient
            .getLocationUpdates(5000L) // Intervalo de 5 segundos
            .catch { e ->
                Log.e("Ubicacion", "Error en actualización de ubicación", e)
                // Puedes mostrar un Toast o notificación de error aquí
            }
            .onEach { location ->
                Log.d("Ubicacion", "Recibida nueva localización: ${location.latitude}, ${location.longitude}")

                val json = """
                {
                    "lat": ${location.latitude},
                    "lon": ${location.longitude}
                }
            """.trimIndent()

                // URL de tu servidor para actualizar la ubicación
                // *** ASEGÚRATE DE QUE ESTA URL ES CORRECTA (HTTPS) ***
                val request = Request.Builder()
                    .url("https://santantonimanacor.disstintbeta.com/update-location")
                    .post(json.toRequestBody("application/json".toMediaType()))
                    .build()

                httpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e("Red", "Error enviando ubicación", e)
                        // Toast.makeText(applicationContext, "Error de red al enviar ubicación", Toast.LENGTH_SHORT).show()
                    }

                    override fun onResponse(call: Call, response: Response) {
                        Log.d("Red", "Ubicación enviada correctamente. Código: ${response.code}")
                        response.close() // Cerrar la respuesta para liberar recursos
                    }
                })

                val lat = location.latitude
                val lon = location.longitude
                val updatedNotification = notificationBuilder
                    .setContentText("Localización: ($lat, $lon)")
                notificationManager.notify(1, updatedNotification.build())
            }
            .launchIn(serviceScope) // Lanzar la corrutina dentro del serviceScope
    }

    // Función para crear el canal de notificación (necesario en Android 8.0+)
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

    // Función para detener el servicio de localización COMPLETAMENTE
    private fun stop() {
        locationUpdateJob?.cancel() // Detener las actualizaciones de ubicación
        locationUpdateJob = null // Limpiar la referencia al Job
        stopForeground(true) // Detiene el servicio en primer plano y elimina la notificación
        stopSelf() // Detiene el propio servicio
        Log.d("LocationService", "Servicio de localización detenido completamente.")
    }

    // NUEVA FUNCIÓN: Borrar coordenadas en el servidor sin detener el servicio completamente
    private fun clearLocation() {
        // Detener solo las actualizaciones de ubicación si están activas
        // No detener el servicio en primer plano ni el servicio en sí
        locationUpdateJob?.cancel()
        locationUpdateJob = null
        Log.d("LocationService", "Actualizaciones de ubicación pausadas para borrado.")

        // URL de tu servidor para borrar las coordenadas
        // *** ASEGÚRATE DE QUE ESTA URL ES CORRECTA (HTTPS) ***
        val request = Request.Builder()
            .url("https://santantonimanacor.disstintbeta.com/clear-location")
            // Cuerpo de petición POST vacío, compatible con diferentes versiones de OkHttp
            .post(RequestBody.create(null, ByteArray(0)))
            .build()

        httpClient.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("Red", "Error al borrar ubicación en el servidor", e)
                Toast.makeText(applicationContext, "Error al borrar coordenadas", Toast.LENGTH_SHORT).show()
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d("Red", "Coordenadas borradas correctamente. Código: ${response.code}")
                response.close() // Cerrar la respuesta
                Toast.makeText(applicationContext, "Coordenadas borradas", Toast.LENGTH_SHORT).show()
            }
        })
        // No se llama a stopForeground(true) ni stopSelf() aquí
        // para que el servicio siga ejecutándose y la app no se minimice.
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel() // Asegurarse de cancelar el scope si el servicio se destruye
        Log.d("LocationService", "Servicio destruido.")
    }

    companion object {
        const val ACTION_START = "ACTION_START"
        const val ACTION_STOP = "ACTION_STOP"
        const val ACTION_CLEAR_LOCATION = "ACTION_CLEAR_LOCATION" // Nueva constante de acción
    }
}
