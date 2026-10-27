package ir.rosheno.launcher.data.config

import android.content.Context
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.repository.ConfigRepository
import org.json.JSONArray
import org.json.JSONObject

/** Versioned, small prototype store. Production Room/DataStore migration is tracked in docs. */
class ConfigStore(context: Context) : ConfigRepository {
    private val prefs = context.getSharedPreferences("launcher-prototype", Context.MODE_PRIVATE)

    override fun read(): LauncherConfig {
        val raw = prefs.getString("config", null) ?: return LauncherConfig()
        // Do not silently overwrite damaged data: let the UI offer explicit reset.
        val json = JSONObject(raw)
        require(json.optInt("version") == 1) { "Unsupported configuration" }
        val contactArray = json.getJSONArray("contacts")
        val contacts = (0 until contactArray.length()).map { i ->
            val c = contactArray.getJSONObject(i)
            FamilyContact(c.getString("id"), c.getString("name"), c.getString("number"),
                c.optString("photo").takeIf { it.isNotBlank() })
        }
        val appArray = json.getJSONArray("favorites")
        return LauncherConfig(
            contacts = contacts.take(9),
            favorites = (0 until appArray.length()).map { appArray.getString(it) }.distinct(),
            helpContactId = json.optString("help").takeIf { it.isNotBlank() },
            size = json.optInt("size").coerceIn(0, 2),
            setupComplete = json.optBoolean("setup"),
        )
    }

    // Keep the platform commit result: the KTX edit helper hides disk-write failure.
    @android.annotation.SuppressLint("UseKtx")
    override fun save(config: LauncherConfig) {
        val contacts = JSONArray().apply {
            config.contacts.forEach { c -> put(JSONObject().apply {
                put("id", c.id); put("name", c.name); put("number", c.number)
                put("photo", c.photo ?: "")
            }) }
        }
        val json = JSONObject().apply {
            put("version", 1); put("contacts", contacts)
            put("favorites", JSONArray(config.favorites)); put("help", config.helpContactId ?: "")
            put("size", config.size); put("setup", config.setupComplete)
        }
        check(prefs.edit().putString("config", json.toString()).commit())
    }
}
