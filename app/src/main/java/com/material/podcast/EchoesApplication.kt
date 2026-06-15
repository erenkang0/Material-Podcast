package com.material.podcast

import android.app.Application
import com.material.podcast.data.repository.PodcastRepository
import com.material.podcast.data.store.LibraryStore
import com.material.podcast.media.DownloadManager

class EchoesApplication : Application() {

    val repository: PodcastRepository by lazy { PodcastRepository() }
    val downloadManager: DownloadManager by lazy { DownloadManager(this) }

    companion object {
        lateinit var instance: EchoesApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
        LibraryStore.init(this)
    }
}
