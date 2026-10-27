package ir.rosheno.launcher.domain.model

data class LauncherConfig(
    val contacts: List<FamilyContact> = emptyList(),
    val favorites: List<String> = emptyList(),
    val helpContactId: String? = null,
    val size: Int = 0,
    val setupComplete: Boolean = false,
)
