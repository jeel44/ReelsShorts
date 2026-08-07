package reelsdrama.freedrama.videosdrama.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import reelsdrama.freedrama.videosdrama.data.local.VideoDao
import reelsdrama.freedrama.videosdrama.data.mapper.toDomain
import reelsdrama.freedrama.videosdrama.domain.model.Video
import reelsdrama.freedrama.videosdrama.domain.repository.VideoRepository
import javax.inject.Inject

class DefaultVideoRepository @Inject constructor(
    private val videoDao: VideoDao,
) : VideoRepository {
    override fun observeVideos(): Flow<List<Video>> = videoDao.observeVideos().map { videos -> videos.map { it.toDomain() } }
}
