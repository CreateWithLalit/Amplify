package com.lalit.amplify.feature.downloader.engine

import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClients {
    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    private class RetryInterceptor(
        private val maxRetries: Int = 3,
        private val initialBackoffMs: Long = 500
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var attempt = 0
            var lastException: Exception? = null
            while (true) {
                try {
                    val response = chain.proceed(chain.request())
                    if (!response.isSuccessful && response.code in listOf(502, 503, 504) && attempt < maxRetries) {
                        response.close()
                        Thread.sleep(initialBackoffMs * (1L shl attempt))
                        attempt++
                        continue
                    }
                    return response
                } catch (e: Exception) {
                    lastException = e
                    if (attempt >= maxRetries) break
                    Thread.sleep(initialBackoffMs * (1L shl attempt))
                    attempt++
                }
            }
            throw lastException ?: IOException("Network request failed after retries")
        }
    }

    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .dns(Dns.SYSTEM)
            .addInterceptor(logging)
            .addInterceptor(RetryInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
