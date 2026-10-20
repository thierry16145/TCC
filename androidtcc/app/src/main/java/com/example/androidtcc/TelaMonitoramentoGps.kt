package com.example.androidtcc

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*

@Composable
fun TelaMonitoramentoGps() {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var localizacaoAtual by remember { mutableStateOf<Location?>(null) }
    var emRastreamento by remember { mutableStateOf(false) }

    var temPermissaoGps by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcherPermissao = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { concedido -> temPermissaoGps = concedido }

    DisposableEffect(emRastreamento, temPermissaoGps) {
        var locationCallback: LocationCallback? = null

        if (emRastreamento && temPermissaoGps) {
            val locationRequest = LocationRequest.Builder(
                Priority.PRIORITY_HIGH_ACCURACY,
                2000L
            ).build()

            locationCallback = object : LocationCallback() {
                override fun onLocationResult(result: LocationResult) {
                    localizacaoAtual = result.lastLocation
                }
            }

            try {
                fusedLocationClient.requestLocationUpdates(
                    locationRequest,
                    locationCallback,
                    Looper.getMainLooper()
                )
            } catch (_: SecurityException) { }
        }

        onDispose {
            locationCallback?.let { fusedLocationClient.removeLocationUpdates(it) }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        if (localizacaoAtual != null) {
            Text(text = "Latitude: ${localizacaoAtual?.latitude}")
            Text(text = "Longitude: ${localizacaoAtual?.longitude}")
            Text(text = "Velocidade: ${localizacaoAtual?.speed ?: 0f} m/s")
        } else {
            Text(text = if (emRastreamento) "Buscando sinal de GPS..." else "GPS desligado")
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = {
                if (!temPermissaoGps) {
                    launcherPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                } else {
                    emRastreamento = !emRastreamento
                }
            }
        ) {
            Text(text = if (emRastreamento) "Parar Rastreamento" else "Iniciar Rastreamento")
        }
    }
}