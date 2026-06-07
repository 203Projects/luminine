package com.luminine.app.data.store

import kotlinx.cinterop.ExperimentalForeignApi
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSURL
import platform.Foundation.NSUserDomainMask

@OptIn(ExperimentalForeignApi::class)
actual fun dataStorePath(fileName: String): String {
    // NSDocumentDirectory is per-app, backed up, survives restart. No Context concept on iOS.
    val documents: NSURL? = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    val dir = requireNotNull(documents?.path) { "Could not resolve iOS Documents directory" }
    return "$dir/$fileName"
}
