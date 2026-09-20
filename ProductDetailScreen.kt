package africa.amoper.app.ui.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: Long,
    viewModel: MarketplaceViewModel,
    onBack: () -> Unit,
    onGoToCart: () -> Unit
) {
    LaunchedEffect(productId) { viewModel.loadProductDetail(productId) }
    var added by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Product") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.Filled.ArrowBack, null) } }
            )
        }
    ) { padding ->
        val product = viewModel.productDetail
        Box(Modifier.padding(padding).fillMaxSize()) {
            when {
                viewModel.isLoadingDetail -> CircularProgressIndicator(Modifier.align(Alignment.Center))
                product == null -> Text("Product not found", Modifier.align(Alignment.Center))
                else -> Column(Modifier.verticalScroll(rememberScrollState())) {
                    if (product.images.isNotEmpty()) {
                        LazyRow {
                            items(product.images) { img ->
                                AsyncImage(
                                    model = img, contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.size(320.dp).padding(4.dp)
                                )
                            }
                        }
                    }
                    Column(Modifier.padding(16.dp)) {
                        Text(product.name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        product.brand?.let { Text(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
                        Spacer(Modifier.height(6.dp))
                        Text("K ${product.displayPrice}", style = MaterialTheme.typography.headlineSmall, color = MaterialTheme.colorScheme.primary)
                        product.sellerName?.let {
                            Spacer(Modifier.height(4.dp))
                            Text("Sold by $it", style = MaterialTheme.typography.labelMedium)
                        }
                        Spacer(Modifier.height(16.dp))
                        Button(
                            onClick = { viewModel.addToCart(product.id); added = true },
                            modifier = Modifier.fillMaxWidth().height(48.dp)
                        ) { Text(if (added) "Added — add another" else "Add to cart") }

                        if (added) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(onClick = onGoToCart, modifier = Modifier.fillMaxWidth()) { Text("Go to cart") }
                        }

                        if (!product.description.isNullOrBlank()) {
                            Spacer(Modifier.height(20.dp))
                            Text("Description", style = MaterialTheme.typography.titleMedium)
                            Spacer(Modifier.height(6.dp))
                            Text(product.description)
                        }

                        if (product.specifications.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Text("Specifications", style = MaterialTheme.typography.titleMedium)
                            product.specifications.forEach {
                                Spacer(Modifier.height(4.dp))
                                Text("${it.key}: ${it.value}", style = MaterialTheme.typography.bodyMedium)
                            }
                        }

                        if (product.reviews.isNotEmpty()) {
                            Spacer(Modifier.height(20.dp))
                            Text("Reviews", style = MaterialTheme.typography.titleMedium)
                            product.reviews.forEach { review ->
                                Spacer(Modifier.height(8.dp))
                                Text("${"★".repeat(review.rating)} ${review.reviewer ?: ""}", fontWeight = FontWeight.SemiBold)
                                review.body?.let { Text(it, style = MaterialTheme.typography.bodySmall) }
                            }
                        }
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }
    }
}
