package com.disstint.dimonislocator

import android.Manifest
import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices // Asegúrate de que esta importación esté presente
import kotlinx.coroutines.launch // Importar para usar corrutinas
import kotlinx.coroutines.delay // Importar para usar delay
import android.util.Log // Importar para usar Log.d

class MainActivity : ComponentActivity() {

    private val PERMISSION_REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Solicitar permisos de ubicación al inicio de la actividad
        ActivityCompat.requestPermissions(
            this,
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ),
            1234
        )

        // Verificar y solicitar permisos requeridos si no están concedidos
        if (!hasRequiredPermissions()) {
            ActivityCompat.requestPermissions(
                this,
                getRequiredPermissions().toTypedArray(),
                PERMISSION_REQUEST_CODE
            )
        }

        setContent {
            // Estado para controlar si el seguimiento de localización está activo (enviando coordenadas)
            // Se inicializa comprobando si el servicio está corriendo (lo que implica que estaba activo al iniciar la app)
            var isTrackingActive by remember { mutableStateOf(isLocationServiceRunning()) }

            // Scope de corrutinas para lanzar tareas asíncronas en el Composable
            val scope = rememberCoroutineScope()

            // Columna principal para organizar los elementos de la UI
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(100.dp),
                verticalArrangement = Arrangement.Center, // Centrar verticalmente
                horizontalAlignment = Alignment.CenterHorizontally // Centrar horizontalmente
            ) {
                // Texto que indica el estado del seguimiento
                Text(
                    text = if (isTrackingActive) "Seguimiento activo" else "Seguimiento detenido",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 24.dp)
                )

                Spacer(modifier = Modifier.height(16.dp)) // Espacio entre botones

                // Botón "Arrancar Seguiment"
                Button(
                    onClick = {
                        if (hasRequiredPermissions()) {
                            Intent(applicationContext, LocationService::class.java).apply {
                                action = LocationService.ACTION_START
                                ContextCompat.startForegroundService(applicationContext, this)
                            }
                            isTrackingActive = true // Actualizar el estado a activo
                        } else {
                            Toast.makeText(this@MainActivity, "Permisos no concedidos", Toast.LENGTH_LONG).show()
                        }
                    },
                    enabled = !isTrackingActive // Habilitado si el seguimiento no está activo
                ) {
                    Text(text = "Arrancar Seguiment")
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espacio entre botones

                // Botón "Aturar Seguiment"
                Button(
                    onClick = {
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_STOP
                            ContextCompat.startForegroundService(applicationContext, this)
                        }
                        isTrackingActive = false // Actualizar el estado a inactivo
                    },
                    enabled = isTrackingActive // Habilitado si el seguimiento está activo
                ) {
                    Text(text = "Aturar Seguiment")
                }

                Spacer(modifier = Modifier.height(16.dp)) // Espacio entre botones

                // Botón "Borrar Coordenades"
                Button(
                    onClick = {
                        // Enviar una intención para borrar las coordenadas
                        Intent(applicationContext, LocationService::class.java).apply {
                            action = LocationService.ACTION_CLEAR_LOCATION // Nueva acción
                            ContextCompat.startForegroundService(applicationContext, this)
                        }
                        Toast.makeText(this@MainActivity, "Enviando solicitud para borrar coordenadas...", Toast.LENGTH_SHORT).show()

                        // Si el seguimiento estaba activo, lo marcamos como inactivo porque el borrado lo detiene
                        if (isTrackingActive) {
                            isTrackingActive = false
                            Log.d("MainActivity", "Seguimiento marcado como inactivo después de solicitud de borrado.")
                        }

                        // Re-evaluar el estado del servicio después de un breve retraso
                        // Esto ayuda a que la UI refleje el estado real del servicio (si el proceso sigue vivo)
                        // pero NO cambia el estado de 'isTrackingActive' aquí, para evitar activarlo falsamente.
                        scope.launch {
                            delay(500) // Pequeño retraso para permitir que el servicio procese la intención
                            Log.d("MainActivity", "Proceso de servicio en ejecución después de solicitud de borrado: ${isLocationServiceRunning()}")
                        }
                    },
                    enabled = true // El botón de borrar coordenadas siempre estará habilitado
                ) {
                    Text(text = "Borrar Coordenades")
                }
            }
        }
    }

    // Función para obtener la lista de permisos requeridos
    private fun getRequiredPermissions(): List<String> {
        val permissions = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.FOREGROUND_SERVICE
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            permissions.add(Manifest.permission.ACCESS_BACKGROUND_LOCATION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            permissions.add(Manifest.permission.FOREGROUND_SERVICE_LOCATION)
        }
        return permissions
    }

    // Función para verificar si todos los permisos requeridos están concedidos
    private fun hasRequiredPermissions(): Boolean {
        return getRequiredPermissions().all {
            ContextCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
        }
    }

    // Función para verificar si el LocationService está corriendo (el proceso)
    private fun isLocationServiceRunning(): Boolean {
        val manager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        return manager.getRunningServices(Int.MAX_VALUE).any {
            it.service.className == LocationService::class.java.name
        }
    }

    // Manejo de la respuesta a la solicitud de permisos
    @Deprecated("This method has been deprecated in favor of using the Activity Result API...")
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                Toast.makeText(this, "Permisos concedidos", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Permisos denegados", Toast.LENGTH_LONG).show()
            }
        }
    }
}


