package reelsdrama.freedrama.videosdrama

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import reelsdrama.freedrama.videosdrama.core.ads.AdInitializer
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var adInitializer: AdInitializer

    override fun onCreate() {
        super.onCreate()
        // Foundation layer only: starts GMA Next-Gen SDK init on a background thread.
        // No ad-loading/display logic here - that's built on top of this later.
        adInitializer.initialize()
    }
}
