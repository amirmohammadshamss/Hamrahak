package ir.rosheno.launcher

import android.content.ComponentName
import java.util.UUID

data class FamilyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val photo: String? = null,
)

data class LauncherConfig(
    val contacts: List<FamilyContact> = emptyList(),
    val favorites: List<String> = emptyList(),
    val helpContactId: String? = null,
    val size: Int = 0,
    val setupComplete: Boolean = false,
)

data class LaunchableApp(val label: String, val component: ComponentName) {
    val key: String get() = component.flattenToString()
}

/** Accept a single dialable number, never URI commands, extensions, or USSD codes. */
fun normalizePhone(input: String, minimumDigits: Int = 3): String? {
    require(minimumDigits in 1..15)
    val normalized = buildString {
        input.trim().forEach { ch ->
            when {
                ch in '۰'..'۹' -> append('0' + (ch - '۰'))
                ch in '٠'..'٩' -> append('0' + (ch - '٠'))
                ch in '0'..'9' || ch == '+' -> append(ch)
                ch == ' ' || ch == '-' || ch == '(' || ch == ')' -> Unit
                else -> return null
            }
        }
    }
    return normalized.takeIf { it.matches(Regex("\\+?[0-9]{$minimumDigits,15}")) }
}

fun persianDigits(text: String): String = text.map {
    if (it in '0'..'9') '۰' + (it - '0') else it
}.joinToString("")
