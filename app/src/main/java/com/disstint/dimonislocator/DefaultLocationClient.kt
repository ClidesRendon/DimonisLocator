package com.disstint.dimonislocator

import android.content.Context
import android.location.Location
import android.location.LocationManager
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow

class DefaultLocationClient(
    private val context: Context,
    private val client: FusedLocationProviderClient
): LocationClient {
    override fun getLocationUpdates(interval: Long): Flow<Location> {
        return callbackFlow {
            if(!context.hasLocationPermission()) {
                throw LocationClient.LocationException("No se han otorgado permisos")
            }
            //CREAR VARIABLES Y ASIGNAR VALORES DE NETWORK Y GPS
            val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val estaGpsActivado = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
            val estaRedActivada = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)

            //IF DE CONFIRMACION DE VALORES
            if(!estaGpsActivado && !estaRedActivada){

            }

        }
    }
}