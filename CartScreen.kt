package africa.amoper.app.ui.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import africa.amoper.app.data.model.CartItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onCheckout: () -> Unit
) {
    LaunchedEffect(Unit) { viewModel.loadCart() }
    val cart = viewModel.cart

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Your cart") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } })
        },
        bottomBar = {
            if (cart != null && cart.items.isNotEmpty()) {
                Surface(shadowElevation = 8.dp) {
                    Column(Modifier.padding(16.dp)) {
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Subtotal", style = MaterialTheme.typography.titleMedium)
                            Text("K ${cart.subtotal}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Spacer(Modifier.height(8.dp))
                        Button(onClick = onCheckout, modifier = Modifier.fillMaxWidth().height(48.dp)) { Text("Checkout") }
                    }
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoadingCart -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                cart == null || cart.items.isEmpty() -> Text("Your cart is empty", Modifier.align(Alignment.Center))
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(cart.items, key = { it.productId }) { item ->
                        CartRow(
                            item = item,
                            onQuantityChange = { viewModel.updateCartQuantity(item.productId, it) },
                            onRemove = { viewModel.removeFromCart(item.productId) }
                        )
                        Divider()
                    }
                }
            }
        }
    }
}

@Composable
private fun CartRow(item: CartItem, onQuantityChange: (Int) -> Unit, onRemove: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
        AsyncImage(
            model = item.imageUrl, contentDescription = item.name, contentScale = ContentScale.Crop,
            modifier = Modifier.size(64.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(item.name, style = MaterialTheme.typography.bodyMedium, maxLines = 2)
            Text("K ${item.unitPrice} × ${item.quantity} = K ${item.lineTotal}", style = MaterialTheme.typography.bodySmall)
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { onQuantityChange(item.quantity - 1) }) { Text("−") }
                Text("${item.quantity}")
                IconButton(onClick = { onQuantityChange(item.quantity + 1) }) { Text("+") }
            }
        }
        IconButton(onClick = onRemove) { Icon(Icons.Filled.Delete, contentDescription = "Remove") }
    }
}
