package com.luminine.app

import androidx.compose.ui.window.ComposeUIViewController
import com.luminine.app.di.LuminineDependencies

fun MainViewController() = ComposeUIViewController {
    // Install DataStore-backed repositories before the UI reads them. Idempotent: safe if the
    // view controller is recreated (a second DataStore on the same path would otherwise throw).
    LuminineDependencies.installDataStore()
    App()
}
