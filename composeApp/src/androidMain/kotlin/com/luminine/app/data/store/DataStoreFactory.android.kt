package com.luminine.app.data.store

import com.luminine.app.platform.AndroidAppContext

actual fun dataStorePath(fileName: String): String {
    val context = AndroidAppContext.require()
    // filesDir is app-private internal storage: survives restart, cleared on uninstall.
    return context.filesDir.resolve(fileName).absolutePath
}
