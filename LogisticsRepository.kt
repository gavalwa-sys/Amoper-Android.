package africa.amoper.app.data.repository

import africa.amoper.app.data.model.*
import africa.amoper.app.data.network.ApiService
import africa.amoper.app.data.network.safeApiCall

class LogisticsRepository(private val api: ApiService) {

    suspend fun requestQuote(
        origin: String,
        destination: String,
        deliverySpeed: String,
        weightKg: Double,
        packageType: String,
        description: String?
    ): Result<Quote> {
        val body: Map<String, Any?> = mapOf(
            "origin" to origin, "destination" to destination, "delivery_speed" to deliverySpeed,
            "weight_kg" to weightKg, "package_type" to packageType, "description" to description
        )
        return safeApiCall { api.requestQuote(body = body) }
    }

    suspend fun quotes(): Result<List<Quote>> = safeApiCall { api.quotes() }

    suspend fun acceptQuote(id: Long): Result<Shipment> =
        safeApiCall { api.acceptQuote(route = "logistics/quotes/$id/accept") }

    suspend fun shipments(): Result<ShipmentPage> = safeApiCall { api.shipments() }

    suspend fun shipmentDetail(id: Long): Result<Shipment> =
        safeApiCall { api.shipmentDetail(route = "logistics/shipments/$id") }

    suspend fun trackShipment(trackingNumber: String): Result<Shipment> =
        safeApiCall { api.trackShipment(route = "logistics/track/$trackingNumber") }

    suspend fun notifications(): Result<List<AppNotification>> = safeApiCall { api.notifications() }

    suspend fun markNotificationRead(id: Long): Result<SimpleMessage> =
        safeApiCall { api.markNotificationRead(route = "notifications/$id/read") }
}
