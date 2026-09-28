package com.lalit.amplify.feature.downloader

import com.lalit.amplify.feature.downloader.engine.NetworkClients
import okhttp3.Interceptor
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class NetworkClientsTest {

    @Test
    fun retryInterceptor_doesNotRetry401Unauthorized() {
        val attempts = AtomicInteger(0)
        val interceptor = NetworkClients.RetryInterceptor(maxRetries = 3, initialBackoffMs = 10)

        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.com/test").build()

            override fun proceed(request: Request): Response {
                attempts.incrementAndGet()
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(401)
                    .message("Unauthorized")
                    .body("".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 1000
            override fun readTimeoutMillis() = 1000
            override fun writeTimeoutMillis() = 1000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        val response = interceptor.intercept(chain)
        assertEquals(401, response.code)
        // Should only execute 1 attempt (no retries for 401)
        assertEquals(1, attempts.get())
    }

    @Test
    fun retryInterceptor_retriesTransient502Error() {
        val attempts = AtomicInteger(0)
        val interceptor = NetworkClients.RetryInterceptor(maxRetries = 2, initialBackoffMs = 10)

        val chain = object : Interceptor.Chain {
            override fun request(): Request = Request.Builder().url("https://example.com/test").build()

            override fun proceed(request: Request): Response {
                val current = attempts.incrementAndGet()
                val code = if (current < 3) 502 else 200
                return Response.Builder()
                    .request(request)
                    .protocol(Protocol.HTTP_1_1)
                    .code(code)
                    .message(if (code == 200) "OK" else "Bad Gateway")
                    .body("".toResponseBody("application/json".toMediaType()))
                    .build()
            }

            override fun connection() = null
            override fun call() = throw UnsupportedOperationException()
            override fun connectTimeoutMillis() = 1000
            override fun readTimeoutMillis() = 1000
            override fun writeTimeoutMillis() = 1000
            override fun withConnectTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withReadTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
            override fun withWriteTimeout(timeout: Int, unit: java.util.concurrent.TimeUnit) = this
        }

        val response = interceptor.intercept(chain)
        assertEquals(200, response.code)
        assertEquals(3, attempts.get())
    }
}
