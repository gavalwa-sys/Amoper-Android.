package africa.amoper.app.ui.logistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.amoper.app.data.model.Shipment

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShipmentsScreen(
    viewModel: LogisticsViewModel,
    onOpenShipment: (Long) -> Unit,
    onNewQuote: () -> Unit,
    onTrackResult: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadShipments() }
    var trackingInput by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Logistics") }) },
        floatingActionButton = {
            ExtendedFloatingActionButton(onClick = onNewQuote, icon = { Icon(Icons.Filled.Add, null) }, text = { Text("New quote") })
        }
    ) { padding ->
        Column(Modifier.padding(padding).fillMaxSize()) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = trackingInput, onValueChange = { trackingInput = it },
                    label = { Text("Track by number") }, singleLine = true, modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
                Button(onClick = {
                    if (trackingInput.isNotBlank()) {
                        viewModel.trackByNumber(trackingInput)
                        onTrackResult()
                    }
                }) { Text("Track") }
            }

            when {
                viewModel.isLoading -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                viewModel.shipments.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No shipments yet — request a quote to get started") }
                else -> LazyColumn(Modifier.fillMaxSize().padding(horizontal = 12.dp)) {
                    items(viewModel.shipments, key = { it.id }) { shipment ->
                        ShipmentRow(shipment) { onOpenShipment(shipment.id) }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShipmentRow(shipment: Shipment, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Text(shipment.trackingNumber, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(2.dp))
            Text("${shipment.origin ?: "?"} → ${shipment.destination ?: "?"}", style = MaterialTheme.typography.bodyMedium)
            Spacer(Modifier.height(4.dp))
            AssistChip(onClick = {}, label = { Text(shipment.status.replace('_', ' ')) })
        }
    }
}
