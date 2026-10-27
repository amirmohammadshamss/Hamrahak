package ir.rosheno.launcher.presentation.launcher

import ir.rosheno.launcher.domain.model.LaunchableApp
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.presentation.common.UiText

data class LauncherState(
    val config: LauncherConfig = LauncherConfig(),
    val apps: List<LaunchableApp> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val message: UiText? = null,
    val savingContact: Boolean = false,
    val contactSaveToken: String? = null,
)
