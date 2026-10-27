package ir.rosheno.launcher.domain.usecase

import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import java.util.UUID

class SaveFamilyContact(private val config: ConfigRepository, private val photos: ContactPhotoRepository) {
    operator fun invoke(current: LauncherConfig, id: String?, name: String, number: String, photo: String?): LauncherConfig {
        val clean = requireNotNull(normalizePhone(number))
        require(name.isNotBlank())
        val previous = current.contacts.find { it.id == id }
        check(if (id == null) current.contacts.size < 9 else previous != null)
        val committed = photos.commit(photo)
        val contact = FamilyContact(id = id ?: UUID.randomUUID().toString(),
            name = name.trim(), number = clean, photo = committed)
        val updated = current.copy(contacts = if (id == null) current.contacts + contact
            else current.contacts.map { if (it.id == id) contact else it })
        try { config.save(updated) }
        catch (error: Exception) {
            if (committed != photo) photos.deleteCommitted(committed)
            throw error
        }
        // Cleanup cannot turn a successful configuration write into a failed save.
        runCatching {
            photos.discardDraft(photo)
            if (previous?.photo != committed) photos.deleteCommitted(previous?.photo)
        }
        return updated
    }
}
