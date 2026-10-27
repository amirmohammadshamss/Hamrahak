package ir.rosheno.launcher.domain.phone

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

fun comparableSmsAddress(address: String): String {
    val normalized = normalizePhone(address) ?: return address.trim()
    return when {
        normalized.startsWith("+98") -> "0" + normalized.drop(3)
        normalized.startsWith("0098") -> "0" + normalized.drop(4)
        else -> normalized
    }
}
