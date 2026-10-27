package ir.rosheno.launcher.domain.repository

import ir.rosheno.launcher.domain.model.LauncherConfig

interface ConfigRepository {
    fun read(): LauncherConfig
    fun save(config: LauncherConfig)
}
