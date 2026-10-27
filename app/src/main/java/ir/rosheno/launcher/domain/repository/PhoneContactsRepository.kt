package ir.rosheno.launcher.domain.repository

import ir.rosheno.launcher.domain.model.PhoneContact

interface PhoneContactsRepository {
    fun load(): List<PhoneContact>
    fun create(name: String, number: String)
    /** Releases the provider observer when the screen stops. */
    fun observe(onChange: () -> Unit): () -> Unit = {}
}
