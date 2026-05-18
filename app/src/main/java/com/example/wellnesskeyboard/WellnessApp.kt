package com.example.wellnesskeyboard

import android.app.Application
import com.example.wellnesskeyboard.workers.NightlyInferenceScheduler

class WellnessApp : Application() {
    override fun onCreate() {
        super.onCreate()
        NightlyInferenceScheduler.schedule(this)
    }
}