package com.ghostbot.trading

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Ghost Bot Application class
 * Initializes Hilt dependency injection and logging
 */
@HiltAndroidApp
class GhostBotApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        initializeTimber()
    }
    
    private fun initializeTimber() {
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        } else {
            // In production, you might want to use a custom tree for crash reporting
            // Timber.plant(CrashReportingTree())
        }
        
        Timber.d("Ghost Bot Application initialized")
    }
}