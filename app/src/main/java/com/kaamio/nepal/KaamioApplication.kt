package com.kaamio.nepal

import android.app.Application
import android.util.Log
import coil.ImageLoader
import coil.ImageLoaderFactory
import coil.disk.DiskCache
import coil.memory.MemoryCache
import coil.request.CachePolicy
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class KaamioApplication : Application(), ImageLoaderFactory {

    override fun onCreate() {
        super.onCreate()
        
        // Manual initialization fallback for environments where google-services.json 
        // initialization is not automatically performed by the plugin. The project
        // config now comes from BuildConfig (env / Secrets plugin), never from
        // hardcoded literals in source.
        if (FirebaseApp.getApps(this).isEmpty()) {
            val apiKey = BuildConfig.FIREBASE_API_KEY
            val appId = BuildConfig.FIREBASE_APP_ID
            val projectId = BuildConfig.FIREBASE_PROJECT_ID
            if (apiKey.isNotBlank() && appId.isNotBlank() && projectId.isNotBlank()) {
                val options = FirebaseOptions.Builder()
                    .setApiKey(apiKey)
                    .setApplicationId(appId)
                    .setProjectId(projectId)
                    .build()
                FirebaseApp.initializeApp(this, options)
            } else {
                Log.w("KaamioApplication", "Firebase config missing: define FIREBASE_API_KEY / FIREBASE_APP_ID / FIREBASE_PROJECT_ID or provide google-services.json")
            }
        }
    }

    override fun newImageLoader(): ImageLoader {
        val cacheDir = cacheDir.resolve("coil_cache")
        return ImageLoader.Builder(this)
            .memoryCachePolicy(CachePolicy.ENABLED)
            .memoryCache {
                MemoryCache.Builder(this)
                    .maxSizePercent(0.20)
                    .build()
            }
            .diskCachePolicy(CachePolicy.ENABLED)
            .diskCache {
                DiskCache.Builder()
                    .directory(cacheDir)
                    .maxSizeBytes(50 * 1024 * 1024)
                    .build()
            }
            .crossfade(true)
            .bitmapConfig(android.graphics.Bitmap.Config.ARGB_8888)
            .build()
    }
}
