package africa.amoper.app.data.network

import africa.amoper.app.BuildConfig
import africa.amoper.app.data.model.ApiEnvelope
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

class ApiException(message: String) : Exception(message)

/** Attaches the saved bearer token (if any) to every outgoing request. */
private class AuthInterceptor(private val tokenStore: TokenStore) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val token = runBlocking { tokenStore.currentToken() }
        val request = if (token != null) {
            chain.request().newBuilder().addHeader("Authorization", "Bearer $token").build()
        } else {
            chain.request()
        }
        return chain.proceed(request)
    }
}

object NetworkModule {

    fun buildApi(tokenStore: TokenStore): ApiService {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BODY else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BuildConfig.API_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        return retrofit.create(ApiService::class.java)
    }
}

/** Unwraps an ApiEnvelope, throwing ApiException with the server's message on failure. */
fun <T> ApiEnvelope<T>.unwrap(): T {
    if (!ok || data == null) throw ApiException(error ?: "Something went wrong. Please try again.")
    return data
}

/** Runs a suspend API call and turns network/HTTP failures into a friendly ApiException too. */
suspend fun <T> safeApiCall(block: suspend () -> ApiEnvelope<T>): Result<T> {
    return try {
        Result.success(block().unwrap())
    } catch (e: ApiException) {
        Result.failure(e)
    } catch (e: Exception) {
        Result.failure(ApiException(e.message ?: "Network error. Check your connection."))
    }
}
