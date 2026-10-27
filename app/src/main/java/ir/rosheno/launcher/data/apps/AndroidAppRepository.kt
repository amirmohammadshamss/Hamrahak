package ir.rosheno.launcher.data.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import ir.rosheno.launcher.domain.model.LaunchableApp
import ir.rosheno.launcher.domain.repository.AppRepository

class AndroidAppRepository(private val context: Context) : AppRepository {
    override fun load(): List<LaunchableApp> {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        return context.packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != context.packageName }
            .map { LaunchableApp(it.loadLabel(context.packageManager).toString(),
                ComponentName(it.activityInfo.packageName, it.activityInfo.name).flattenToString()) }
            .distinctBy { it.key }
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
    }
}
