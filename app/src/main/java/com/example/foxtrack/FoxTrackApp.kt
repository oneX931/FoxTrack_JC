package com.example.foxtrack

import android.app.Application
import com.example.foxtrack.data.AppContainer

class FoxTrackApp : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
