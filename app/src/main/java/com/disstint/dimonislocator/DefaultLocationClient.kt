package com.disstint.dimonislocator
import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.os.Looper
import android.util.Log
import androidx.annotation.RequiresPermission
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.Priority




//CLASS QUE RECIBE DATOS DE LOCALIZACIÓN
class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    @SuppressLint("MissingPermission")
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if (!context.hasLocationPermission()) {
                throw LocationClient.LocationException("No se han otorgado permisos")
            }

            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val estaGpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val estaRedActivada = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            if (!estaGpsActivado && !estaRedActivada) {
                throw LocationClient.LocationException("El GPS está desactivado")
            }

            val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, interval)
                .setMinUpdateIntervalMillis(interval)
                .build()


            val locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    result.locations.lastOrNull()?.let { location ->
                        trySend(location)
                    }
                }
            }

            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            awaitClose {
                client.removeLocationUpdates(locationCallback)
            }
        }
    }



}