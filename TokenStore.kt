package africa.amoper.app.data.network

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "amoper_session")

/** Persists the bearer token across app restarts. */
class TokenStore(private val context: Context) {

    private val tokenKey = stringPreferencesKey("api_token")

    val tokenFlow: Flow<String?> = context.dataStore.data.map { it[tokenKey] }

    suspend fun currentToken(): String? = context.dataStore.data.first()[tokenKey]

    suspend fun save(token: String) {
        context.dataStore.edit { it[tokenKey] = token }
    }

    suspend fun clear() {
        context.dataStore.edit { it.remove(tokenKey) }
    }
}
