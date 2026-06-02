package com.luminine.app.data.session

// Single-session model (one logged-in user). load() returns null when no session is persisted
// (logged out / first run).
interface SessionRepository {
    suspend fun save(session: Session)
    suspend fun load(): Session?
    suspend fun clear()
}
