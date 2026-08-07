package reelsdrama.freedrama.videosdrama.domain.repository

import kotlinx.coroutines.flow.Flow
import reelsdrama.freedrama.videosdrama.domain.model.Video

interface VideoRepository {
    fun observeVideos(): Flow<List<Video>>
}
