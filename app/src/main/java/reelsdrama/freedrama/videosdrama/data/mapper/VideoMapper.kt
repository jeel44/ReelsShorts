package reelsdrama.freedrama.videosdrama.data.mapper

import reelsdrama.freedrama.videosdrama.data.local.VideoEntity
import reelsdrama.freedrama.videosdrama.domain.model.Video

fun VideoEntity.toDomain(): Video = Video(id = id, title = title, videoUrl = videoUrl, thumbnailUrl = thumbnailUrl)
