package africa.amoper.app.data.model

import com.google.gson.annotations.SerializedName

/** Envelope every AMOPER API response is wrapped in. */
data class ApiEnvelope<T>(
    val ok: Boolean,
    val data: T? = null,
    val error: String? = null
)

// ---------- Auth ----------

data class User(
    val id: Long,
    val name: String,
    val email: String,
    val phone: String?,
    val status: String?,
    val roles: List<String> = emptyList(),
    @SerializedName("driver_profile") val driverProfile: DriverProfile? = null
) {
    val isDriver: Boolean get() = roles.contains("driver")
}

data class DriverProfile(
    val id: Long,
    @SerializedName("verification_status") val verificationStatus: String?,
    val rating: Double?
)

data class AuthSession(
    val token: String,
    @SerializedName("expires_at") val expiresAt: String?,
    val user: User
)

// ---------- Marketplace ----------

data class Category(
    val id: Long,
    @SerializedName("parent_id") val parentId: Long?,
    val name: String,
    val slug: String?
)

data class ProductSummary(
    val id: Long,
    val name: String,
    val slug: String?,
    val brand: String?,
    val price: String,
    @SerializedName("sale_price") val salePrice: String?,
    @SerializedName("category_id") val categoryId: Long?,
    @SerializedName("image_url") val imageUrl: String?,
    val rating: String?
) {
    val displayPrice: String get() = salePrice ?: price
}

data class ProductPage(
    val items: List<ProductSummary>,
    val page: Int,
    @SerializedName("per_page") val perPage: Int,
    val total: Int
)

data class ProductSpec(
    @SerializedName("spec_key") val key: String,
    @SerializedName("spec_value") val value: String
)

data class ProductReview(
    val id: Long,
    val rating: Int,
    val body: String?,
    val reviewer: String?
)

data class ProductDetail(
    val id: Long,
    val name: String,
    val brand: String?,
    val description: String?,
    val price: String,
    @SerializedName("sale_price") val salePrice: String?,
    @SerializedName("seller_name") val sellerName: String?,
    val images: List<String> = emptyList(),
    val specifications: List<ProductSpec> = emptyList(),
    val reviews: List<ProductReview> = emptyList()
) {
    val displayPrice: String get() = salePrice ?: price
}

data class CartItem(
    @SerializedName("product_id") val productId: Long,
    val name: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: Double,
    @SerializedName("line_total") val lineTotal: Double,
    @SerializedName("image_url") val imageUrl: String?
)

data class Cart(
    val items: List<CartItem>,
    val subtotal: Double
)

data class DeliveryAddress(
    @SerializedName("full_name") val fullName: String,
    val phone: String,
    val area: String?,
    val street: String?
)

data class OrderItem(
    @SerializedName("product_id") val productId: Long,
    val name: String,
    val quantity: Int,
    @SerializedName("unit_price") val unitPrice: String,
    @SerializedName("image_url") val imageUrl: String?
)

data class OrderShipmentRef(
    val id: Long,
    @SerializedName("tracking_number") val trackingNumber: String,
    val status: String,
    @SerializedName("estimated_date") val estimatedDate: String?
)

data class Order(
    val id: Long,
    @SerializedName("order_number") val orderNumber: String,
    val status: String,
    @SerializedName("payment_status") val paymentStatus: String,
    val total: String,
    @SerializedName("created_at") val createdAt: String?,
    val items: List<OrderItem>? = null,
    val shipments: List<OrderShipmentRef>? = null
)

data class OrderPage(
    val items: List<Order>,
    val page: Int,
    @SerializedName("per_page") val perPage: Int
)

// ---------- Logistics ----------

data class Quote(
    val id: Long,
    @SerializedName("quote_number") val quoteNumber: String,
    @SerializedName("total_amount") val totalAmount: String,
    val currency: String,
    @SerializedName("transit_days") val transitDays: String?,
    val status: String,
    @SerializedName("valid_until") val validUntil: String?,
    @SerializedName("shipment_id") val shipmentId: Long,
    @SerializedName("tracking_number") val trackingNumber: String?,
    val origin: String?,
    val destination: String?
)

data class ShipmentEvent(
    val status: String,
    val location: String?,
    val note: String?,
    @SerializedName("happened_at") val happenedAt: String
)

data class CargoPackage(
    @SerializedName("package_type") val packageType: String?,
    val quantity: Int,
    @SerializedName("weight_kg") val weightKg: String?,
    val description: String?
)

data class LastKnownLocation(
    val latitude: String,
    val longitude: String,
    @SerializedName("recorded_at") val recordedAt: String
)

data class Shipment(
    val id: Long,
    @SerializedName("tracking_number") val trackingNumber: String,
    val origin: String?,
    val destination: String?,
    val status: String,
    @SerializedName("delivery_speed") val deliverySpeed: String?,
    @SerializedName("estimated_date") val estimatedDate: String?,
    val events: List<ShipmentEvent>? = null,
    val packages: List<CargoPackage>? = null,
    @SerializedName("last_known_location") val lastKnownLocation: LastKnownLocation? = null
)

data class ShipmentPage(
    val items: List<Shipment>,
    val page: Int,
    @SerializedName("per_page") val perPage: Int
)

data class DriverDelivery(
    @SerializedName("shipment_id") val shipmentId: Long,
    @SerializedName("tracking_number") val trackingNumber: String,
    val origin: String?,
    val destination: String?,
    val status: String,
    @SerializedName("delivery_speed") val deliverySpeed: String?,
    @SerializedName("accepted_at") val acceptedAt: String?,
    @SerializedName("completed_at") val completedAt: String?,
    val earnings: String?
)

data class AppNotification(
    val id: Long,
    val title: String?,
    val body: String?,
    @SerializedName("read_at") val readAt: String?,
    @SerializedName("created_at") val createdAt: String
)

data class SimpleMessage(val message: String)
