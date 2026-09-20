package africa.amoper.app.data.network

import africa.amoper.app.data.model.*
import retrofit2.http.*

/**
 * Every path here is relative to the API_BASE_URL and prefixed with "api.php"
 * because that entrypoint always exists (no .htaccess rewrite required).
 * Route selection happens via the "r" query parameter, matching api/index.php.
 */
interface ApiService {

    // ---- Auth ----
    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun register(@Query("r") route: String = "auth/register", @Body body: Map<String, String?>): ApiEnvelope<AuthSession>

    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun login(@Query("r") route: String = "auth/login", @Body body: Map<String, String>): ApiEnvelope<AuthSession>

    @POST("api.php")
    suspend fun logout(@Query("r") route: String = "auth/logout"): ApiEnvelope<SimpleMessage>

    @GET("api.php")
    suspend fun me(@Query("r") route: String = "auth/me"): ApiEnvelope<User>

    // ---- Marketplace ----
    @GET("api.php")
    suspend fun categories(@Query("r") route: String = "categories"): ApiEnvelope<List<Category>>

    @GET("api.php")
    suspend fun products(
        @Query("r") route: String = "products",
        @Query("category") category: Long? = null,
        @Query("search") search: String? = null,
        @Query("page") page: Int = 1
    ): ApiEnvelope<ProductPage>

    @GET("api.php")
    suspend fun productDetail(@Query("r") route: String, ): ApiEnvelope<ProductDetail>

    @GET("api.php")
    suspend fun cart(@Query("r") route: String = "cart"): ApiEnvelope<Cart>

    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun addToCart(
        @Query("r") route: String = "cart/items",
        @Body body: Map<String, Long>
    ): ApiEnvelope<Cart>

    @PUT("api.php")
    @Headers("Content-Type: application/json")
    suspend fun updateCartItem(
        @Query("r") route: String,
        @Body body: Map<String, Int>
    ): ApiEnvelope<Cart>

    @DELETE("api.php")
    suspend fun removeCartItem(@Query("r") route: String): ApiEnvelope<Cart>

    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun placeOrder(
        @Query("r") route: String = "orders",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiEnvelope<Order>

    @GET("api.php")
    suspend fun orders(@Query("r") route: String = "orders"): ApiEnvelope<OrderPage>

    @GET("api.php")
    suspend fun orderDetail(@Query("r") route: String): ApiEnvelope<Order>

    // ---- Logistics ----
    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun requestQuote(
        @Query("r") route: String = "logistics/quotes",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiEnvelope<Quote>

    @GET("api.php")
    suspend fun quotes(@Query("r") route: String = "logistics/quotes"): ApiEnvelope<List<Quote>>

    @POST("api.php")
    suspend fun acceptQuote(@Query("r") route: String): ApiEnvelope<Shipment>

    @GET("api.php")
    suspend fun shipments(@Query("r") route: String = "logistics/shipments"): ApiEnvelope<ShipmentPage>

    @GET("api.php")
    suspend fun shipmentDetail(@Query("r") route: String): ApiEnvelope<Shipment>

    @GET("api.php")
    suspend fun trackShipment(@Query("r") route: String): ApiEnvelope<Shipment>

    // ---- Driver ----
    @GET("api.php")
    suspend fun driverDeliveries(@Query("r") route: String = "driver/deliveries"): ApiEnvelope<List<DriverDelivery>>

    @PUT("api.php")
    @Headers("Content-Type: application/json")
    suspend fun updateDeliveryStatus(
        @Query("r") route: String,
        @Body body: Map<String, String?>
    ): ApiEnvelope<Map<String, String>>

    @POST("api.php")
    @Headers("Content-Type: application/json")
    suspend fun pingLocation(
        @Query("r") route: String = "driver/location",
        @Body body: Map<String, @JvmSuppressWildcards Any?>
    ): ApiEnvelope<SimpleMessage>

    // ---- Notifications ----
    @GET("api.php")
    suspend fun notifications(@Query("r") route: String = "notifications"): ApiEnvelope<List<AppNotification>>

    @POST("api.php")
    suspend fun markNotificationRead(@Query("r") route: String): ApiEnvelope<SimpleMessage>
}
