package ir.rosheno.launcher

import android.app.Application
import android.content.Context
import android.content.ContextWrapper
import java.io.File
import java.util.UUID

/** Separate preferences/files: reset/corruption tests must never touch installation data. */
internal class IsolatedTestApp(base: Context) : Application() {
    private val prefix = "feature-test-${UUID.randomUUID()}"
    val directory = File(base.cacheDir, prefix).apply { mkdirs() }
    private val preferences = mutableSetOf<String>()
    init {
        attachBaseContext(object : ContextWrapper(base) {
            override fun getFilesDir(): File = directory
            override fun getSharedPreferences(name: String, mode: Int): android.content.SharedPreferences {
                val isolated = "$prefix-$name"
                preferences += isolated
                return base.getSharedPreferences(isolated, mode)
            }
        })
    }
    fun cleanup() {
        preferences.forEach { baseContext.deleteSharedPreferences(it) }
        directory.deleteRecursively()
    }
}
