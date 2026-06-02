package com.luminine.app.platform

import android.content.Context
import com.luminine.app.di.LuminineDependencies

/**
 * Android persistence bootstrap. Seeds the Context holder, then installs DataStore-backed repos.
 * Ordering matters: Context must be seeded BEFORE installDataStore() resolves dataStorePath -> filesDir.
 * MainActivity calls this once in onCreate, before setContent.
 */
object LumininePersistence {
    fun init(context: Context) {
        AndroidAppContext.init(context)
        LuminineDependencies.installDataStore()
    }
}
