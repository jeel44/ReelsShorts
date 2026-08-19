package reelsdrama.freedrama.videosdrama.core.referrer

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.android.installreferrer.api.InstallReferrerClient
import com.android.installreferrer.api.InstallReferrerClient.InstallReferrerResponse
import com.android.installreferrer.api.InstallReferrerStateListener
import com.android.installreferrer.api.ReferrerDetails
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.net.URLDecoder
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

/**
 * Foundation layer for Google Play Install Referrer attribution - same "init once, fire and
 * forget, fail silently" shape as [reelsdrama.freedrama.videosdrama.core.ads.AdInitializer] and
 * [reelsdrama.freedrama.videosdrama.core.notifications.OneSignalManager].
 *
 * Fetches the raw Play install referrer string exactly once per install (gated on the
 * [PreferencesKeys.INSTALL_REFERRER_SENT] DataStore flag, mirroring
 * [reelsdrama.freedrama.videosdrama.core.notifications.OneSignalManager]'s
 * `onesignal_dialog_shown` flag) and POSTs it to the backend. The flag is written ONLY after a
 * successful (2xx) response, so a failed send (no network, backend down, Play Services
 * unavailable, etc.) naturally retries on the next app open instead of being lost.
 *
 * Every failure mode here is swallowed - this must never crash or delay the splash flow it's
 * triggered from (see [maybeSendInstallReferrer]'s call site in `SplashViewModel.startTimer`).
 */
@Singleton
class InstallReferrerManager @Inject constructor(
    @ApplicationContext private val context: Context,
    // Same shared DataStore<Preferences> instance SettingsModule provides as a @Singleton -
    // same idiom OneSignalManager uses for its own one-off persisted flag.
    private val dataStore: DataStore<Preferences>,
    // Reuses NetworkModule's existing OkHttpClient singleton rather than standing up a second
    // client - this backend's host differs from AppConstants.BASE_URL, but there's no reason to
    // duplicate connection pooling/interceptors for one fire-and-forget POST.
    private val okHttpClient: OkHttpClient
) {
    // App-process-lifetime scope, same rationale as AdInitializer.initScope/OneSignalManager.initScope
    // - this is a Hilt singleton with no owner to cancel it against, and it only needs to run once.
    private val initScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var hasStarted = false

    private val json = Json { ignoreUnknownKeys = true }

    private object PreferencesKeys {
        val INSTALL_REFERRER_SENT = booleanPreferencesKey("install_referrer_sent")
    }

    @Serializable
    private data class InstallReferrerPayload(
        val referrer: String,
        val device_id: String,
        val app_version: String,
        val install_begin_timestamp: Long,
        val referrer_click_timestamp: Long
    )

    /**
     * Kicks off the install-referrer fetch+send on a background thread and returns immediately -
     * non-suspending, exactly like [AdInitializer.initialize]/[OneSignalManager.initialize], so
     * it's safe to call from anywhere without awaiting it (see call site doc comment).
     *
     * No-op if already attempted this process (in-memory [hasStarted] guard) or if
     * [PreferencesKeys.INSTALL_REFERRER_SENT] is already true (persisted guard - the real
     * "once ever per install" gate).
     */
    fun maybeSendInstallReferrer() {
        if (hasStarted) return
        hasStarted = true

        initScope.launch {
            try {
                val alreadySent = dataStore.data.first()[PreferencesKeys.INSTALL_REFERRER_SENT] == true
                if (alreadySent) {
                    Log.d(TAG, "maybeSendInstallReferrer: already sent on a prior launch, skipping")
                    return@launch
                }

                val details = fetchReferrerDetails()
                if (details == null) {
                    Log.d(TAG, "maybeSendInstallReferrer: no referrer details available, will retry next launch")
                    return@launch
                }

                val rawReferrer = details.installReferrer ?: ""
                // Parsed only for local diagnostic visibility (see log below) - the backend's own
                // PHP endpoint parses the raw referrer itself, and the exact request body below
                // sends that raw string regardless of whether parsing here found anything, so a
                // renamed/unexpected query param on the network side never loses data.
                val clickId = extractClickId(rawReferrer)
                Log.d(TAG, "maybeSendInstallReferrer: parsed clickId=$clickId from referrer")

                val payload = InstallReferrerPayload(
                    referrer = rawReferrer,
                    device_id = Settings.Secure.getString(context.contentResolver, Settings.Secure.ANDROID_ID)
                        ?: "",
                    app_version = getAppVersionName(),
                    install_begin_timestamp = details.installBeginTimestampSeconds,
                    referrer_click_timestamp = details.referrerClickTimestampSeconds
                )

                if (postToBackend(payload)) {
                    dataStore.edit { it[PreferencesKeys.INSTALL_REFERRER_SENT] = true }
                    Log.d(TAG, "maybeSendInstallReferrer: sent successfully, flag persisted")
                } else {
                    Log.d(TAG, "maybeSendInstallReferrer: send failed, flag left unset for retry")
                }
            } catch (e: Exception) {
                // Catch-all: Play Services unavailable, DataStore I/O, JSON encoding, whatever -
                // this must never propagate and never block/crash the splash flow.
                Log.w(TAG, "maybeSendInstallReferrer: failed silently", e)
            }
        }
    }

    /**
     * Wraps [InstallReferrerClient]'s callback-based connection in a suspend call. Returns null
     * (never throws) on any non-OK response code, Play Services being unavailable, or the
     * client's own callback throwing - callers treat null as "try again next launch".
     */
    private suspend fun fetchReferrerDetails(): ReferrerDetails? = suspendCancellableCoroutine { cont ->
        val client = InstallReferrerClient.newBuilder(context).build()
        cont.invokeOnCancellation { runCatching { client.endConnection() } }

        client.startConnection(object : InstallReferrerStateListener {
            override fun onInstallReferrerSetupFinished(responseCode: Int) {
                try {
                    val result = if (responseCode == InstallReferrerResponse.OK) {
                        client.installReferrer
                    } else {
                        Log.d(TAG, "fetchReferrerDetails: non-OK responseCode=$responseCode")
                        null
                    }
                    if (cont.isActive) cont.resume(result)
                } catch (e: Exception) {
                    if (cont.isActive) cont.resume(null)
                } finally {
                    runCatching { client.endConnection() }
                }
            }

            override fun onInstallReferrerServiceDisconnected() {
                // Not retried within this attempt - maybeSendInstallReferrer's outer flag guard
                // means the whole fetch simply runs again on the next app open.
            }
        })
    }

    /** click_id, falling back to fbclid then gclid - diagnostic-only, see [InstallReferrerPayload]. */
    private fun extractClickId(referrer: String): String? {
        val params = runCatching {
            referrer.split("&")
                .mapNotNull { pair ->
                    val idx = pair.indexOf('=')
                    if (idx <= 0) return@mapNotNull null
                    pair.substring(0, idx) to URLDecoder.decode(pair.substring(idx + 1), "UTF-8")
                }
                .toMap()
        }.getOrDefault(emptyMap())

        return params["click_id"] ?: params["fbclid"] ?: params["gclid"]
    }

    private fun getAppVersionName(): String = try {
        val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.packageManager.getPackageInfo(context.packageName, PackageManager.PackageInfoFlags.of(0))
        } else {
            @Suppress("DEPRECATION")
            context.packageManager.getPackageInfo(context.packageName, 0)
        }
        packageInfo.versionName ?: UNKNOWN_VERSION
    } catch (e: Exception) {
        UNKNOWN_VERSION
    }

    /** Blocking OkHttp call - fine here, this whole coroutine already runs on [Dispatchers.IO]. */
    private fun postToBackend(payload: InstallReferrerPayload): Boolean = try {
        val body = json.encodeToString(payload).toRequestBody("application/json".toMediaType())
        val request = Request.Builder().url(ENDPOINT_URL).post(body).build()
        okHttpClient.newCall(request).execute().use { it.isSuccessful }
    } catch (e: Exception) {
        Log.w(TAG, "postToBackend: failed", e)
        false
    }

    private companion object {
        const val TAG = "InstallReferrer"
        const val ENDPOINT_URL = "https://installreferral.sixfigurefinance.com/"
        const val UNKNOWN_VERSION = "unknown"
    }
}
