package com.luminine.app.data.store

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import okio.Path.Companion.toPath

/**
 * Platform-resolved absolute file path for the DataStore preferences file (ends with .preferences_pb).
 * Returns a String (not okio.Path) so the expect/actual surface carries zero okio/androidx types.
 */
expect fun dataStorePath(fileName: String): String

const val LUMININE_PREFS_FILE = "luminine.preferences_pb"

// One shared DataStore<Preferences> backs BOTH repositories (session + survey) via distinct keys.
// DataStore throws if two instances point at the same file, so this is created exactly once
// (see LuminineDependencies.installDataStore, which is idempotent).
fun createLuminineDataStore(
    fileName: String = LUMININE_PREFS_FILE,
): DataStore<Preferences> = PreferenceDataStoreFactory.createWithPath(
    produceFile = { dataStorePath(fileName).toPath() },
)
