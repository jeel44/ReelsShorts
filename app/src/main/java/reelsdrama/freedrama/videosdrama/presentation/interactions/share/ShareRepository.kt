package reelsdrama.freedrama.videosdrama.presentation.interactions.share

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val packageManager = context.packageManager

    private val supportedApps = listOf(
        ShareApp("WhatsApp", "com.whatsapp"),
        ShareApp("Telegram", "org.telegram.messenger"),
        ShareApp("Instagram", "com.instagram.android"),
        ShareApp("Facebook", "com.facebook.katana"),
        ShareApp("X (Twitter)", "com.twitter.android"),
        ShareApp("Messenger", "com.facebook.orca"),
        ShareApp("Gmail", "com.google.android.gm"),
        ShareApp("Messages", "com.google.android.apps.messaging")
    )

    fun getInstalledShareApps(): List<ShareApp> {
        return supportedApps.filter { isAppInstalled(it.packageName) }
    }

    private fun isAppInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: PackageManager.NameNotFoundException) {
            false
        }
    }

    fun shareToApp(packageName: String, text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
            setPackage(packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        if (intent.resolveActivity(packageManager) != null) {
            context.startActivity(intent)
        }
    }

    fun shareMore(text: String) {
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, text)
        }
        val chooser = Intent.createChooser(intent, "Share video via").apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }
}
