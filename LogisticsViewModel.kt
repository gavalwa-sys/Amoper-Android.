package africa.amoper.app.ui.logistics

import africa.amoper.app.data.model.Quote
import africa.amoper.app.data.model.Shipment
import africa.amoper.app.data.repository.LogisticsRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class LogisticsViewModel(private val repository: LogisticsRepository) : ViewModel() {

    var shipments by mutableStateOf<List<Shipment>>(emptyList())
        private set
    var selectedShipment by mutableStateOf<Shipment?>(null)
        private set
    var latestQuote by mutableStateOf<Quote?>(null)
        private set
    var isLoading by mutableStateOf(false)
        private set
    var isSubmittingQuote by mutableStateOf(false)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadShipments() {
        isLoading = true
        viewModelScope.launch {
            repository.shipments().onSuccess { shipments = it.items }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun loadShipmentDetail(id: Long) {
        isLoading = true
        selectedShipment = null
        viewModelScope.launch {
            repository.shipmentDetail(id).onSuccess { selectedShipment = it }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun trackByNumber(trackingNumber: String) {
        isLoading = true
        selectedShipment = null
        errorMessage = null
        viewModelScope.launch {
            repository.trackShipment(trackingNumber.trim()).onSuccess { selectedShipment = it }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun requestQuote(origin: String, destination: String, speed: String, weightKg: Double, packageType: String, description: String?) {
        isSubmittingQuote = true
        errorMessage = null
        latestQuote = null
        viewModelScope.launch {
            repository.requestQuote(origin, destination, speed, weightKg, packageType, description)
                .onSuccess { latestQuote = it }
                .onFailure { errorMessage = it.message }
            isSubmittingQuote = false
        }
    }

    fun acceptQuote(quoteId: Long, onBooked: (Long) -> Unit) {
        viewModelScope.launch {
            repository.acceptQuote(quoteId).onSuccess { onBooked(it.id) }.onFailure { errorMessage = it.message }
        }
    }

    fun clearQuote() { latestQuote = null }
    fun clearError() { errorMessage = null }

    companion object {
        fun factory(repository: LogisticsRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = LogisticsViewModel(repository) as T
        }
    }
}
