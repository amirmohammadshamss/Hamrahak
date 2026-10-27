package ir.rosheno.launcher.domain.usecase

import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository

class SaveLauncherConfig(private val config: ConfigRepository, private val photos: ContactPhotoRepository) {
    operator fun invoke(previous: LauncherConfig, updated: LauncherConfig) {
        config.save(updated)
        val obsolete = previous.contacts.mapNotNull { it.photo }.toSet() -
            updated.contacts.mapNotNull { it.photo }.toSet()
        // The configuration is already durable; a cleanup failure must not revert UI state.
        runCatching { obsolete.forEach(photos::deleteCommitted) }
    }
}
