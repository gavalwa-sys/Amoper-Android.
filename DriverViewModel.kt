package africa.amoper.app.ui.driver

import africa.amoper.app.data.model.DriverDelivery
import africa.amoper.app.data.repository.DriverRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class DriverViewModel(private val repository: DriverRepository) : ViewModel() {

    var deliveries by mutableStateOf<List<DriverDelivery>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set
    var isSharingLocation by mutableStateOf(false)

    fun loadDeliveries() {
        isLoading = true
        viewModelScope.launch {
            repository.deliveries().onSuccess { deliveries = it }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun updateStatus(shipmentId: Long, status: String) {
        viewModelScope.launch {
            repository.updateStatus(shipmentId, status, note = null, location = null)
                .onSuccess { loadDeliveries() }
                .onFailure { errorMessage = it.message }
        }
    }

    fun pingLocation(shipmentId: Long?, lat: Double, lng: Double) {
        viewModelScope.launch {
            repository.pingLocation(shipmentId, lat, lng)
        }
    }

    companion object {
        fun factory(repository: DriverRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = DriverViewModel(repository) as T
        }
    }
}
