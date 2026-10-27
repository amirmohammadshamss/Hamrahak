package ir.rosheno.launcher.domain.usecase

import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository

class ResetLauncher(private val config: ConfigRepository, private val photos: ContactPhotoRepository) {
    operator fun invoke() {
        config.save(LauncherConfig())
        photos.clear()
    }
}
