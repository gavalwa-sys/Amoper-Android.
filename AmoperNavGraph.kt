package africa.amoper.app.ui.nav

import africa.amoper.app.AppContainer
import africa.amoper.app.ui.auth.AuthViewModel
import africa.amoper.app.ui.auth.LoginScreen
import africa.amoper.app.ui.auth.RegisterScreen
import africa.amoper.app.ui.auth.SessionState
import africa.amoper.app.ui.driver.DriverDashboardScreen
import africa.amoper.app.ui.driver.DriverViewModel
import africa.amoper.app.ui.logistics.LogisticsViewModel
import africa.amoper.app.ui.logistics.QuoteRequestScreen
import africa.amoper.app.ui.logistics.ShipmentDetailScreen
import africa.amoper.app.ui.logistics.ShipmentsScreen
import africa.amoper.app.ui.marketplace.CartScreen
import africa.amoper.app.ui.marketplace.CheckoutScreen
import africa.amoper.app.ui.marketplace.MarketplaceViewModel
import africa.amoper.app.ui.marketplace.ProductDetailScreen
import africa.amoper.app.ui.marketplace.ProductListScreen
import africa.amoper.app.ui.orders.OrderDetailScreen
import africa.amoper.app.ui.orders.OrdersScreen
import africa.amoper.app.ui.orders.OrdersViewModel
import africa.amoper.app.ui.profile.ProfileScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.ListAlt
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.navigation.NavType

@Composable
fun AmoperApp(container: AppContainer) {
    val authViewModel: AuthViewModel = viewModel(factory = AuthViewModel.factory(container.authRepository))

    when (val state = authViewModel.session) {
        is SessionState.Loading -> Box(Modifier) { CircularProgressIndicator() }
        is SessionState.LoggedOut -> AuthNavHost(authViewModel)
        is SessionState.LoggedIn -> MainScaffold(container, authViewModel, state)
    }
}

@Composable
private fun AuthNavHost(authViewModel: AuthViewModel) {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = Routes.LOGIN) {
        composable(Routes.LOGIN) {
            LoginScreen(viewModel = authViewModel, onGoToRegister = { navController.navigate(Routes.REGISTER) })
        }
        composable(Routes.REGISTER) {
            RegisterScreen(viewModel = authViewModel, onBackToLogin = { navController.popBackStack() })
        }
    }
}

@Composable
private fun MainScaffold(container: AppContainer, authViewModel: AuthViewModel, state: SessionState.LoggedIn) {
    val navController = rememberNavController()
    val marketplaceViewModel: MarketplaceViewModel = viewModel(factory = MarketplaceViewModel.factory(container.marketplaceRepository))
    val ordersViewModel: OrdersViewModel = viewModel(factory = OrdersViewModel.factory(container.marketplaceRepository))
    val logisticsViewModel: LogisticsViewModel = viewModel(factory = LogisticsViewModel.factory(container.logisticsRepository))
    val driverViewModel: DriverViewModel = viewModel(factory = DriverViewModel.factory(container.driverRepository))

    val tabs = remember(state.user.isDriver) {
        if (state.user.isDriver) listOf(BottomTab.Market, BottomTab.Logistics, BottomTab.Orders, BottomTab.Driver, BottomTab.Profile)
        else listOf(BottomTab.Market, BottomTab.Logistics, BottomTab.Orders, BottomTab.Profile)
    }

    Scaffold(
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentRoute = backStackEntry?.destination?.route
            NavigationBar {
                tabs.forEach { tab ->
                    NavigationBarItem(
                        selected = currentRoute == tab.route,
                        onClick = {
                            navController.navigate(tab.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(tabIcon(tab), contentDescription = tab.label) },
                        label = { Text(tab.label) }
                    )
                }
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            NavHost(navController = navController, startDestination = BottomTab.Market.route) {
                composable(Routes.PRODUCT_LIST) {
                    ProductListScreen(
                        viewModel = marketplaceViewModel,
                        onOpenProduct = { navController.navigate(Routes.productDetail(it)) },
                        onOpenCart = { navController.navigate(Routes.CART) }
                    )
                }
                composable(
                    Routes.PRODUCT_DETAIL,
                    arguments = listOf(navArgument("productId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("productId") ?: 0L
                    ProductDetailScreen(
                        productId = id, viewModel = marketplaceViewModel,
                        onBack = { navController.popBackStack() },
                        onGoToCart = { navController.navigate(Routes.CART) }
                    )
                }
                composable(Routes.CART) {
                    CartScreen(
                        viewModel = marketplaceViewModel,
                        onBack = { navController.popBackStack() },
                        onCheckout = { navController.navigate(Routes.CHECKOUT) }
                    )
                }
                composable(Routes.CHECKOUT) {
                    CheckoutScreen(
                        viewModel = marketplaceViewModel,
                        onBack = { navController.popBackStack() },
                        onOrderPlaced = { orderId ->
                            navController.navigate(Routes.orderDetail(orderId)) {
                                popUpTo(Routes.PRODUCT_LIST)
                            }
                        }
                    )
                }

                composable(Routes.ORDER_LIST) {
                    OrdersScreen(viewModel = ordersViewModel, onOpenOrder = { navController.navigate(Routes.orderDetail(it)) })
                }
                composable(
                    Routes.ORDER_DETAIL,
                    arguments = listOf(navArgument("orderId") { type = NavType.LongType })
                ) { backStackEntry ->
                    val id = backStackEntry.arguments?.getLong("orderId") ?: 0L
                    OrderDetailScreen(orderId = id, viewModel = ordersViewModel, onBack = { navController.popBackStack() })
                }

                composable(Routes.SHIPMENT_LIST) {
                    ShipmentsScreen(
                        viewModel = logisticsViewModel,
                        onOpenShipment = { navController.navigate(Routes.shipmentDetail(it)) },
                        onNewQuote = { navController.navigate(Routes.QUOTE_REQUEST) },
                        onTrackResult = { navController.navigate(Routes.shipmentDetail(0L) + "?tracked=1") }
                    )
                }
                composable(
                    Routes.SHIPMENT_DETAIL + "?tracked={tracked}",
                    arguments = listOf(
                        navArgument("shipmentId") { type = NavType.LongType },
                        navArgument("tracked") { type = NavType.StringType; defaultValue = "0" }
                    )
                ) { backStackEntry ->
                    val tracked = backStackEntry.arguments?.getString("tracked") == "1"
                    val id = backStackEntry.arguments?.getLong("shipmentId") ?: 0L
                    ShipmentDetailScreen(
                        shipmentId = if (tracked) null else id,
                        viewModel = logisticsViewModel,
                        onBack = { navController.popBackStack() }
                    )
                }
                composable(Routes.QUOTE_REQUEST) {
                    QuoteRequestScreen(
                        viewModel = logisticsViewModel,
                        onBack = { navController.popBackStack() },
                        onBooked = { shipmentId ->
                            navController.navigate(Routes.shipmentDetail(shipmentId)) { popUpTo(Routes.SHIPMENT_LIST) }
                        }
                    )
                }

                composable(BottomTab.Driver.route) {
                    DriverDashboardScreen(viewModel = driverViewModel)
                }

                composable(Routes.PROFILE) {
                    ProfileScreen(user = state.user, onLogout = { authViewModel.logout() })
                }
            }
        }
    }
}

private fun tabIcon(tab: BottomTab) = when (tab) {
    BottomTab.Market -> Icons.Filled.Storefront
    BottomTab.Logistics -> Icons.Filled.LocalShipping
    BottomTab.Orders -> Icons.Filled.ListAlt
    BottomTab.Driver -> Icons.Filled.DirectionsCar
    BottomTab.Profile -> Icons.Filled.Person
}
