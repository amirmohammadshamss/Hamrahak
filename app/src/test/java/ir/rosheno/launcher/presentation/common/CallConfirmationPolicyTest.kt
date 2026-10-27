package ir.rosheno.launcher.presentation.common

import ir.rosheno.launcher.domain.phone.normalizePhone
import org.junit.Assert.*
import org.junit.Test

class CallConfirmationPolicyTest {
    private val policy = CallConfirmationPolicy(
        normalize = { normalizePhone(it, minimumDigits = 1) },
        isEmergency = { it == "112" },
    )

    @Test fun deniedPermissionRetainsNormalizedSnapshotWithoutConfirming() {
        val requested = requireNotNull(policy.request("۰۹۱۲۱۲۳۴۵۶۷", canCall = false))
        assertTrue(requested.needsPermission)
        assertEquals("09121234567", requested.awaitingPermission)
        assertNull(requested.confirming)
        val denied = policy.permissionResult(requested, granted = false)
        assertTrue(denied.permissionDenied)
        assertNull(denied.confirming); assertNull(denied.awaitingPermission)
    }

    @Test fun permissionGrantOpensConfirmationAndCannotBeReplayed() {
        val requested = requireNotNull(policy.request("09121234567", canCall = false))
        val confirmed = policy.permissionResult(requested, granted = true)
        assertEquals("09121234567", confirmed.confirming)
        assertFalse(confirmed.needsPermission)
        assertNull(confirmed.dismiss().confirming)
        assertNull(policy.permissionResult(confirmed.dismiss(), granted = true).confirming)
    }

    @Test fun emergencyAndAlreadyGrantedNumbersStillRequireConfirmation() {
        val emergency = requireNotNull(policy.request("۱۱۲", canCall = false))
        assertFalse(emergency.needsPermission)
        assertEquals("112", emergency.confirming)
        val allowed = requireNotNull(policy.request("09121234567", canCall = true))
        assertFalse(allowed.needsPermission)
        assertEquals("09121234567", allowed.confirming)
    }

    @Test fun unsafeNumbersNeverProducePendingActions() {
        listOf("*123#", "tel:09121234567", "0912;123", "").forEach {
            assertNull(policy.request(it, canCall = true))
            assertNull(policy.request(it, canCall = false))
        }
    }
}
