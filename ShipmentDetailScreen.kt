package africa.amoper.app.ui.logistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp

/** Pass shipmentId to (re)load from the API, or null to just render the already-loaded selectedShipment (used after a tracking-number lookup). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentDetailScreen(shipmentId: Long?, viewModel: LogisticsViewModel, onBack: () -> Unit) {
    LaunchedEffect(shipmentId) { if (shipmentId != null) viewModel.loadShipmentDetail(shipmentId) }
    val shipment = viewModel.selectedShipment

    Scaffold(
        topBar = { TopAppBar(title = { Text("Shipment tracking") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                shipment == null -> Text(viewModel.errorMessage ?: "Shipment not found", Modifier.align(Alignment.Center))
                else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(shipment.trackingNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(4.dp))
                    Text("${shipment.origin ?: "?"} → ${shipment.destination ?: "?"}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(4.dp))
                    AssistChip(onClick = {}, label = { Text(shipment.status.replace('_', ' ')) })

                    shipment.lastKnownLocation?.let { loc ->
                        Spacer(Modifier.height(12.dp))
                        Text("Last known location: ${loc.latitude}, ${loc.longitude}", style = MaterialTheme.typography.bodySmall)
                        Text("as of ${loc.recordedAt}", style = MaterialTheme.typography.bodySmall)
                    }

                    if (!shipment.packages.isNullOrEmpty()) {
                        Spacer(Modifier.height(16.dp))
                        Text("Package", style = MaterialTheme.typography.titleMedium)
                        shipment.packages.forEach { pkg ->
                            Spacer(Modifier.height(4.dp))
                            Text("${pkg.packageType ?: "Parcel"} · ${pkg.weightKg ?: "?"} kg × ${pkg.quantity}")
                            pkg.description?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text("Tracking history", style = MaterialTheme.typography.titleMedium)
                    shipment.events?.forEach { event ->
                        Spacer(Modifier.height(10.dp))
                        Text(event.status.replace('_', ' '), fontWeight = FontWeight.SemiBold)
                        Text(event.happenedAt, style = MaterialTheme.typography.bodySmall)
                        event.note?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                        Divider(Modifier.padding(top = 8.dp))
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
