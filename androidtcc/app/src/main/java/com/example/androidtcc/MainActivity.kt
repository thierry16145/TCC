package com.example.androidtcc

import android.content.SharedPreferences
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Route
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.androidtcc.data.ApiClient
import com.example.androidtcc.data.ApiService
import com.example.androidtcc.data.AuthResponse
import com.example.androidtcc.data.CadastroRequest
import com.example.androidtcc.data.LoginRequest
import com.example.androidtcc.data.PerfilResponse
import com.example.androidtcc.data.UsuarioDto
import com.example.androidtcc.ui.theme.AndroidTccTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

enum class EstadoAutenticacao {
    BOAS_VINDAS, LOGIN, CADASTRO
}

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val api = ApiClient.criar(applicationContext)
        val preferencias = getSharedPreferences("monitoramento_jornada", MODE_PRIVATE)

        setContent {
            AndroidTccTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.White
                ) {
                    TelaAutenticacao(
                        api = api,
                        preferencias = preferencias
                    )
                }
            }
        }
    }
}

@Composable
fun HeaderApp() {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 24.dp)
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF4358F6)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.Route,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(22.dp)
            )
        }
        Spacer(modifier = Modifier.width(10.dp))
        Text(
            text = "Monitoramento de Jornada",
            fontSize = 17.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1E293B)
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TelaAutenticacao(
    api: ApiService,
    preferencias: SharedPreferences
) {
    var estadoAtual by remember { mutableStateOf(EstadoAutenticacao.BOAS_VINDAS) }
    var nome by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var senha by remember { mutableStateOf("") }
    var confirmarSenha by remember { mutableStateOf("") }
    var mensagem by remember { mutableStateOf("") }
    var carregando by remember { mutableStateOf(false) }
    var usuario by remember { mutableStateOf<UsuarioDto?>(null) }

    val primaryBlue = Color(0xFF4358F6)
    val inputBorderColor = Color(0xFFE2E8F0)

    LaunchedEffect(Unit) {
        val token = preferencias.getString("token", null)
        if (!token.isNullOrBlank()) {
            api.perfil().enqueue(object : Callback<PerfilResponse> {
                override fun onResponse(call: Call<PerfilResponse>, response: Response<PerfilResponse>) {
                    if (response.isSuccessful) {
                        usuario = response.body()?.usuario
                    } else {
                        preferencias.edit().remove("token").apply()
                    }
                }

                override fun onFailure(call: Call<PerfilResponse>, t: Throwable) {
                    mensagem = "Não foi possível verificar a sessão. Confira se a API está conectada."
                }
            })
        }
    }

    val usuarioAtual = usuario

    if (usuarioAtual != null) {
        PainelJornada(
            api = api,
            preferencias = preferencias,
            usuario = usuarioAtual,
            onSair = {
                preferencias.edit().remove("token").apply()
                usuario = null
                senha = ""
                confirmarSenha = ""
                mensagem = ""
                estadoAtual = EstadoAutenticacao.LOGIN
            }
        )
    } else {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.White)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            HeaderApp()

            when (estadoAtual) {
                EstadoAutenticacao.BOAS_VINDAS -> {
                    Spacer(modifier = Modifier.weight(1f))

                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xFFEEF2FF)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Route,
                            contentDescription = null,
                            tint = primaryBlue,
                            modifier = Modifier.size(54.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    Text(
                        text = "Seu caminho,",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Normal,
                        color = Color(0xFF1E293B),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = "bem acompanhado.",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = primaryBlue,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Registre suas jornadas com\nsimplicidade e segurança.",
                        fontSize = 15.sp,
                        color = Color(0xFF64748B),
                        textAlign = TextAlign.Center,
                        lineHeight = 22.sp
                    )

                    Spacer(modifier = Modifier.height(32.dp))

                    Button(
                        onClick = { estadoAtual = EstadoAutenticacao.LOGIN },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        modifier = Modifier
                            .fillMaxWidth(0.55f)
                            .height(48.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Começar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.weight(1f))

                    Text(
                        text = "Protótipo visual · v1.0",
                        fontSize = 12.sp,
                        color = Color(0xFF94A3B8),
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                EstadoAutenticacao.LOGIN -> {
                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Bem-vindo de volta",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Acesse sua conta para continuar.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 28.dp)
                    )

                    Text("E-mail", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("voce@exemplo.com", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    Text("Senha", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        placeholder = { Text("Digite sua senha", color = Color(0xFF94A3B8)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    TextButton(
                        onClick = { /* Ação esqueci minha senha */ },
                        modifier = Modifier.align(Alignment.End).padding(top = 4.dp)
                    ) {
                        Text("Esqueci minha senha", color = primaryBlue, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Button(
                        onClick = {
                            if (!email.contains("@")) {
                                mensagem = "Informe um e-mail válido."
                            } else if (senha.length < 8) {
                                mensagem = "A senha precisa ter pelo menos 8 caracteres."
                            } else {
                                carregando = true
                                mensagem = "Conectando à API..."

                                api.login(LoginRequest(email = email.trim(), senha = senha)).enqueue(object : Callback<AuthResponse> {
                                    override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                        carregando = false
                                        val resultado = response.body()
                                        if (response.isSuccessful && resultado != null) {
                                            preferencias.edit().putString("token", resultado.token).apply()
                                            usuario = resultado.usuario
                                            mensagem = resultado.mensagem
                                        } else {
                                            val detalhe = response.errorBody()?.string()
                                            mensagem = "Não foi possível entrar. " + (detalhe ?: "Erro HTTP ${response.code()}")
                                        }
                                    }

                                    override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                                        carregando = false
                                        mensagem = "Erro ao conectar à API: ${t.message}"
                                    }
                                })
                            }
                        },
                        enabled = !carregando,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (carregando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Entrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("Ainda não tem uma conta? ")
                            withStyle(style = SpanStyle(color = primaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Criar conta")
                            }
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.clickable {
                            mensagem = ""
                            estadoAtual = EstadoAutenticacao.CADASTRO
                        }
                    )
                }

                EstadoAutenticacao.CADASTRO -> {
                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Crie sua conta",
                        fontSize = 26.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF0F172A),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        text = "Leva menos de dois minutos.",
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.fillMaxWidth().padding(top = 4.dp, bottom = 20.dp)
                    )

                    Text("Nome completo", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = nome,
                        onValueChange = { nome = it },
                        placeholder = { Text("Como podemos chamar você?", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("E-mail", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        placeholder = { Text("voce@exemplo.com", color = Color(0xFF94A3B8)) },
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Senha", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = senha,
                        onValueChange = { senha = it },
                        placeholder = { Text("Mínimo de 8 caracteres", color = Color(0xFF94A3B8)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Text("Confirmar senha", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = Color(0xFF334155), modifier = Modifier.fillMaxWidth())
                    Spacer(modifier = Modifier.height(4.dp))
                    OutlinedTextField(
                        value = confirmarSenha,
                        onValueChange = { confirmarSenha = it },
                        placeholder = { Text("Repita sua senha", color = Color(0xFF94A3B8)) },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(10.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = primaryBlue,
                            unfocusedBorderColor = inputBorderColor
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            when {
                                nome.trim().length < 2 -> mensagem = "Informe seu nome completo."
                                !email.contains("@") -> mensagem = "Informe um e-mail válido."
                                senha.length < 8 -> mensagem = "A senha deve ter pelo menos 8 caracteres."
                                senha != confirmarSenha -> mensagem = "As senhas não coincidem."
                                else -> {
                                    carregando = true
                                    mensagem = "Cadastrando..."

                                    api.cadastrar(CadastroRequest(nome = nome.trim(), email = email.trim(), senha = senha)).enqueue(object : Callback<AuthResponse> {
                                        override fun onResponse(call: Call<AuthResponse>, response: Response<AuthResponse>) {
                                            carregando = false
                                            val resultado = response.body()
                                            if (response.isSuccessful && resultado != null) {
                                                preferencias.edit().putString("token", resultado.token).apply()
                                                usuario = resultado.usuario
                                                mensagem = resultado.mensagem
                                            } else {
                                                val detalhe = response.errorBody()?.string()
                                                mensagem = "Não foi possível cadastrar. " + (detalhe ?: "Erro HTTP ${response.code()}")
                                            }
                                        }

                                        override fun onFailure(call: Call<AuthResponse>, t: Throwable) {
                                            carregando = false
                                            mensagem = "Erro de conexão: ${t.message}"
                                        }
                                    })
                                }
                            }
                        },
                        enabled = !carregando,
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = primaryBlue),
                        modifier = Modifier.fillMaxWidth().height(50.dp)
                    ) {
                        if (carregando) {
                            CircularProgressIndicator(color = Color.White, modifier = Modifier.size(22.dp))
                        } else {
                            Text("Cadastrar", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    Spacer(modifier = Modifier.height(20.dp))

                    Text(
                        text = buildAnnotatedString {
                            append("Já possui uma conta? ")
                            withStyle(style = SpanStyle(color = primaryBlue, fontWeight = FontWeight.Bold)) {
                                append("Entrar")
                            }
                        },
                        fontSize = 14.sp,
                        color = Color(0xFF64748B),
                        modifier = Modifier.clickable {
                            mensagem = ""
                            estadoAtual = EstadoAutenticacao.LOGIN
                        }
                    )
                }
            }

            if (mensagem.isNotBlank()) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = mensagem,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}