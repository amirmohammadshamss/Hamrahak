package ir.hamrahak.launcher

import android.app.Activity
import android.app.role.RoleManager
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.provider.Settings
import android.net.Uri

class PlatformActions(
    private val activity: Activity,
    private val report: (String) -> Unit,
    private val requestRole: (Intent) -> Unit,
) {
    fun open(intent: Intent) {
        try { activity.startActivity(intent) }
        catch (_: ActivityNotFoundException) { report("برنامه‌ای برای این کار پیدا نشد.") }
        catch (_: SecurityException) { report("باز کردن این برنامه ممکن نیست. تنظیمات گوشی را بررسی کنید.") }
    }
    fun chooseHome() {
        if (Build.VERSION.SDK_INT >= 29) {
            val manager = activity.getSystemService(RoleManager::class.java)
            if (manager.isRoleAvailable(RoleManager.ROLE_HOME)) {
                if (manager.isRoleHeld(RoleManager.ROLE_HOME)) {
                    report("همراهک اکنون صفحهٔ اصلی گوشی شماست.")
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
        val safe = normalizePhone(number)
        if (safe == null) report("شماره معتبر نیست. از خانواده بخواهید آن را اصلاح کنند.")
        else open(Intent(Intent.ACTION_DIAL, Uri.fromParts("tel", safe, null)))
    }
    fun dialer() = open(Intent(Intent.ACTION_DIAL))
    fun launch(key: String) {
        val component = ComponentName.unflattenFromString(key)
        if (component == null) report("این میان‌بر در دسترس نیست.")
        else open(Intent.makeMainActivity(component))
    }
    fun messages() = open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MESSAGING))
    fun photos() = open(Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_GALLERY))
    fun camera() = open(Intent("android.media.action.STILL_IMAGE_CAMERA"))
}
