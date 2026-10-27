package ir.rosheno.launcher.presentation.common

import android.content.Context
import androidx.annotation.StringRes

/** UI messages are resolved by the View so retained ViewModels do not cache a locale. */
sealed interface UiText {
    data class Resource(@param:StringRes val id: Int, val arguments: List<Any> = emptyList()) : UiText
    data class Dynamic(val value: String) : UiText
}

fun UiText.resolve(context: Context): String = when (this) {
    is UiText.Resource -> if (arguments.isEmpty()) context.getString(id) else context.getString(id, *arguments.toTypedArray())
    is UiText.Dynamic -> value
}
