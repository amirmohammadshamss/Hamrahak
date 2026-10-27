package ir.rosheno.launcher.presentation.common

import ir.rosheno.launcher.platform.normalizeDialNumber
import ir.rosheno.launcher.platform.usesEmergencyDialer

data class CallConfirmationState(
    val confirming: String? = null,
    val awaitingPermission: String? = null,
    val permissionDenied: Boolean = false,
) {
    val needsPermission: Boolean get() = awaitingPermission != null
    fun dismiss() = copy(confirming = null)
}

/** Shared by contact calling and the manual dialer. Permission never places a call. */
class CallConfirmationPolicy(
    private val normalize: (String) -> String? = ::normalizeDialNumber,
    private val isEmergency: (String) -> Boolean = ::usesEmergencyDialer,
) {
    fun request(number: String, canCall: Boolean): CallConfirmationState? {
        val safe = normalize(number) ?: return null
        return if (!isEmergency(safe) && !canCall) CallConfirmationState(awaitingPermission = safe)
        else CallConfirmationState(confirming = safe)
    }

    fun permissionResult(state: CallConfirmationState, granted: Boolean) = state.copy(
        awaitingPermission = null,
        confirming = if (granted) state.awaitingPermission else null,
        permissionDenied = !granted,
    )
}
