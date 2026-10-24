package com.example

import android.app.Application
import android.content.Context
import android.os.Build
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache

/**
 * Custom Application class that optimizes memory management, image caching,
 * and crash prevention for Android 10 (API 29) and devices with 4GB RAM.
 */
class AuditApplication : Application(), ImageLoaderFactory {

    companion object {
        private const val TAG = "AuditApplication"

        fun clearAppMemoryCaches(context: Context) {
            try {
                coil.Coil.imageLoader(context).memoryCache?.clear()
                System.gc()
            } catch (t: Throwable) {
                Log.w(TAG, "Failed clearing memory caches: ${t.message}")
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        setupGlobalExceptionHandler()
    }

    /**
     * Configures Coil ImageLoader with strict memory limits to avoid OutOfMemoryError
     * when scrolling through many vehicle and KPI photos on 4GB RAM devices.
     */
    override fun newImageLoader(): ImageLoader {
        return ImageLoader.Builder(this)
            .memoryCache {
                MemoryCache.Builder(this)
                    // Limit Coil memory cache to 10% of available heap to leave 90%
                    // for camera, canvas marking, and database operations.
                    .maxSizePercent(0.10)
                    .build()
            }
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir.resolve("image_cache"))
                    .maxSizeBytes(60L * 1024 * 1024) // 60 MB disk cache limit
                    .build()
            }
            .respectCacheHeaders(false)
            .allowRgb565(true) // Conserves 50% bitmap memory when transparency is not required
            .allowHardware(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
            .build()
    }

    /**
     * Handled when the OS signals memory pressure (e.g. when camera app is in foreground).
     * Frees Coil cache and hints GC to prevent Low Memory Killer (LMK) from killing this process.
     */
    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
            // When app goes to background (UI hidden e.g. Camera opened) or OS runs low on memory
            if (level >= TRIM_MEMORY_UI_HIDDEN || level >= TRIM_MEMORY_RUNNING_LOW) {
                System.gc()
            }
        } catch (t: Throwable) {
            Log.w(TAG, "Error in onTrimMemory: ${t.message}")
        }
    }

    override fun onLowMemory() {
        super.onLowMemory()
        try {
            coil.Coil.imageLoader(this).memoryCache?.clear()
            System.gc()
        } catch (t: Throwable) {
            Log.w(TAG, "Error in onLowMemory: ${t.message}")
        }
    }

    private fun setupGlobalExceptionHandler() {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Log.e(TAG, "Uncaught exception on thread ${thread.name}: ${throwable.message}", throwable)
            if (throwable is OutOfMemoryError) {
                try {
                    coil.Coil.imageLoader(this).memoryCache?.clear()
                    System.gc()
                } catch (_: Throwable) {}
            }
            defaultHandler?.uncaughtException(thread, throwable)
        }
    }
}
