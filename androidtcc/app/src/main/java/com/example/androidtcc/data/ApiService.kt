package com.example.androidtcc.data

import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path

interface ApiService {

    @POST("api/auth/register")
    fun cadastrar(
        @Body dados: CadastroRequest
    ): Call<AuthResponse>

    @POST("api/auth/login")
    fun login(
        @Body dados: LoginRequest
    ): Call<AuthResponse>

    @GET("api/auth/me")
    fun perfil(): Call<PerfilResponse>

    @POST("api/jornadas")
    fun iniciarJornada(
        @Body dados: Map<String, String>
    ): Call<ResponseBody>

    @GET("api/jornadas")
    fun listarJornadas(): Call<ResponseBody>

    @GET("api/jornadas/{id}")
    fun obterJornada(
        @Path("id") id: Long
    ): Call<ResponseBody>

    @PATCH("api/jornadas/{id}/finalizar")
    fun finalizarJornada(
        @Path("id") id: Long
    ): Call<ResponseBody>
}