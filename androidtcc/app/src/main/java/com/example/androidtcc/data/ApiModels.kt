package com.example.androidtcc.data

data class CadastroRequest(
    val nome: String,
    val email: String,
    val senha: String
)

data class LoginRequest(
    val email: String,
    val senha: String
)

data class UsuarioDto(
    val id: Long,
    val nome: String,
    val email: String,
    val perfil: String
)

data class AuthResponse(
    val mensagem: String,
    val token: String,
    val usuario: UsuarioDto
)

data class PerfilResponse(
    val usuario: UsuarioDto
)