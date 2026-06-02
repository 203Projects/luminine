package com.luminine.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.luminine.app.platform.LumininePersistence

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Seed the Context holder + install DataStore-backed repositories before the UI reads them.
        LumininePersistence.init(this)
        setContent {
            App()
        }
    }
}
