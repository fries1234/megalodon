package org.joinmastodon.android

import android.annotation.SuppressLint
import android.app.Application
import android.content.Context
import me.grishka.appkit.imageloader.ImageCache
import me.grishka.appkit.utils.NetworkUtils
import me.grishka.appkit.utils.V
import org.joinmastodon.android.api.PushSubscriptionManager

class MastodonApp : Application() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        @JvmField // tell kotlin to expose context as a static field to the JVM
        var context: Context? = null
    }

    override fun onCreate() {
        super.onCreate()
        context = applicationContext
        // tell AppKit to use context as the application context
        V.setApplicationContext(context)
        // create an ImageCache object
        val params = ImageCache.Parameters()
        // this is 100 MiB
        params.diskCacheSize = 100 * 1024 * 1024
        // set the max memory cache size to the 32bit integer limit
        params.maxMemoryCacheSize = Int.MAX_VALUE
        // give the ImageCache the params object with our values
        ImageCache.setParams(params)
        NetworkUtils.setUserAgent("Megalodon/${BuildConfig.VERSION_NAME}")

        // attempt to register Firebase Cloud Messaging
        PushSubscriptionManager.tryRegisterFCM()
        // load the user's preferences
        GlobalUserPreferences.load()
    }
}