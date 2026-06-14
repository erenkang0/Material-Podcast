package com.material.podcast

import android.app.Application
import com.material.podcast.data.repository.PodcastRepository

class EchoesApplication : Application() {

    val repository: PodcastRepository by lazy { PodcastRepository() }

    companion object {
        lateinit var instance: EchoesApplication
            private set
    }

    override fun onCreate() {
        super.onCreate()
        instance = this
    }
}
