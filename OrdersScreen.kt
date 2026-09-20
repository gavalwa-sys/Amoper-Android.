package africa.amoper.app.ui.orders

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import africa.amoper.app.data.model.Order

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(viewModel: OrdersViewModel, onOpenOrder: (Long) -> Unit) {
    LaunchedEffect(Unit) { viewModel.loadOrders() }

    Scaffold(topBar = { TopAppBar(title = { Text("My orders") }) }) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoading -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                viewModel.orders.isEmpty() -> Text("No orders yet", Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize().padding(12.dp)) {
                    items(viewModel.orders, key = { it.id }) { order -> OrderRow(order) { onOpenOrder(order.id) } }
                }
            }
        }
    }
}

@Composable
private fun OrderRow(order: Order, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(order.orderNumber, fontWeight = FontWeight.Bold)
                Text("K ${order.total}", fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(4.dp))
            Text("Status: ${order.status} · Payment: ${order.paymentStatus}", style = MaterialTheme.typography.bodySmall)
        }
    }
}
