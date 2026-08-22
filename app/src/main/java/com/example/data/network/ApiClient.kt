package com.example.data.network

import android.content.Context
import com.example.BuildConfig
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonReader
import com.squareup.moshi.JsonWriter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit

class UnitJsonAdapter : JsonAdapter<Unit>() {
    override fun fromJson(reader: JsonReader): Unit {
        reader.skipValue()
        return Unit
    }

    override fun toJson(writer: JsonWriter, value: Unit?) {
        writer.nullValue()
    }
}

object ApiClient {

    // Default base URL dynamically resolved based on build type (HTTPS in Release, Emulator loopback in Debug)
    val DEFAULT_BASE_URL: String = BuildConfig.BASE_URL

    @Volatile
    private var apiService: ApiService? = null

    fun getService(context: Context, customBaseUrl: String? = null): ApiService {
        return apiService ?: synchronized(this) {
            apiService ?: buildRetrofit(context, customBaseUrl ?: DEFAULT_BASE_URL).create(ApiService::class.java).also {
                apiService = it
            }
        }
    }

    private fun buildRetrofit(context: Context, baseUrl: String): Retrofit {
        val tokenManager = TokenManager.getInstance(context)

        val loggingInterceptor = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) {
                HttpLoggingInterceptor.Level.BODY
            } else {
                HttpLoggingInterceptor.Level.NONE
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenManager))
            .addInterceptor(loggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()

        val moshi = Moshi.Builder()
            .add(Unit::class.java, UnitJsonAdapter())
            .addLast(KotlinJsonAdapterFactory())
            .build()

        val cleanUrl = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"

        return Retrofit.Builder()
            .baseUrl(cleanUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
    }
}
