package africa.amoper.app.ui.driver

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.google.android.gms.location.LocationServices
import africa.amoper.app.data.model.DriverDelivery
import kotlinx.coroutines.delay

private val statusFlow = listOf("picked_up", "in_transit", "out_for_delivery", "delivered")

@SuppressLint("MissingPermission")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DriverDashboardScreen(viewModel: DriverViewModel) {
    val context = LocalContext.current
    LaunchedEffect(Unit) { viewModel.loadDeliveries() }

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        hasLocationPermission = granted
        if (granted) viewModel.isSharingLocation = true
    }

    // While sharing is on, ping the last active (non-delivered) shipment's location every ~20s.
    LaunchedEffect(viewModel.isSharingLocation) {
        if (!viewModel.isSharingLocation || !hasLocationPermission) return@LaunchedEffect
        val client = LocationServices.getFusedLocationProviderClient(context)
        val activeShipmentId = viewModel.deliveries.firstOrNull { it.completedAt == null }?.shipmentId
        while (viewModel.isSharingLocation) {
            client.lastLocation.addOnSuccessListener { loc ->
                if (loc != null) viewModel.pingLocation(activeShipmentId, loc.latitude, loc.longitude)
            }
            delay(20_000)
        }
    }

    Scaffold(topBar = { TopAppBar(title = { Text("Driver dashboard") }) }) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Card(Modifier.fillMaxWidth().padding(12.dp)) {
                Row(
                    Modifier.fillMaxWidth().padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Share my location", fontWeight = FontWeight.SemiBold)
                        Text("Sends a GPS update every 20s while on a delivery", style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(
                        checked = viewModel.isSharingLocation,
                        onCheckedChange = { enabled ->
                            if (enabled && !hasLocationPermission) {
                                permissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION)
                            } else {
                                viewModel.isSharingLocation = enabled
                            }
                        }
                    )
                }
            }

            when {
                viewModel.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                viewModel.deliveries.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No deliveries assigned yet") }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(viewModel.deliveries, key = { it.shipmentId }) { delivery ->
                        DeliveryCard(delivery, onAdvance = { next -> viewModel.updateStatus(delivery.shipmentId, next) })
                    }
                }
            }
        }
    }
}

@Composable
private fun DeliveryCard(delivery: DriverDelivery, onAdvance: (String) -> Unit) {
    Card(Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(delivery.trackingNumber, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("${delivery.origin ?: "?"} → ${delivery.destination ?: "?"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text(delivery.status.replace('_', ' ')) })

            if (delivery.completedAt == null) {
                val currentIndex = statusFlow.indexOf(delivery.status)
                val next = statusFlow.getOrNull(currentIndex + 1)
                Spacer(Modifier.height(10.dp))
                Row {
                    if (next != null) {
                        Button(onClick = { onAdvance(next) }) { Text("Mark ${next.replace('_', ' ')}") }
                    }
                    Spacer(Modifier.width(8.dp))
                    OutlinedButton(onClick = { onAdvance("delivery_failed") }) { Text("Failed attempt") }
                }
            } else {
                Text("Delivered ✓", color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}
