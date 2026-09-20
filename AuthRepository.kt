package africa.amoper.app.data.repository

import africa.amoper.app.data.model.AuthSession
import africa.amoper.app.data.model.User
import africa.amoper.app.data.network.ApiService
import africa.amoper.app.data.network.TokenStore
import africa.amoper.app.data.network.safeApiCall

class AuthRepository(
    private val api: ApiService,
    private val tokenStore: TokenStore
) {
    val tokenFlow = tokenStore.tokenFlow

    suspend fun register(name: String, email: String, phone: String?, password: String, role: String): Result<AuthSession> {
        val body = mapOf(
            "name" to name, "email" to email, "phone" to phone,
            "password" to password, "role" to role
        )
        return safeApiCall { api.register(body = body) }.onSuccess { tokenStore.save(it.token) }
    }

    suspend fun login(email: String, password: String): Result<AuthSession> {
        val body = mapOf("email" to email, "password" to password)
        return safeApiCall { api.login(body = body) }.onSuccess { tokenStore.save(it.token) }
    }

    suspend fun logout() {
        runCatching { api.logout() }
        tokenStore.clear()
    }

    suspend fun me(): Result<User> = safeApiCall { api.me() }

    suspend fun isLoggedIn(): Boolean = tokenStore.currentToken() != null
}
