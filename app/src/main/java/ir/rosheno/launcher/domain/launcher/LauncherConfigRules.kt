package ir.rosheno.launcher.domain.launcher

import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.phone.normalizePhone
import java.util.Collections

class LauncherConfigRules {
    fun moveContact(config: LauncherConfig, id: String, offset: Int): LauncherConfig {
        val contacts = config.contacts.toMutableList()
        val index = contacts.indexOfFirst { it.id == id }
        val next = index + offset
        if (index >= 0 && next in contacts.indices) Collections.swap(contacts, index, next)
        return config.copy(contacts = contacts)
    }

    fun removeContact(config: LauncherConfig, id: String) = config.copy(
        contacts = config.contacts.filterNot { it.id == id },
        helpContactId = config.helpContactId.takeUnless { it == id },
    )

    fun canSelectHelp(config: LauncherConfig, id: String) =
        config.contacts.any { it.id == id && normalizePhone(it.number) != null }

    fun selectHelp(config: LauncherConfig, id: String) =
        if (canSelectHelp(config, id)) config.copy(helpContactId = id) else config

    fun setSize(config: LauncherConfig, size: Int) = config.copy(size = size.coerceIn(0, 2))
    fun completeSetup(config: LauncherConfig) = config.copy(setupComplete = true)

    fun toggleFavorite(config: LauncherConfig, key: String) = config.copy(favorites =
        if (key in config.favorites) config.favorites - key else (config.favorites + key).take(8))

    fun moveFavorite(config: LauncherConfig, key: String, offset: Int): LauncherConfig {
        val keys = config.favorites.toMutableList()
        val index = keys.indexOf(key)
        val next = index + offset
        if (index >= 0 && next in keys.indices) Collections.swap(keys, index, next)
        return config.copy(favorites = keys)
    }
}
