package ir.rosheno.launcher.presentation.dialer

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import ir.rosheno.launcher.presentation.common.CallConfirmationPolicy
import ir.rosheno.launcher.presentation.common.CallConfirmationState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

data class DialerUiState(val call: CallConfirmationState = CallConfirmationState(), val chooseFamily: Boolean = false) {
    val confirming get() = call.confirming
    val requestingNumber get() = call.awaitingPermission
    val permissionDenied get() = call.permissionDenied
}

class DialerViewModel @JvmOverloads constructor(
    private val saved: SavedStateHandle,
    private val callPolicy: CallConfirmationPolicy = CallConfirmationPolicy(),
) : ViewModel() {
    private val mutable = MutableStateFlow(DialerUiState(
        CallConfirmationState(saved["confirming"], saved["requesting"], saved["denied"] ?: false),
        saved["family"] ?: false,
    ))
    val state = mutable.asStateFlow()
    private fun change(transform: (DialerUiState) -> DialerUiState) {
        val next = transform(mutable.value)
        mutable.value = next
        saved["confirming"] = next.confirming; saved["requesting"] = next.requestingNumber
        saved["denied"] = next.permissionDenied; saved["family"] = next.chooseFamily
    }
    fun requestCall(number: String, canCall: Boolean): Boolean {
        val call = callPolicy.request(number, canCall) ?: return false
        change { it.copy(call = call) }
        return call.needsPermission
    }
    fun permissionResult(granted: Boolean) = change { it.copy(call = callPolicy.permissionResult(it.call, granted)) }
    fun dismissConfirmation() = change { it.copy(call = it.call.dismiss()) }
    fun consumeConfirmation(): String? = state.value.confirming.also { dismissConfirmation() }
    fun chooseFamily(show: Boolean) = change { it.copy(chooseFamily = show) }
}
