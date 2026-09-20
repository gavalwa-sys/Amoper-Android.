package africa.amoper.app.ui.nav

object Routes {
    const val LOGIN = "login"
    const val REGISTER = "register"

    const val PRODUCT_LIST = "products"
    const val PRODUCT_DETAIL = "products/{productId}"
    const val CART = "cart"
    const val CHECKOUT = "checkout"

    const val ORDER_LIST = "orders"
    const val ORDER_DETAIL = "orders/{orderId}"

    const val QUOTE_REQUEST = "logistics/quote"
    const val SHIPMENT_LIST = "logistics/shipments"
    const val SHIPMENT_DETAIL = "logistics/shipments/{shipmentId}"

    const val DRIVER_DASHBOARD = "driver"
    const val PROFILE = "profile"

    fun productDetail(id: Long) = "products/$id"
    fun orderDetail(id: Long) = "orders/$id"
    fun shipmentDetail(id: Long) = "logistics/shipments/$id"
}

enum class BottomTab(val route: String, val label: String) {
    Market(Routes.PRODUCT_LIST, "Market"),
    Logistics(Routes.SHIPMENT_LIST, "Logistics"),
    Orders(Routes.ORDER_LIST, "Orders"),
    Driver(Routes.DRIVER_DASHBOARD, "Driver"),
    Profile(Routes.PROFILE, "Profile")
}
