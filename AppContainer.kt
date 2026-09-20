package africa.amoper.app

import android.content.Context
import africa.amoper.app.data.network.NetworkModule
import africa.amoper.app.data.network.TokenStore
import africa.amoper.app.data.repository.AuthRepository
import africa.amoper.app.data.repository.DriverRepository
import africa.amoper.app.data.repository.LogisticsRepository
import africa.amoper.app.data.repository.MarketplaceRepository

/** Small hand-rolled dependency container — no DI framework needed for this app's size. */
class AppContainer(context: Context) {
    private val tokenStore = TokenStore(context.applicationContext)
    private val api = NetworkModule.buildApi(tokenStore)

    val authRepository = AuthRepository(api, tokenStore)
    val marketplaceRepository = MarketplaceRepository(api)
    val logisticsRepository = LogisticsRepository(api)
    val driverRepository = DriverRepository(api)
}

class AmoperApplication : android.app.Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
