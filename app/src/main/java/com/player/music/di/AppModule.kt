package com.player.music.di

import android.content.Context
import android.util.Log
import com.player.music.config.Config
import com.player.music.local.DataStoreManager
import com.player.music.network.ApiService
import com.player.music.network.NetworkInterceptor
import com.player.music.repository.CircleRepository
import com.player.music.repository.ChatRepository
import com.player.music.repository.MusicRepository
import com.player.music.repository.UserRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Singleton

/**
 * Hilt 依赖注入模块：网络、数据存储、仓库
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(networkInterceptor: NetworkInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(DetailedLoggingInterceptor())
            .addInterceptor(networkInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideRetrofit(okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .baseUrl(Config.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
    }

    @Provides
    @Singleton
    fun provideApiService(retrofit: Retrofit): ApiService {
        return retrofit.create(ApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDataStoreManager(@ApplicationContext context: Context): DataStoreManager {
        return DataStoreManager(context)
    }

    @Provides
    @Singleton
    fun provideNetworkInterceptor(dataStoreManager: DataStoreManager): NetworkInterceptor {
        return NetworkInterceptor(dataStoreManager)
    }

    @Provides
    @Singleton
    fun provideMusicRepository(apiService: ApiService): MusicRepository {
        return MusicRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideCircleRepository(apiService: ApiService): CircleRepository {
        return CircleRepository(apiService)
    }

    @Provides
    @Singleton
    fun provideChatRepository(apiService: ApiService, okHttpClient: OkHttpClient): ChatRepository {
        return ChatRepository(apiService, okHttpClient)
    }

    @Provides
    @Singleton
    fun provideUserRepository(
        apiService: ApiService,
        dataStoreManager: DataStoreManager
    ): UserRepository {
        return UserRepository(apiService, dataStoreManager)
    }
}

/**
 * 网络请求/响应日志拦截器，便于调试接口
 */
class DetailedLoggingInterceptor : Interceptor {
    companion object {
        private const val TAG = "NetworkLog"
    }

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        logRequest(request)

        val startTime = System.nanoTime()
        val response = chain.proceed(request)
        val duration = (System.nanoTime() - startTime) / 1_000_000.0
        logResponse(response, duration)
        return response
    }

    private fun logRequest(request: Request) {
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "📤 发起请求")
        Log.d(TAG, "URL: ${request.url}")
        Log.d(TAG, "Method: ${request.method}")
        val requestBody = request.body
        if (requestBody != null) {
            try {
                val buffer = okio.Buffer()
                requestBody.writeTo(buffer)
                Log.d(TAG, "Body: ${buffer.readUtf8()}")
            } catch (e: Exception) {
                Log.d(TAG, "Body: [无法读取请求体]")
            }
        }
        Log.d(TAG, "═══════════════════════════════════════════════════")
    }

    private fun logResponse(response: Response, duration: Double) {
        Log.d(TAG, "═══════════════════════════════════════════════════")
        Log.d(TAG, "📥 收到响应")
        Log.d(TAG, "URL: ${response.request.url}")
        Log.d(TAG, "状态码: ${response.code} ${response.message}")
        Log.d(TAG, "耗时: ${duration}ms")
        try {
            val source = response.body?.source()
            source?.request(Long.MAX_VALUE)
            val content = source?.buffer?.clone()?.readUtf8()
            Log.d(TAG, "Body: ${if (content.isNullOrBlank()) "[空]" else content}")
        } catch (e: Exception) {
            Log.d(TAG, "Body: [无法读取响应体]")
        }
        Log.d(TAG, "═══════════════════════════════════════════════════")
    }
}
