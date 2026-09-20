package africa.amoper.app.ui.orders

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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailScreen(orderId: Long, viewModel: OrdersViewModel, onBack: () -> Unit) {
    LaunchedEffect(orderId) { viewModel.loadOrderDetail(orderId) }
    val order = viewModel.selectedOrder

    Scaffold(
        topBar = { TopAppBar(title = { Text("Order detail") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                order == null -> Text("Order not found", Modifier.align(Alignment.Center))
                else -> Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp)) {
                    Text(order.orderNumber, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Text("Status: ${order.status}", style = MaterialTheme.typography.bodyMedium)
                    Text("Payment: ${order.paymentStatus}", style = MaterialTheme.typography.bodyMedium)
                    Spacer(Modifier.height(16.dp))
                    Text("Items", style = MaterialTheme.typography.titleMedium)
                    order.items?.forEach { item ->
                        Spacer(Modifier.height(6.dp))
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("${item.name} × ${item.quantity}")
                            Text("K ${item.unitPrice}")
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", fontWeight = FontWeight.Bold)
                        Text("K ${order.total}", fontWeight = FontWeight.Bold)
                    }

                    if (!order.shipments.isNullOrEmpty()) {
                        Spacer(Modifier.height(20.dp))
                        Text("Shipments", style = MaterialTheme.typography.titleMedium)
                        order.shipments.forEach { shipment ->
                            Spacer(Modifier.height(6.dp))
                            Text("${shipment.trackingNumber} — ${shipment.status}")
                        }
                    }
                    Spacer(Modifier.height(24.dp))
                }
            }
        }
    }
}
