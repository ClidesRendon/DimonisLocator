package com.disstint.dimonislocator
import android.content.Context
import android.location.Location
import android.location.LocationManager
import android.location.LocationRequest
import android.os.Looper
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationResult
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

//CLASS QUE RECIBE DATOS DE LOCALIZACIÓN
class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow @androidx.annotation.RequiresPermission(allOf = [android.Manifest.permission.ACCESS_FINE_LOCATION, android.Manifest.permission.ACCESS_COARSE_LOCATION]) {
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("No se han otorgado permisos")
            }
            //CREAR VARIABLES Y ASIGNAR VALORES DE NETWORK Y GPS
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val estaGpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val estaRedActivada = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            //IF DE CONFIRMACION DE VALORES
            if(!estaGpsActivado && !estaRedActivada){
                throw LocationClient.LocationException("El GPS está desactivado")
            }

            val request = com.google.android.gms.location.LocationRequest.create()
                .setInterval(interval)
                .setFastestInterval(interval)

            val locationCallback = object : LocationCallback(){
                override fun onLocationResult(result: LocationResult) {
                    super.onLocationResult(result)
                    result.locations.lastOrNull()?.let {location ->
                        launch { send(location) }
                    }
                }

            }

            //PEDIR ACTUALIZACIONES DE LOCALIZACIÓN AL CLIENTE
            client.requestLocationUpdates(
                request,
                locationCallback,
                Looper.getMainLooper()
            )

            //ESPERAR CIERRE DE ACTUALIZACIONES
            awaitClose{
                client.removeLocationUpdates { locationCallback }
            }

        }
    }
}