package ir.rosheno.launcher.domain.model

data class LaunchableApp(val label: String, val key: String) {
    val packageName: String get() = key.substringBefore('/')
}
