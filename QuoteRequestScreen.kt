package africa.amoper.app.ui.logistics

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp

private val speedOptions = listOf("economy" to "Economy (6-9 days)", "standard" to "Standard (3-5 days)", "express" to "Express (1-2 days)")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QuoteRequestScreen(
    viewModel: LogisticsViewModel,
    onBack: () -> Unit,
    onBooked: (Long) -> Unit
) {
    var origin by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf("") }
    var speed by remember { mutableStateOf("standard") }
    var weight by remember { mutableStateOf("") }
    var packageType by remember { mutableStateOf("parcel") }
    var description by remember { mutableStateOf("") }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Request a quote") }, navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }) }
    ) { padding ->
        Column(Modifier.padding(padding).verticalScroll(rememberScrollState()).padding(16.dp)) {
            val quote = viewModel.latestQuote
            if (quote == null) {
                OutlinedTextField(origin, { origin = it }, label = { Text("Origin (city, country)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(destination, { destination = it }, label = { Text("Destination (city, country)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(
                    weight, { weight = it }, label = { Text("Weight (kg)") }, singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal), modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(packageType, { packageType = it }, label = { Text("Package type (parcel, pallet, vehicle...)") }, singleLine = true, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(10.dp))
                OutlinedTextField(description, { description = it }, label = { Text("Description (optional)") }, modifier = Modifier.fillMaxWidth())

                Spacer(Modifier.height(16.dp))
                Text("Delivery speed", style = MaterialTheme.typography.labelLarge)
                Column(Modifier.selectableGroup()) {
                    speedOptions.forEach { (value, label) ->
                        Row(
                            Modifier.fillMaxWidth().selectable(selected = speed == value, onClick = { speed = value }, role = Role.RadioButton).padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = speed == value, onClick = null)
                            Spacer(Modifier.width(8.dp))
                            Text(label)
                        }
                    }
                }

                viewModel.errorMessage?.let {
                    Spacer(Modifier.height(8.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = {
                        viewModel.requestQuote(origin, destination, speed, weight.toDoubleOrNull() ?: 1.0, packageType, description.ifBlank { null })
                    },
                    enabled = !viewModel.isSubmittingQuote && origin.isNotBlank() && destination.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp)
                ) {
                    if (viewModel.isSubmittingQuote) CircularProgressIndicator(Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    else Text("Get quote")
                }
            } else {
                Text("Your quote", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("${quote.origin} → ${quote.destination}")
                        Spacer(Modifier.height(6.dp))
                        Text("Transit time: ${quote.transitDays ?: "-"}")
                        Spacer(Modifier.height(6.dp))
                        Text("Total: ${quote.currency} ${quote.totalAmount}", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("Valid until ${quote.validUntil ?: "-"}", style = MaterialTheme.typography.bodySmall)
                    }
                }
                Spacer(Modifier.height(16.dp))
                Button(onClick = { viewModel.acceptQuote(quote.id, onBooked) }, modifier = Modifier.fillMaxWidth().height(50.dp)) {
                    Text("Accept & book shipment")
                }
                Spacer(Modifier.height(8.dp))
                OutlinedButton(onClick = { viewModel.clearQuote() }, modifier = Modifier.fillMaxWidth()) { Text("Request a different quote") }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
