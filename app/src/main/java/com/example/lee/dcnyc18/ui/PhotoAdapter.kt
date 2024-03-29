package com.example.lee.dcnyc18.ui

import android.support.v7.util.DiffUtil
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ViewGroup
import com.example.lee.dcnyc18.R
import com.example.lee.dcnyc18.util.DCNYDispatchers
import com.example.lee.dcnyc18.models.Photo
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

class PhotoAdapter(
        private val photoCellIntentHandler: PhotoCellIntentHandler,
        private val dispatchers: DCNYDispatchers = DCNYDispatchers()
): RecyclerView.Adapter<PhotoViewHolder>(), CoroutineScope {
    private lateinit var job: Job
    override val coroutineContext: CoroutineContext
        get() = dispatchers.main + job

    private var data: List<Photo> = listOf()

    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        job = Job()
    }

    fun updateData(updatedPhotos: List<Photo>) {
        launch {
            createDiff(this@PhotoAdapter.data, updatedPhotos)
        }
    }

    private suspend fun createDiff(currentData: List<Photo>, updatedData: List<Photo>) {
        val diffCallback = PhotoModelDiffCallback(currentData, updatedData)
        val diffResult = DiffUtil.calculateDiff(diffCallback)

        withContext(dispatchers.main) {
            data = updatedData
            diffResult.dispatchUpdatesTo(this@PhotoAdapter)
        }
    }

    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        job.cancel()
        super.onDetachedFromRecyclerView(recyclerView)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val photoView = LayoutInflater.from(parent.context).inflate(R.layout.view_photo_cell, parent, false)
        return PhotoViewHolder(photoView)
    }

    override fun getItemCount(): Int = data.size

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = data[position]
        holder.bindData(photo, photoCellIntentHandler)
    }

    companion object {
        private const val TAG = "YELLOW"
    }
}


