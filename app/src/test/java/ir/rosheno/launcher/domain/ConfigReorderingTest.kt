package ir.rosheno.launcher.domain

import ir.rosheno.launcher.domain.launcher.LauncherConfigRules
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LauncherConfig
import org.junit.Assert.*
import org.junit.Test

class ConfigReorderingTest {
    private val rules = LauncherConfigRules()
    private val original = LauncherConfig(
        contacts = listOf(FamilyContact("one", "First", "09121234567"), FamilyContact("two", "Second", "09121234568")),
        favorites = listOf("app/one", "app/two"), helpContactId = "one", setupComplete = true,
    )

    @Test fun contactsAndFavoritesReorderWithoutChangingOtherConfiguration() {
        val contactsMoved = rules.moveContact(original, "one", 1)
        assertEquals(listOf("two", "one"), contactsMoved.contacts.map { it.id })
        assertEquals(original, contactsMoved.copy(contacts = original.contacts))
        val favoritesMoved = rules.moveFavorite(original, "app/two", -1)
        assertEquals(listOf("app/two", "app/one"), favoritesMoved.favorites)
        assertEquals(original, favoritesMoved.copy(favorites = original.favorites))
    }

    @Test fun missingItemsAndOutOfBoundsMovesLeaveBothListsUnchanged() {
        assertEquals(original, rules.moveContact(original, "missing", 1))
        assertEquals(original, rules.moveFavorite(original, "missing", 1))
        assertEquals(original, rules.moveContact(original, "one", -1))
        assertEquals(original, rules.moveContact(original, "two", 1))
        assertEquals(original, rules.moveFavorite(original, "app/one", -1))
        assertEquals(original, rules.moveFavorite(original, "app/two", 1))
    }
}
