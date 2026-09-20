package africa.amoper.app.data.repository

import africa.amoper.app.data.model.DriverDelivery
import africa.amoper.app.data.network.ApiService
import africa.amoper.app.data.network.safeApiCall

class DriverRepository(private val api: ApiService) {

    suspend fun deliveries(): Result<List<DriverDelivery>> = safeApiCall { api.driverDeliveries() }

    suspend fun updateStatus(shipmentId: Long, status: String, note: String?, location: String?): Result<Map<String, String>> {
        val body = mapOf("status" to status, "note" to note, "location" to location)
        return safeApiCall { api.updateDeliveryStatus(route = "driver/deliveries/$shipmentId/status", body = body) }
    }

    suspend fun pingLocation(shipmentId: Long?, latitude: Double, longitude: Double): Result<Unit> {
        val body: Map<String, Any?> = mapOf(
            "shipment_id" to shipmentId, "latitude" to latitude, "longitude" to longitude
        )
        return safeApiCall { api.pingLocation(body = body) }.map { }
    }
}
