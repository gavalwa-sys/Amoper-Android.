package africa.amoper.app.ui.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onOrderPlaced: (Long) -> Unit
) {
    var fullName by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var area by remember { mutableStateOf("") }
    var street by remember { mutableStateOf("") }
    val deliveryFee = 30.0

    LaunchedEffect(viewModel.checkoutSuccess) {
        viewModel.checkoutSuccess?.let {
            onOrderPlaced(it.id)
            viewModel.resetCheckoutSuccess()
        }
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Checkout") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            Text("Delivery details", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(fullName, { fullName = it }, label = { Text("Full name") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(phone, { phone = it }, label = { Text("Phone") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(area, { area = it }, label = { Text("Area / suburb") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            OutlinedTextField(street, { street = it }, label = { Text("Street / building") }, singleLine = true, modifier = Modifier.fillMaxWidth())

            Spacer(Modifier.height(16.dp))
            val subtotal = viewModel.cart?.subtotal ?: 0.0
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(Modifier.padding(12.dp)) {
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Subtotal"); Text("K $subtotal") }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) { Text("Delivery fee"); Text("K $deliveryFee") }
                    Divider(Modifier.padding(vertical = 6.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Total", style = MaterialTheme.typography.titleMedium)
                        Text("K ${subtotal + deliveryFee}", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            viewModel.errorMessage?.let {
                Spacer(Modifier.height(8.dp))
                Text(it, color = MaterialTheme.colorScheme.error)
            }

            Spacer(Modifier.height(16.dp))
            Button(
                onClick = { viewModel.placeOrder(fullName, phone, area, street, deliveryFee) },
                enabled = !viewModel.isCheckingOut && fullName.isNotBlank() && phone.isNotBlank(),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (viewModel.isCheckingOut) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                else Text("Place order")
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
