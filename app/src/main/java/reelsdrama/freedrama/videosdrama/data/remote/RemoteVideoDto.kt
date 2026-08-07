package reelsdrama.freedrama.videosdrama.data.remote

import kotlinx.serialization.Serializable

@Serializable
data class RemoteVideoDto(
    val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String,
)
