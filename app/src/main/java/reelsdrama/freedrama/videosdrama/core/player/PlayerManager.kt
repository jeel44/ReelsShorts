package reelsdrama.freedrama.videosdrama.core.player

import android.content.Context
import androidx.media3.exoplayer.ExoPlayer
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

class PlayerManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    fun createPlayer(): ExoPlayer = ExoPlayer.Builder(context).build()
}
