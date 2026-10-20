package com.example.androidtcc

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.location.Location
import android.os.Looper
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.example.androidtcc.data.ApiService
import com.example.androidtcc.data.UsuarioDto
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMapOptions
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// 1. DADOS DA JORNADA (INCLUI O DESTINO)
data class ItemJornadaCard(
    val id: Long,
    val titulo: String, // Usaremos para guardar o Destino
    val horarioInicio: String,
    val horarioFim: String,
    val duracao: String,
    val distanciaKm: String,
    val rota: List<LatLng>
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelJornada(
    api: ApiService,
    preferencias: SharedPreferences,
    usuario: UsuarioDto,
    onSair: () -> Unit
) {
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }

    var jornadaId by remember { mutableStateOf<Long?>(null) }
    var carregando by remember { mutableStateOf(false) }

    var painelGpsVisivel by remember { mutableStateOf(false) }
    var emAndamento by remember { mutableStateOf(false) }

    // 2. NOVO: CAMPO DE DESTINO
    var destinoInput by remember { mutableStateOf("") }

    val rotaPontos = remember { mutableStateListOf<LatLng>() }
    var horarioInicioDate by remember { mutableStateOf<Date?>(null) }
    var distanciaTotalMetros by remember { mutableFloatStateOf(0f) }
    var velocidadeAtualKmh by remember { mutableFloatStateOf(0f) }

    val listaHistorico = remember { mutableStateListOf<ItemJornadaCard>() }

    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(LatLng(-23.5505, -46.6333), 15f)
    }
    val coroutineScope = rememberCoroutineScope()

    var temPermissaoGps by remember {
        mutableStateOf(ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED)
    }

    val launcherPermissao = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
        temPermissaoGps = concedido
    }

    val locationCallback = remember {
        object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                if (!emAndamento) return
                for (location in result.locations) {
                    val novaPos = LatLng(location.latitude, location.longitude)

                    if (rotaPontos.isNotEmpty()) {
                        val ultimo = rotaPontos.last()
                        val res = FloatArray(1)
                        Location.distanceBetween(ultimo.latitude, ultimo.longitude, novaPos.latitude, novaPos.longitude, res)
                        distanciaTotalMetros += res[0]
                    }

                    rotaPontos.add(novaPos)
                    if (location.hasSpeed()) {
                        velocidadeAtualKmh = location.speed * 3.6f
                    }

                    coroutineScope.launch {
                        cameraPositionState.animate(CameraUpdateFactory.newLatLngZoom(novaPos, 16f))
                    }
                }
            }
        }
    }

    @SuppressLint("MissingPermission")
    fun iniciarRastreio() {
        if (!temPermissaoGps) {
            launcherPermissao.launch(Manifest.permission.ACCESS_FINE_LOCATION)
            return
        }

        // Se o destino estiver vazio, colocamos um genérico
        val destinoFinal = if(destinoInput.isBlank()) "Jornada Livre" else "Destino: $destinoInput"

        horarioInicioDate = Date()
        distanciaTotalMetros = 0f
        velocidadeAtualKmh = 0f
        rotaPontos.clear()
        emAndamento = true

        val request = LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper())
    }

    fun finalizarRastreio() {
        emAndamento = false
        fusedLocationClient.removeLocationUpdates(locationCallback)

        val dataFim = Date()
        val dataIn = horarioInicioDate ?: dataFim

        val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val diffMil = dataFim.time - dataIn.time
        val min = (diffMil / (1000 * 60)).toInt()

        val distKmStr = String.format("%.2f km", distanciaTotalMetros / 1000f)

        // Adiciona na lista de histórico local (tela inicial) usando o Destino
        val destinoFinal = if(destinoInput.isBlank()) "Jornada Livre" else destinoInput

        listaHistorico.add(
            0,
            ItemJornadaCard(
                id = System.currentTimeMillis(),
                titulo = destinoFinal,
                horarioInicio = sdf.format(dataIn),
                horarioFim = sdf.format(dataFim),
                duracao = "${min}m",
                distanciaKm = distKmStr,
                rota = rotaPontos.toList()
            )
        )

        // Limpa para a próxima
        destinoInput = ""
        painelGpsVisivel = false
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().background(Color(0xFFF8F9FA))) {

            // --- TOPO (Header) ---
            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 24.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.size(48.dp).background(Color(0xFF4A65FF), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Text(text = usuario.nome.take(1).uppercase(), color = Color.White, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = "Olá, ${usuario.nome}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color(0xFF1E293B))
                    Text(text = usuario.email, fontSize = 14.sp, color = Color(0xFF64748B))
                }
                IconButton(onClick = onSair) {
                    Icon(imageVector = Icons.Default.ExitToApp, contentDescription = "Sair", tint = Color(0xFF64748B))
                }
            }

            // --- LISTA DE JORNADAS REGISTRADAS ---
            Text(
                text = "Jornadas Registradas",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = Color(0xFF1E293B),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (listaHistorico.isEmpty()) {
                Box(modifier = Modifier.fillMaxWidth().weight(1f), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nenhuma jornada registrada ainda.\nClique no botão '+' abaixo para iniciar.",
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(1),
                    modifier = Modifier.fillMaxWidth().weight(1f),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(listaHistorico) { item ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(text = item.titulo, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                Spacer(modifier = Modifier.height(8.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Início: ${item.horarioInicio}", fontSize = 14.sp, color = Color.DarkGray)
                                    Text(text = "Fim: ${item.horarioFim}", fontSize = 14.sp, color = Color.DarkGray)
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(text = "Distância: ${item.distanciaKm}", fontSize = 14.sp, color = Color.DarkGray)
                                    Text(text = "Tempo: ${item.duracao}", fontSize = 14.sp, color = Color.DarkGray)
                                }
                            }
                        }
                    }
                }
            }

            // Barra invisível inferior para o FAB não cobrir conteúdo
            Spacer(modifier = Modifier.height(80.dp))
        }

        // --- PAINEL DO MAPA E CONTROLES (SOBREPÕE TUDO) ---
        if (painelGpsVisivel) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.White)
            ) {
                // Mapa
                GoogleMap(
                    modifier = Modifier.fillMaxSize(),
                    cameraPositionState = cameraPositionState,
                    googleMapOptionsFactory = { GoogleMapOptions() },
                    properties = MapProperties(isMyLocationEnabled = temPermissaoGps),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false)
                ) {
                    if (rotaPontos.size >= 2) {
                        Polyline(
                            points = rotaPontos.toList(),
                            color = Color(0xFFFF5722),
                            width = 12f
                        )
                    }
                }

                // Card Inferior de Controles e Destino
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 80.dp, start = 16.dp, end = 16.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    elevation = CardDefaults.cardElevation(8.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (!emAndamento) {
                            Text("Para onde você vai?", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(8.dp))

                            OutlinedTextField(
                                value = destinoInput,
                                onValueChange = { destinoInput = it },
                                label = { Text("Digite o destino") },
                                modifier = Modifier.fillMaxWidth()
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceEvenly
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("VELOCIDADE", fontSize = 12.sp, color = Color.Gray)
                                    Text("${String.format("%.1f", velocidadeAtualKmh)} km/h", fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Text("DISTÂNCIA", fontSize = 12.sp, color = Color.Gray)
                                    Text(String.format("%.2f km", distanciaTotalMetros / 1000f), fontWeight = FontWeight.Bold, fontSize = 20.sp)
                                }
                            }
                            Spacer(modifier = Modifier.height(16.dp))
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Button(
                                onClick = { iniciarRastreio() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                enabled = !emAndamento,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF5722))
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = "Iniciar")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Iniciar")
                            }

                            Spacer(modifier = Modifier.width(16.dp))

                            Button(
                                onClick = { finalizarRastreio() },
                                modifier = Modifier.weight(1f).height(50.dp),
                                enabled = emAndamento,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFE2E8F0),
                                    contentColor = Color(0xFF475569)
                                )
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = "Finalizar")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Finalizar")
                            }
                        }
                    }
                }
            }
        }

        // --- BARRA INFERIOR (BOTTOM NAV) + FAB CENTRAL ---
        Box(
            modifier = Modifier.fillMaxWidth().align(Alignment.BottomCenter).height(70.dp).background(Color.White)
                .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp))
        ) {
            Row(
                modifier = Modifier.fillMaxSize().padding(horizontal = 24.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { /* Home */ }) {
                    Icon(Icons.Default.Home, contentDescription = "Home")
                }
                IconButton(onClick = { /* Search */ }) {
                    Icon(Icons.Default.Search, contentDescription = "Buscar")
                }

                Spacer(modifier = Modifier.width(56.dp)) // Espaço do FAB

                IconButton(onClick = { /* History */ }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Histórico")
                }
                IconButton(onClick = { /* Profile */ }) {
                    Icon(Icons.Default.Person, contentDescription = "Perfil")
                }
            }
        }

        // FAB FLUTUANTE (Alterna o modo mapa)
        FloatingActionButton(
            onClick = {
                painelGpsVisivel = !painelGpsVisivel
                if (!painelGpsVisivel && emAndamento) finalizarRastreio()
            },
            modifier = Modifier.align(Alignment.BottomCenter).offset(y = (-20).dp).size(64.dp),
            shape = CircleShape,
            containerColor = if (painelGpsVisivel) Color.Black else Color.Black,
            contentColor = Color.White
        ) {
            if (painelGpsVisivel) {
                Icon(Icons.Default.Close, contentDescription = "Fechar Mapa", modifier = Modifier.size(32.dp))
            } else {
                Icon(Icons.Default.Add, contentDescription = "Nova Jornada", modifier = Modifier.size(32.dp))
            }
        }
    }
}