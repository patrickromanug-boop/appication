package com.example

import android.app.ActivityManager
import android.app.Application
import android.content.Context
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import coil.util.DebugLogger

class MainApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        com.example.util.NotificationHelper.createNotificationChannel(this)
        com.example.util.JobAlertWorker.schedulePeriodic(this)
        com.example.util.JobAlertWorker.checkNowWhenOnline(this)
    }

    override fun newImageLoader(): ImageLoader {
        val activityManager = getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val isLowRamDevice = activityManager.isLowRamDevice

        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // On low RAM devices use 10% of available memory, on regular 20%
                    .maxSizePercent(if (isLowRamDevice) 0.10 else 0.20)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    // Limit disk cache size to 25MB to prevent excessive storage usage
                    .maxSizeBytes(25 * 1024 * 1024)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .allowHardware(!isLowRamDevice) // Disable hardware bitmaps on low RAM devices to avoid GPU memory pressure
            .allowRgb565(isLowRamDevice) // Use RGB_565 (2 bytes/pixel vs 4 bytes/pixel for ARGB_8888) on low RAM
            .crossfade(!isLowRamDevice) // Skip heavy crossfade animations on low RAM devices
            .build()
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        // Trim memory when system requests it
        if (level >= TRIM_MEMORY_MODERATE) {
            // Memory is low, Coil handles memoryCache cleanup automatically or via System GC
            System.gc()
        }
    }
}
