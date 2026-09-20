package africa.amoper.app.ui.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import africa.amoper.app.data.model.ProductSummary

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductListScreen(
    viewModel: MarketplaceViewModel,
    onOpenProduct: (Long) -> Unit,
    onOpenCart: () -> Unit
) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AMOPER Market") },
                actions = {
                    IconButton(onClick = onOpenCart) {
                        Icon(Icons.Filled.ShoppingCart, contentDescription = "Cart")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.padding(padding)) {
            OutlinedTextField(
                value = viewModel.searchQuery,
                onValueChange = { viewModel.updateSearch(it); viewModel.submitSearch() },
                placeholder = { Text("Search products") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth().padding(12.dp)
            )

            if (viewModel.categories.isNotEmpty()) {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(viewModel.categories.filter { it.parentId == null }) { category ->
                        FilterChip(
                            selected = viewModel.selectedCategory == category.id,
                            onClick = { viewModel.selectCategory(category.id) },
                            label = { Text(category.name) }
                        )
                    }
                }
                Spacer(Modifier.height(8.dp))
            }

            when {
                viewModel.isLoadingProducts -> Box(Modifier.fillMaxSize(), Alignment.Center) { CircularProgressIndicator() }
                viewModel.products.isEmpty() -> Box(Modifier.fillMaxSize(), Alignment.Center) { Text("No products found") }
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(viewModel.products, key = { it.id }) { product ->
                        ProductCard(product = product, onClick = { onOpenProduct(product.id) })
                    }
                }
            }
        }
    }
}

@Composable
private fun ProductCard(product: ProductSummary, onClick: () -> Unit) {
    Card(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
        Column {
            AsyncImage(
                model = product.imageUrl,
                contentDescription = product.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            )
            Column(Modifier.padding(10.dp)) {
                Text(product.name, maxLines = 2, overflow = TextOverflow.Ellipsis, style = MaterialTheme.typography.bodyMedium)
                Spacer(Modifier.height(4.dp))
                Text(
                    "K ${product.displayPrice}",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}
