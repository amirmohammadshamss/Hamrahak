package ir.rosheno.launcher.platform

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.phone.normalizePhone

class PlatformActions(
    private val activity: Activity,
    private val report: (String) -> Unit,
    private val requestRole: (Intent) -> Unit,
) {
    fun open(intent: Intent) {
        try { activity.startActivity(intent) }
        catch (_: ActivityNotFoundException) { report(activity.getString(R.string.error_no_matching_app)) }
        catch (_: SecurityException) { report(activity.getString(R.string.error_open_app)) }
    }
    fun chooseHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val manager = activity.getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    report(activity.getString(R.string.home_already_selected))
                // RequestRoleActivity needs a caller/result token to identify this package.
                } else requestRole(manager.createRequestRoleIntent(RoleManager.ROLE_HOME))
                return
            }
        }
        homeSettings()
    }
    fun isHome(): Boolean {
        if (Build.VERSION.SDK_INT >= 29) {
            val manager = activity.getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_HOME)) return manager.isRoleHeld(RoleManager.ROLE_HOME)
        }
        val info = activity.packageManager.resolveActivity(
            Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0)
        return info?.activityInfo?.packageName == activity.packageName
    }
    fun homeSettings() = open(Intent(Settings.ACTION_HOME_SETTINGS))
    fun dial(number: String) {
        val safe = normalizeDialNumber(number)
        if (safe == null) report(activity.getString(R.string.error_family_number_invalid))
        else open(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", safe, null)))
    }
    fun dialer() = open(Intent(Intent.ACTION_DIAL))
    fun startUserCall(number: String) {
        val intent = userCallIntent(number)
        if (intent == null) { report(activity.getString(R.string.error_enter_valid_number)); return }
        if (intent.action == Intent.ACTION_CALL && androidx.core.content.ContextCompat.checkSelfPermission(activity,
                android.Manifest.permission.CALL_PHONE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            report(activity.getString(R.string.error_call_permission_inactive))
            return
        }
        open(intent)
    }
    fun launch(key: String) {
        val component = ComponentName.unflattenFromString(key)
        if (component == null) report(activity.getString(R.string.error_shortcut_unavailable))
        else open(Intent.makeMainActivity(component))
    }
    fun messages() = open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
    fun photos() = open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY))
    fun camera() = open(Intent("android.media.action.STILL_IMAGE_CAMERA"))
}

@Suppress("DEPRECATION")
fun usesEmergencyDialer(number: String): Boolean {
    val safe = normalizePhone(number, minimumDigits = 1) ?: return false
    return safe in setOf("110", "112", "115", "125", "911") ||
        runCatching { android.telephony.PhoneNumberUtils.isEmergencyNumber(safe) }.getOrDefault(false)
}

fun userCallIntent(number: String): Intent? {
    val safe = normalizeDialNumber(number) ?: return null
    return Intent(if (usesEmergencyDialer(safe)) Intent.ACTION_DIAL else Intent.ACTION_CALL, Uri.fromParts("tel", safe, null))
}

fun normalizeDialNumber(number: String): String? = normalizePhone(number)
    ?: normalizePhone(number, minimumDigits = 1)?.takeIf { usesEmergencyDialer(it) }
