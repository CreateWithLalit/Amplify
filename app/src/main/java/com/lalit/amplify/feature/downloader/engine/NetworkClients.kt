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
    private const val TAG = "AmplifyNetwork"

    private val logging by lazy {
        HttpLoggingInterceptor { message ->
            if (!message.contains("Authorization:", ignoreCase = true) &&
                !message.contains("Cookie:", ignoreCase = true)) {
                Log.d(TAG, message)
            }
        }.apply { level = HttpLoggingInterceptor.Level.BASIC }
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

    class RetryInterceptor(
        private val maxRetries: Int = 3,
        private val initialBackoffMs: Long = 500
    ) : Interceptor {
        override fun intercept(chain: Interceptor.Chain): Response {
            var attempt = 0
            var lastException: Exception? = null
            while (true) {
                try {
                    val request = chain.request()
                    val response = chain.proceed(request)
                    
                    // Do not retry permanent client authorization/access errors (401, 403, 404)
                    if (response.code in listOf(401, 403, 404)) {
                        Log.w(TAG, "Request failed with non-retryable HTTP ${response.code}")
                        return response
                    }

                    // Retry transient server errors (502, 503, 504) or rate limiting (429)
                    val isTransientError = response.code in listOf(429, 502, 503, 504)
                    if (!response.isSuccessful && isTransientError && attempt < maxRetries) {
                        response.close()
                        val backoff = initialBackoffMs * (1L shl attempt)
                        Log.w(TAG, "Transient HTTP ${response.code}, retrying attempt ${attempt + 1}/$maxRetries in ${backoff}ms")
                        try {
                            Thread.sleep(backoff)
                        } catch (_: InterruptedException) {
                            Thread.currentThread().interrupt()
                            throw IOException("Retry interrupted")
                        }
                        attempt++
                        continue
                    }
                    return response
                } catch (e: Exception) {
                    lastException = e
                    if (attempt >= maxRetries) break
                    val backoff = initialBackoffMs * (1L shl attempt)
                    Log.w(TAG, "Network exception on attempt ${attempt + 1}/$maxRetries: ${e.message}, retrying in ${backoff}ms")
                    try {
                        Thread.sleep(backoff)
                    } catch (_: InterruptedException) {
                        Thread.currentThread().interrupt()
                        throw IOException("Retry interrupted")
                    }
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
