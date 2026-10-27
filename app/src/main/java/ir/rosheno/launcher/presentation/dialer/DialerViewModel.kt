package ir.rosheno.launcher.presentation.dialer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ir.rosheno.launcher.platform.normalizeDialNumber
import ir.rosheno.launcher.platform.usesEmergencyDialer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DialerUiState(val confirming: String? = null, val requestingNumber: String? = null,
    val permissionDenied: Boolean = false, val chooseFamily: Boolean = false)

class DialerViewModel(private val saved: SavedStateHandle) : ViewModel() {
    private val mutable = MutableStateFlow(DialerUiState(saved["confirming"], saved["requesting"],
        saved["denied"] ?: false, saved["family"] ?: false))
    val state = mutable.asStateFlow()
    private fun change(transform: (DialerUiState) -> DialerUiState) {
        val next = transform(mutable.value)
        mutable.value = next
        saved["confirming"] = next.confirming; saved["requesting"] = next.requestingNumber
        saved["denied"] = next.permissionDenied; saved["family"] = next.chooseFamily
    }
    fun requestCall(number: String, canCall: Boolean): Boolean {
        val safe = normalizeDialNumber(number) ?: return false
        val needsPermission = !usesEmergencyDialer(safe) && !canCall
        change { it.copy(confirming = if (needsPermission) null else safe,
            requestingNumber = if (needsPermission) safe else null, permissionDenied = false) }
        return needsPermission
    }
    fun permissionResult(granted: Boolean) = change { it.copy(requestingNumber = null,
        permissionDenied = !granted, confirming = if (granted) it.requestingNumber else null) }
    fun dismissConfirmation() = change { it.copy(confirming = null) }
    fun consumeConfirmation(): String? = state.value.confirming.also { dismissConfirmation() }
    fun chooseFamily(show: Boolean) = change { it.copy(chooseFamily = show) }
}
