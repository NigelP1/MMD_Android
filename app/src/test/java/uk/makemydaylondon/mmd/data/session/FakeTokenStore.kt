package uk.makemydaylondon.mmd.data.session

class FakeTokenStore(var saved: StoredSession? = null) : TokenStore {
    override suspend fun read(): StoredSession? = saved
    override suspend fun write(session: StoredSession) { saved = session }
    override suspend fun clear() { saved = null }
}
