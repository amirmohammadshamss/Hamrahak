package ir.rosheno.launcher

import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LauncherStateTest {
    @get:Rule val compose = createEmptyComposeRule()
    private lateinit var app: IsolatedTestApp
    private val owners = mutableListOf<ViewModelStore>()
    private fun main(block: () -> Unit) = InstrumentationRegistry.getInstrumentation().runOnMainSync(block)
    @Before fun prepare() { app = IsolatedTestApp(ApplicationProvider.getApplicationContext()) }
    @After fun cleanup() { main { owners.forEach { it.clear() } }; app.cleanup() }
    private fun model(): LauncherViewModel {
        lateinit var vm: LauncherViewModel
        main {
            val owner = ViewModelStore().also { owners += it }
            vm = ViewModelProvider(owner, ViewModelProvider.AndroidViewModelFactory(app))[LauncherViewModel::class.java]
        }
        compose.waitUntil(5_000) { !vm.state.value.loading }
        return vm
    }

    @Test fun familyCapacityDuplicateSaveAndInvalidInputsDoNotCorruptContacts() {
        ConfigStore(app).save(LauncherConfig(contacts = (1..8).map { FamilyContact("$it", "Person $it", "09121234567") }))
        val vm = model()
        main { vm.saveContact(null, " Ninth ", "۰۹۱۲۱۲۳۴۵۶۷", null); vm.saveContact(null, "Duplicate", "09121234567", null) }
        compose.waitUntil(5_000) { !vm.state.value.savingContact }
        assertEquals(9, ConfigStore(app).read().contacts.size)
        assertEquals("Ninth", vm.state.value.config.contacts.last().name)
        main { vm.saveContact(null, "Tenth", "09121234567", null) }
        compose.waitUntil(5_000) { !vm.state.value.savingContact }
        assertNotNull(vm.state.value.message)
        val before = ConfigStore(app).read()
        main { vm.saveContact(null, "", "09121234567", null); vm.saveContact(null, "Bad", "*123#", null) }
        assertEquals(before, ConfigStore(app).read())
    }

    @Test fun favoriteCapacityOrderingRemovalAndTextSizePersist() {
        val vm = model()
        main {
            (1..9).forEach { vm.toggleFavorite("app-$it/.Main") }
            vm.moveFavorite("app-8/.Main", -1)
            vm.setSize(99)
        }
        compose.waitUntil(5_000) { vm.state.value.config.size == 2 }
        assertEquals(8, vm.state.value.config.favorites.size)
        assertEquals("app-8/.Main", vm.state.value.config.favorites[6])
        main { vm.toggleFavorite("app-1/.Main"); vm.moveFavorite("missing", 1); vm.setSize(-1); vm.completeSetup() }
        compose.waitUntil(5_000) { vm.state.value.config.setupComplete }
        val restored = model().state.value.config
        assertEquals(7, restored.favorites.size)
        assertFalse("app-1/.Main" in restored.favorites)
        assertEquals(0, restored.size)
        assertTrue(restored.setupComplete)
    }

    @Test fun deletingHelpContactClearsHelpAndOnlyDeletesItsOwnedPhoto() {
        val photo = app.directory.resolve("photos/owned.jpg").apply { parentFile!!.mkdirs(); writeText("fixture") }
        val unrelated = app.directory.resolve("unrelated.txt").apply { writeText("keep") }
        ConfigStore(app).save(LauncherConfig(contacts = listOf(FamilyContact("one", "Person", "09121234567", photo.path)), helpContactId = "one"))
        val vm = model()
        main { vm.removeContact("one") }
        compose.waitUntil(5_000) { vm.state.value.config.contacts.isEmpty() && !photo.exists() }
        assertNull(ConfigStore(app).read().helpContactId)
        assertTrue(unrelated.exists())
        main { vm.setHelp("missing") }
        assertNotNull(vm.state.value.message)
        assertNull(vm.state.value.config.helpContactId)
    }

    @Test fun damagedStorageIsPreservedUntilExplicitReset() {
        val prefs = app.getSharedPreferences("launcher-prototype", 0)
        prefs.edit().putString("config", "damaged-json").commit()
        val vm = model()
        assertTrue(vm.state.value.storageError)
        main { vm.setSize(2); vm.completeSetup() }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        assertEquals("damaged-json", prefs.getString("config", null))
        main { vm.reset() }
        compose.waitUntil(5_000) { !vm.state.value.storageError }
        assertEquals(LauncherConfig(), ConfigStore(app).read())
    }

    @Test fun appDirectoryExcludesItselfAndHasUniqueLaunchComponents() {
        val vm = model()
        compose.waitUntil(5_000) { vm.state.value.apps.isNotEmpty() }
        val apps = vm.state.value.apps
        assertTrue(apps.none { it.component.packageName == app.packageName })
        assertEquals(apps.size, apps.map { it.key }.distinct().size)
        assertTrue(apps.any { it.component.packageName == "com.android.settings" })
    }
}
