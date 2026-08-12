package reelsdrama.freedrama.videosdrama.core.ads

import android.content.Context
import com.google.android.libraries.ads.mobile.sdk.MobileAds
import com.google.android.libraries.ads.mobile.sdk.initialization.InitializationConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import reelsdrama.freedrama.videosdrama.core.constants.AdConstants
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Foundation layer for AdMob (GMA Next-Gen SDK) integration.
 *
 * This class is responsible ONLY for one-time SDK initialization and exposing whether it
 * has completed. It does not load or show any ads - that's built on top of this in a
 * later part of the integration.
 */
@Singleton
class AdInitializer @Inject constructor(
    @ApplicationContext private val context: Context
) {
    // App-process-lifetime scope: initialization happens once and this class is a
    // Hilt singleton, so there's no owner to cancel this scope against.
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val _isInitialized = MutableStateFlow(false)

    /** True once MobileAds initialization (SDK + mediation adapters) has completed. */
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private var hasStarted = false

    /**
     * Kicks off MobileAds initialization on a background thread, per GMA Next-Gen SDK's
     * requirement (calling it on the main thread risks an ANR). Safe to call more than
     * once - only the first call actually starts initialization.
     */
    fun initialize() {
        if (hasStarted) return
        hasStarted = true

        initScope.launch {
            MobileAds.initialize(
                context,
                InitializationConfig.Builder(AdConstants.ADMOB_APP_ID).build()
            ) {
                // Fires once GMA Next-Gen SDK + adapter initialization finish (or after
                // the SDK's internal 30s timeout).
                _isInitialized.value = true
            }
        }
    }
}
