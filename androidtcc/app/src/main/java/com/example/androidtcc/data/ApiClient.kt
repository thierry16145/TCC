package com.example.androidtcc.data

import android.content.Context
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL =
        "http://10.0.2.2:3000/"

    fun criar(context: Context): ApiService {

        val preferencias = context.getSharedPreferences(
            "monitoramento_jornada",
            Context.MODE_PRIVATE
        )

        val cliente = OkHttpClient.Builder()
            .addInterceptor { chain ->

                val requisicaoOriginal = chain.request()
                val construtor = requisicaoOriginal.newBuilder()

                val token = preferencias.getString(
                    "token",
                    null
                )

                if (
                    !token.isNullOrBlank() &&
                    requisicaoOriginal.header("Authorization") == null
                ) {
                    construtor.header(
                        "Authorization",
                        "Bearer $token"
                    )
                }

                chain.proceed(construtor.build())
            }
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(cliente)
            .addConverterFactory(
                GsonConverterFactory.create()
            )
            .build()
            .create(ApiService::class.java)
    }
}