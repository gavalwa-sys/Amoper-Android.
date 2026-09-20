package africa.amoper.app.ui.marketplace

import africa.amoper.app.data.model.*
import africa.amoper.app.data.repository.MarketplaceRepository
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch

class MarketplaceViewModel(private val repository: MarketplaceRepository) : ViewModel() {

    var categories by mutableStateOf<List<Category>>(emptyList())
        private set
    var products by mutableStateOf<List<ProductSummary>>(emptyList())
        private set
    var isLoadingProducts by mutableStateOf(false)
        private set
    var selectedCategory by mutableStateOf<Long?>(null)
        private set
    var searchQuery by mutableStateOf("")
        private set
    var errorMessage by mutableStateOf<String?>(null)
        private set

    var productDetail by mutableStateOf<ProductDetail?>(null)
        private set
    var isLoadingDetail by mutableStateOf(false)
        private set

    var cart by mutableStateOf<Cart?>(null)
        private set
    var isLoadingCart by mutableStateOf(false)
        private set

    var checkoutSuccess by mutableStateOf<Order?>(null)
        private set
    var isCheckingOut by mutableStateOf(false)
        private set

    init {
        loadCategories()
        loadProducts()
    }

    fun loadCategories() {
        viewModelScope.launch {
            repository.categories().onSuccess { categories = it }
        }
    }

    fun loadProducts() {
        isLoadingProducts = true
        errorMessage = null
        viewModelScope.launch {
            repository.products(category = selectedCategory, search = searchQuery.ifBlank { null })
                .onSuccess { products = it.items }
                .onFailure { errorMessage = it.message }
            isLoadingProducts = false
        }
    }

    fun selectCategory(id: Long?) {
        selectedCategory = if (selectedCategory == id) null else id
        loadProducts()
    }

    fun updateSearch(query: String) {
        searchQuery = query
    }

    fun submitSearch() {
        loadProducts()
    }

    fun loadProductDetail(id: Long) {
        isLoadingDetail = true
        productDetail = null
        viewModelScope.launch {
            repository.productDetail(id).onSuccess { productDetail = it }.onFailure { errorMessage = it.message }
            isLoadingDetail = false
        }
    }

    fun loadCart() {
        isLoadingCart = true
        viewModelScope.launch {
            repository.cart().onSuccess { cart = it }.onFailure { errorMessage = it.message }
            isLoadingCart = false
        }
    }

    fun addToCart(productId: Long, quantity: Int = 1) {
        viewModelScope.launch {
            repository.addToCart(productId, quantity).onSuccess { cart = it }.onFailure { errorMessage = it.message }
        }
    }

    fun updateCartQuantity(productId: Long, quantity: Int) {
        viewModelScope.launch {
            repository.updateCartItem(productId, quantity).onSuccess { cart = it }.onFailure { errorMessage = it.message }
        }
    }

    fun removeFromCart(productId: Long) {
        viewModelScope.launch {
            repository.removeCartItem(productId).onSuccess { cart = it }.onFailure { errorMessage = it.message }
        }
    }

    fun placeOrder(fullName: String, phone: String, area: String, street: String, deliveryFee: Double) {
        isCheckingOut = true
        errorMessage = null
        viewModelScope.launch {
            repository.placeOrder(DeliveryAddress(fullName, phone, area, street), deliveryFee, null)
                .onSuccess { checkoutSuccess = it; cart = Cart(emptyList(), 0.0) }
                .onFailure { errorMessage = it.message }
            isCheckingOut = false
        }
    }

    fun resetCheckoutSuccess() { checkoutSuccess = null }
    fun clearError() { errorMessage = null }

    companion object {
        fun factory(repository: MarketplaceRepository) = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T = MarketplaceViewModel(repository) as T
        }
    }
}
