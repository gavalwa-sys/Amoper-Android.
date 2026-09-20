package africa.amoper.app.ui.auth

import africa.amoper.app.data.model.User
import africa.amoper.app.data.repository.AuthRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

sealed interface SessionState {
    data object Loading : SessionState
    data object LoggedOut : SessionState
    data class LoggedIn(val user: User) : SessionState
}

class AuthViewModel(private val repository: AuthRepository) : ViewModel() {

    var session by mutableStateOf<SessionState>(SessionState.Loading)
        private set

    var isSubmitting by mutableStateOf(false)
        private set

    var errorMessage by mutableStateOf<String?>(null)
        private set

    init {
        refreshSession()
    }

    fun refreshSession() {
        viewModelScope.launch {
            if (!repository.isLoggedIn()) {
                session = SessionState.LoggedOut
                return@launch
            }
            repository.me().onSuccess { session = SessionState.LoggedIn(it) }
                .onFailure { session = SessionState.LoggedOut }
        }
    }

    fun login(email: String, password: String) {
        errorMessage = null
        isSubmitting = true
        viewModelScope.launch {
            repository.login(email, password)
                .onSuccess { session = SessionState.LoggedIn(it.user) }
                .onFailure { errorMessage = it.message }
            isSubmitting = false
        }
    }

    fun register(name: String, email: String, phone: String, password: String, role: String) {
        errorMessage = null
        isSubmitting = true
        viewModelScope.launch {
            repository.register(name, email, phone.ifBlank { null }, password, role)
                .onSuccess { session = SessionState.LoggedIn(it.user) }
                .onFailure { errorMessage = it.message }
            isSubmitting = false
        }
    }

    fun logout() {
        viewModelScope.launch {
            repository.logout()
            session = SessionState.LoggedOut
        }
    }

    fun clearError() { errorMessage = null }

    companion object {
        fun factory(repository: AuthRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = AuthViewModel(repository) as T
        }
    }
}
