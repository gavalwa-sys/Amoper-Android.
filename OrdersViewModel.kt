package africa.amoper.app.ui.orders

import africa.amoper.app.data.model.Order
import africa.amoper.app.data.repository.MarketplaceRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class OrdersViewModel(private val repository: MarketplaceRepository) : ViewModel() {

    var orders by mutableStateOf<List<Order>>(emptyList())
        private set
    var isLoading by mutableStateOf(false)
        private set
    var selectedOrder by mutableStateOf<Order?>(null)
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    fun loadOrders() {
        isLoading = true
        viewModelScope.launch {
            repository.orders().onSuccess { orders = it.items }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    fun loadOrderDetail(id: Long) {
        isLoading = true
        selectedOrder = null
        viewModelScope.launch {
            repository.orderDetail(id).onSuccess { selectedOrder = it }.onFailure { errorMessage = it.message }
            isLoading = false
        }
    }

    companion object {
        fun factory(repository: MarketplaceRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = OrdersViewModel(repository) as T
        }
    }
}
