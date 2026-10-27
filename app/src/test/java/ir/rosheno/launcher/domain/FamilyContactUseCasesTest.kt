package ir.rosheno.launcher.domain

import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.domain.usecase.ResetLauncher
import ir.rosheno.launcher.domain.usecase.SaveFamilyContact
import ir.rosheno.launcher.domain.usecase.SaveLauncherConfig
import org.junit.Assert.*
import org.junit.Test

class FamilyContactUseCasesTest {
    private class ConfigFake(var value: LauncherConfig = LauncherConfig()) : ConfigRepository {
        var failSave = false
        override fun read() = value
        override fun save(config: LauncherConfig) {
            check(!failSave) { "Disk unavailable" }
            value = config
        }
    }

    private class PhotosFake : ContactPhotoRepository {
        val deleted = mutableListOf<String?>()
        val discarded = mutableListOf<String?>()
        var failCleanup = false
        var cleared = false
        override fun stage(source: String) = source
        override fun commit(path: String?) = path?.replace("draft/", "photos/")
        override fun discardDraft(path: String?) { discarded += path }
        override fun deleteCommitted(path: String?) {
            check(!failCleanup)
            deleted += path
        }
        override fun clear() { cleared = true }
    }

    @Test fun savesNormalizedContactAndCleansUpOnlyAfterPersistence() {
        val old = FamilyContact("one", "Old name", "09121234567", "photos/old")
        val config = ConfigFake(LauncherConfig(contacts = listOf(old), helpContactId = old.id))
        val photos = PhotosFake()
        val updated = SaveFamilyContact(config, photos)(config.value, old.id, " New name ", "۰۹۱۲۱۲۳۴۵۶۷", "draft/new")
        assertEquals(FamilyContact("one", "New name", "09121234567", "photos/new"), updated.contacts.single())
        assertEquals("one", updated.helpContactId)
        assertEquals(updated, config.value)
        assertEquals(listOf("photos/old"), photos.deleted)
        assertEquals(listOf("draft/new"), photos.discarded)
    }

    @Test fun failedPersistenceRollsBackNewPhotoAndPreservesOldContactAndDraft() {
        val initial = LauncherConfig(contacts = listOf(FamilyContact("one", "Old", "09121234567", "photos/old")))
        val config = ConfigFake(initial).apply { failSave = true }
        val photos = PhotosFake()
        assertTrue(runCatching {
            SaveFamilyContact(config, photos)(initial, "one", "New", "09121234567", "draft/new")
        }.isFailure)
        assertEquals(initial, config.value)
        assertEquals(listOf("photos/new"), photos.deleted)
        assertTrue(photos.discarded.isEmpty())
    }

    @Test fun contactLimitAndMissingEditTargetFailBeforeCommittingPhotos() {
        val initial = LauncherConfig(contacts = (1..9).map { FamilyContact("$it", "Person", "09121234567") })
        val config = ConfigFake(initial)
        val photos = PhotosFake()
        val save = SaveFamilyContact(config, photos)
        assertTrue(runCatching { save(initial, null, "Extra", "09121234567", null) }.isFailure)
        assertTrue(runCatching { save(initial, "missing", "Edit", "09121234567", null) }.isFailure)
        assertEquals(initial, config.value)
        assertTrue(photos.discarded.isEmpty())
    }

    @Test fun removingContactKeepsPhotosStillReferencedByAnotherContact() {
        val first = FamilyContact("one", "First", "09121234567", "photos/shared")
        val second = first.copy(id = "two")
        val initial = LauncherConfig(contacts = listOf(first, second))
        val config = ConfigFake(initial)
        val photos = PhotosFake()
        SaveLauncherConfig(config, photos)(initial, initial.copy(contacts = listOf(second)))
        assertTrue(photos.deleted.isEmpty())
        assertEquals(listOf(second), config.value.contacts)
    }

    @Test fun cleanupFailureDoesNotUndoSuccessfulConfigurationWrite() {
        val initial = LauncherConfig(contacts = listOf(FamilyContact("one", "Person", "09121234567", "photos/old")))
        val config = ConfigFake(initial)
        val photos = PhotosFake().apply { failCleanup = true }
        SaveLauncherConfig(config, photos)(initial, LauncherConfig())
        assertEquals(LauncherConfig(), config.value)
    }

    @Test fun resetNeverDeletesPhotosWhenConfigurationWriteFails() {
        val config = ConfigFake().apply { failSave = true }
        val photos = PhotosFake()
        assertTrue(runCatching { ResetLauncher(config, photos)() }.isFailure)
        assertFalse(photos.cleared)
        config.failSave = false
        ResetLauncher(config, photos)()
        assertTrue(photos.cleared)
        assertEquals(LauncherConfig(), config.value)
    }
}
