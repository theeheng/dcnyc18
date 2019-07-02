package com.example.lee.dcnyc18.ui

import android.arch.lifecycle.LiveData
import android.arch.lifecycle.MutableLiveData
import android.arch.lifecycle.ViewModel
import com.example.lee.dcnyc18.db.PhotoDataSource
import com.example.lee.dcnyc18.models.Photo
import com.example.lee.dcnyc18.network.UnsplashService
import com.example.lee.dcnyc18.prefs.PrefsManager
import kotlinx.coroutines.*
import java.lang.Exception
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import com.example.lee.dcnyc18.util.DCNYDispatchers

class PhotoListViewModel @Inject constructor(
        private val photoDataSource: PhotoDataSource,
        private val unsplashService: UnsplashService,
        private val prefsManager: PrefsManager,
        private val dispatchers: DCNYDispatchers
): ViewModel(), ListIntentHandler, PhotoCellIntentHandler, CoroutineScope {
    private val job = Job()
    override val coroutineContext: CoroutineContext
        get() = dispatchers.main + job

    // TODO: this is flawed logic
    private var pageToFetch = prefsManager.retrieveNextApiPage()
        set(value) {
            field = value
            prefsManager.persistNextApiPage(value)
        }

    private lateinit var photos: MutableLiveData<List<Photo>>
    //private val disposables: CompositeDisposable = CompositeDisposable()

    fun getPhotosListData(): LiveData<List<Photo>> {
        if (!::photos.isInitialized) {
            photos = MutableLiveData()
            listenForDataFromDb()
        }
        return photos
    }

    override fun onCleared() {
        job.cancel()
        super.onCleared()
    }

    private fun updateModelState(modelId: String, newLikeStatus: Boolean) {
        launch {
            try {
                photoDataSource.persistLikeStatus(modelId, newLikeStatus)
                println("$TAG SUCCESS LEMUR! 🐒 ")
            }
            catch (e: Exception) {
                println("$TAG FAIL WHALE! 🐳 ")
            }

        }
    }

    private fun listenForDataFromDb() {
        launch {
            val channel = photoDataSource.getAllPhotos()
            for(updatedPhotos in channel) {
                if (updatedPhotos.isEmpty()) {
                    withContext(dispatchers.io) {
                        // No photos have been persisted, begin fetching from network
                        fetchNextPageOfPhotos()
                    }
                } else {
                    withContext(dispatchers.main) {
                        photos.postValue(updatedPhotos)
                    }
                }
            }
        }
    }

    private suspend fun fetchNextPageOfPhotos() {
        try{
            val curatedPhotos = unsplashService.getCuratedPhotos(pageToFetch++).await()
            photoDataSource.insertIfNotPresent(curatedPhotos)
            println("Successfully inserted into db")
        } catch (e: Exception) {
            //Handle the exception
            println("$TAG Error ${e.message}")
        }
    }

    /*private fun fetchNextPageOfPhotos() {
        val subsciption = unsplashService.getCuratedPhotos(pageToFetch = pageToFetch++)
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .flatMapCompletable {
                    photoDataSource.insertIfNotPresent(it)
                }
                .subscribe(
                        {
                            // Successfully inserted into db
                            println("Successfully inserted into db")
                        },
                        {
                            // TODO: handle the error
                            println("$TAG Error $it")
                        }
                )

        disposables.add(subsciption)
    }*/


    override fun onReachedEndOfData() {
        launch(dispatchers.io) {
            fetchNextPageOfPhotos()
        }
    }

    override fun handleHeartIconClicked(photo: Photo) {
        // Toggle like status on tap
        val newLikeStatus = !photo.likedByUser
        updateModelState(photo.id, newLikeStatus)
    }

    companion object {
        private const val TAG = "YELLOW"
    }
}

