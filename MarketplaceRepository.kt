package africa.amoper.app.data.repository

import africa.amoper.app.data.model.*
import africa.amoper.app.data.network.ApiService
import africa.amoper.app.data.network.safeApiCall

class MarketplaceRepository(private val api: ApiService) {

    suspend fun categories(): Result<List<Category>> = safeApiCall { api.categories() }

    suspend fun products(category: Long? = null, search: String? = null, page: Int = 1): Result<ProductPage> =
        safeApiCall { api.products(category = category, search = search, page = page) }

    suspend fun productDetail(id: Long): Result<ProductDetail> =
        safeApiCall { api.productDetail(route = "products/$id") }

    suspend fun cart(): Result<Cart> = safeApiCall { api.cart() }

    suspend fun addToCart(productId: Long, quantity: Int = 1): Result<Cart> =
        safeApiCall { api.addToCart(body = mapOf("product_id" to productId, "quantity" to quantity.toLong())) }

    suspend fun updateCartItem(productId: Long, quantity: Int): Result<Cart> =
        safeApiCall { api.updateCartItem(route = "cart/items/$productId", body = mapOf("quantity" to quantity)) }

    suspend fun removeCartItem(productId: Long): Result<Cart> =
        safeApiCall { api.removeCartItem(route = "cart/items/$productId") }

    suspend fun placeOrder(address: DeliveryAddress, deliveryFee: Double, notes: String?): Result<Order> {
        val body: Map<String, Any?> = mapOf(
            "address" to mapOf(
                "full_name" to address.fullName, "phone" to address.phone,
                "area" to address.area, "street" to address.street
            ),
            "delivery_fee" to deliveryFee,
            "notes" to notes
        )
        return safeApiCall { api.placeOrder(body = body) }
    }

    suspend fun orders(): Result<OrderPage> = safeApiCall { api.orders() }

    suspend fun orderDetail(id: Long): Result<Order> = safeApiCall { api.orderDetail(route = "orders/$id") }
}
