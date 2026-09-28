package com.lalit.amplify.feature.downloader.engine

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import okhttp3.Dns
import okhttp3.Interceptor
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit

object NetworkClients {
    private const val TAG = "NetworkClients"

    private val logging by lazy {
        HttpLoggingInterceptor().apply { level = HttpLoggingInterceptor.Level.BASIC }
    }

    private class StandardHeadersInterceptor : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            val original = chain.request()
            val requestBuilder = original.newBuilder()

            if (original.header("User-Agent") == null) {
                requestBuilder.header("User-Agent", "Amplify-MusicPlayer/1.0 (Android; Mobile)")
            }
            if (original.header("Accept") == null) {
                requestBuilder.header("Accept", "application/json, audio/*, */*")
            }
            if (original.header("Accept-Language") == null) {
                requestBuilder.header("Accept-Language", "en-US,en;q=0.9")
            }

            return chain.proceed(requestBuilder.build())
        }
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
                    // Retry only transient server errors or rate limiting
                    val isTransientError = !response.isSuccessful && (response.code in listOf(429, 502, 503, 504))
                    if (isTransientError && attempt < maxRetries) {
                        Log.w(TAG, "Transient HTTP ${response.code} received. Retrying attempt ${attempt + 1}/$maxRetries...")
                        response.close()
                        Thread.sleep(initialBackoffMs * (1L shl attempt))
                        attempt++
                        continue
                    }
                    return response
                } catch (e: Exception) {
                    lastException = e
                    if (attempt >= maxRetries) {
                        Log.e(TAG, "Network request failed after $maxRetries retries: ${e.message}")
                        break
                    }
                    Log.w(TAG, "Network exception on attempt ${attempt + 1}/$maxRetries: ${e.javaClass.simpleName}. Backing off...")
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
            .addInterceptor(StandardHeadersInterceptor())
            .addInterceptor(logging)
            .addInterceptor(RetryInterceptor())
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(90, TimeUnit.SECONDS)
            .build()
    }
}
