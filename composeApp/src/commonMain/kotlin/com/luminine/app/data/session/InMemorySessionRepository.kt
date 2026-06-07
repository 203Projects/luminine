package com.luminine.app.data.session

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

// Suspend-correct, thread-safe in-memory session store. Also the compile-safe default used by
// LuminineDependencies before platform startup overrides it with a DataStore-backed repo.
class InMemorySessionRepository(initial: Session? = null) : SessionRepository {
    private val mutex = Mutex()
    private var current: Session? = initial

    override suspend fun save(session: Session) = mutex.withLock { current = session }
    override suspend fun load(): Session? = mutex.withLock { current }
    override suspend fun clear() = mutex.withLock { current = null }
}
