package com.example.lee.dcnyc18.db

import com.example.lee.dcnyc18.util.DCNYDispatchers
import com.example.lee.dcnyc18.models.Photo
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.reactive.openSubscription
import kotlinx.coroutines.withContext

class PhotoDataSource(
        private val photoDao: PhotoDao,
        private val dispatchers: DCNYDispatchers
) {
    fun getAllPhotos(): ReceiveChannel<List<Photo>> {
        return photoDao.all.openSubscription()
    }

    suspend fun insertIfNotPresent(photos: List<Photo>) {
        withContext(dispatchers.db) {
            photoDao.insertAll(photos)
        }
    }

    suspend fun persistLikeStatus(modelId: String, likeStatus: Boolean) {
        withContext(dispatchers.db) {
            photoDao.updateLikeStatus(modelId, likeStatus)
        }
    }
}