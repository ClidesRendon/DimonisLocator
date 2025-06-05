package com.disstint.dimonislocator

import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach


class LocationService: Service() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private lateinit var  locationClient: LocationClient

    override fun onBind(intent: Intent?): IBinder? {
        return null
    }

    override fun onCreate() {
        super.onCreate()
        locationClient = DefaultLocationClient(
            applicationContext,
            LocationServices.getFusedLocationProviderClient(applicationContext)
        )
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when(intent?.action) {
            ACTION_START -> start()
            ACTION_STOP -> stop()
        }
        return super.onStartCommand(intent, flags, startId)
    }

    //FUNCION START
    private fun start() {

        //CREAR NOTIFICACION
        val notification = NotificationCompat.Builder(this, "location")
            .setContentTitle("Tracking location...")
            .setContentText("Location: null")
            .setSmallIcon(R.drawable.ic_launcher_background)
            .setOngoing(true)

        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        locationClient
            .getLocationUpdates(5000L) //ACTUALIZACION DE LOCATION CADA 5 SEGUNDOS
            .catch { e -> e.printStackTrace() } //HACER PRINT DE POSIBLES EXCEPCIONES
            .onEach { location ->
                val lat = location.latitude.toString() //OBTENER LATITUD Y CONVERTIR A STRING
                val long = location.longitude.toString() //OBTENER LONGITUD Y CONVERTIR A STRING
                val updatedNotification = notification.setContentText(
                    "Localizacion: ($lat,$long)"
                )
                notificationManager.notify(1,updatedNotification.build())
            }
            .launchIn(serviceScope)

        startForeground(1,notification.build())

    }


    //FUNCION STOP
    private fun stop() {
        stopForeground(true)
        stopSelf()

    }

    // FUNCION PARA CANCELARLO CUANDO SE DESTRUYA SERVER
    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }

    //CREACION OBJETOS PARA ARRANCAR Y PARAR TRACKING
    companion object {
        const val  ACTION_START = "ACTION_START"
        const val  ACTION_STOP = "ACTION_STOP"
    }
}