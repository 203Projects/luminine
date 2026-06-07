package com.luminine.app.platform

import android.content.Context

/**
 * Holds the application Context for Android-only persistence wiring. DataStore needs a Context,
 * but the no-arg App()/MainViewController() entrypoints do not carry one — MainActivity seeds this
 * holder before persistence is used. Android-only (androidMain): no commonMain coupling to Context.
 */
object AndroidAppContext {
    @Volatile private var appContext: Context? = null

    fun init(context: Context) {
        appContext = context.applicationContext // applicationContext avoids leaking the Activity.
    }

    fun require(): Context = appContext
        ?: error("AndroidAppContext.init(context) must be called before using DataStore (call LumininePersistence.init from MainActivity).")
}
