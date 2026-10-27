package ir.rosheno.launcher.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import ir.rosheno.launcher.domain.model.FamilyContact
import ir.rosheno.launcher.domain.model.LaunchableApp
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.model.PhoneContact
import ir.rosheno.launcher.domain.repository.CaptureRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.domain.repository.PhoneContactsRepository
import ir.rosheno.launcher.presentation.apps.AppDirectoryViewModel
import ir.rosheno.launcher.presentation.contacts.ContactsViewModel
import ir.rosheno.launcher.presentation.dialer.DialerViewModel
import ir.rosheno.launcher.presentation.family.ContactEditorViewModel
import ir.rosheno.launcher.presentation.help.HelpViewModel
import ir.rosheno.launcher.presentation.launcher.LauncherNavigationViewModel
import ir.rosheno.launcher.presentation.photos.CameraViewModel
import ir.rosheno.launcher.presentation.photos.PhotoLibraryViewModel
import ir.rosheno.launcher.presentation.settings.SettingsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class FeatureViewModelsTest {
    private val dispatcher = StandardTestDispatcher()
    private val stores = mutableListOf<ViewModelStore>()
    @Before fun setUp() { Dispatchers.setMain(dispatcher) }
    @After fun tearDown() { stores.forEach { it.clear() }; Dispatchers.resetMain() }
    private fun <T : ViewModel> own(model: T): T {
        val store = ViewModelStore().also { stores += it }
        return ViewModelProvider(store, object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <V : ViewModel> create(modelClass: Class<V>): V = model as V
        })[model.javaClass]
    }
    private class ContactsFake : PhoneContactsRepository {
        var rows = listOf(PhoneContact(1, "Alice", listOf("09121234567")), PhoneContact(2, "Bob", emptyList()))
        var failure: Exception? = null
        var writeFailure: Exception? = null
        var reads = 0
        var releases = 0
        var listener: (() -> Unit)? = null
        override fun load(): List<PhoneContact> { reads++; failure?.let { throw it }; return rows }
        override fun create(name: String, number: String) { writeFailure?.let { throw it } }
        override fun observe(onChange: () -> Unit): () -> Unit {
            listener = onChange
            return { releases++; listener = null }
        }
    }
    private class PhotosFake : ContactPhotoRepository {
        var stages = 0
        var fail = false
        val discarded = mutableListOf<String?>()
        override fun stage(source: String): String { stages++; check(!fail); return "draft/$source" }
        override fun commit(path: String?) = path
        override fun discardDraft(path: String?) { discarded += path }
        override fun deleteCommitted(path: String?) = Unit
        override fun clear() = Unit
    }
    private class CapturesFake : CaptureRepository {
        var fail = false
        var creates = 0
        val deleted = mutableListOf<String>()
        override fun list(): List<String> { check(!fail); return listOf("captures/one") }
        override fun create(): String { creates++; check(!fail); return "captures/$creates" }
        override fun delete(path: String) { deleted += path }
    }

    @Test fun navigationRestoresHistoryDraftAndEditorSession() {
        val saved = SavedStateHandle()
        val model = own(LauncherNavigationViewModel(saved))
        model.navigate("family"); model.navigate("settings"); model.dialNumber("۰۹۱۲")
        model.editContact("one")
        val restored = own(LauncherNavigationViewModel(saved))
        assertEquals(model.state.value, restored.state.value)
        restored.back()
        assertEquals("family", restored.state.value.screen)
        restored.navigate("home"); restored.back()
        assertEquals("home", restored.state.value.screen)
        assertTrue(restored.state.value.history.isEmpty())
    }
    @Test fun homeSignalClosesPendingActionsAndPreservesDialDraft() {
        val model = own(LauncherNavigationViewModel(SavedStateHandle()))
        model.homeSignal(0); model.navigate("settings"); model.editContact(); model.callContact("one")
        model.confirmReset(true); model.dialNumber("123"); model.homeSignal(1)
        assertEquals("home", model.state.value.screen)
        assertFalse(model.state.value.adding); assertFalse(model.state.value.reset)
        assertNull(model.state.value.callingId); assertEquals("123", model.state.value.dialedNumber)
    }
    @Test fun helpSelectionCallsOnlyAfterConfigurationIsSaved() {
        val model = own(LauncherNavigationViewModel(SavedStateHandle()))
        val contact = FamilyContact("one", "Alice", "09121234567")
        model.navigate("help-contact"); model.selectHelper("one")
        model.configurationChanged(LauncherConfig(contacts = listOf(contact)), null)
        assertNull(model.state.value.callingId)
        model.configurationChanged(LauncherConfig(contacts = listOf(contact), helpContactId = "one"), null)
        assertEquals("home", model.state.value.screen); assertEquals("one", model.state.value.callingId)
        model.callContact(null)
        model.configurationChanged(LauncherConfig(contacts = listOf(contact), helpContactId = "one"), null)
        assertNull(model.state.value.callingId)
    }
    @Test fun contactsRefreshRetryFilteringAndObserverCleanup() = runTest(dispatcher) {
        val repository = ContactsFake().apply { failure = IllegalStateException() }
        val model = own(ContactsViewModel(SavedStateHandle(), repository, dispatcher))
        model.start(true); advanceUntilIdle(); assertTrue(model.state.value.failed)
        repository.failure = null; model.refresh(); advanceUntilIdle()
        model.search("Alice"); assertEquals(listOf(1L), model.state.value.filtered.map { it.id })
        model.search("۰۹۱۲"); assertEquals(listOf(1L), model.state.value.filtered.map { it.id })
        repository.rows = listOf(PhoneContact(3, "Changed", emptyList()))
        repository.listener?.invoke(); advanceUntilIdle()
        assertEquals(3L, model.state.value.contacts.single().id)
        model.stop(); assertNull(repository.listener); assertEquals(1, repository.releases)
        model.start(false); advanceUntilIdle()
        assertFalse(model.state.value.allowed); assertTrue(model.state.value.contacts.isEmpty())
    }
    @Test fun deniedSaveRetainsDraftAndRestoresForm() = runTest(dispatcher) {
        val saved = SavedStateHandle()
        val repository = ContactsFake().apply { writeFailure = SecurityException() }
        val model = own(ContactsViewModel(saved, repository, dispatcher))
        model.add(); model.editName("Alice"); model.editNumber("۰۹۱۲۱۲۳۴۵۶۷"); model.save()
        advanceUntilIdle()
        assertFalse(model.state.value.saving); assertFalse(model.state.value.saved)
        assertNotNull(model.state.value.error); assertTrue(model.state.value.adding)
        val restored = own(ContactsViewModel(saved, repository, dispatcher))
        assertEquals("Alice", restored.state.value.draftName)
        assertEquals("۰۹۱۲۱۲۳۴۵۶۷", restored.state.value.draftNumber)
    }
    @Test fun permissionGrantProducesConfirmationThatCanOnlyBeConsumedOnce() {
        val saved = SavedStateHandle()
        val model = own(DialerViewModel(saved))
        assertTrue(model.requestCall("09121234567", false))
        assertNull(model.state.value.confirming)
        val restored = own(DialerViewModel(saved)); restored.permissionResult(true)
        assertEquals("09121234567", restored.state.value.confirming)
        assertEquals("09121234567", restored.consumeConfirmation())
        assertNull(restored.consumeConfirmation())
        assertFalse(restored.requestCall("*123#", true))
    }
    @Test fun editorStagesOnlyOnceAndRestoresDraftAcrossRecreation() = runTest(dispatcher) {
        val repository = PhotosFake()
        val saved = SavedStateHandle()
        val model = own(ContactEditorViewModel(saved, repository, dispatcher))
        model.begin("session", null); model.name("Alice"); model.number("09121234567")
        model.stagePhoto("one"); model.stagePhoto("two"); advanceUntilIdle()
        assertEquals(1, repository.stages); assertEquals("draft/one", model.state.value.photo)
        val restored = own(ContactEditorViewModel(saved, repository, dispatcher))
        restored.begin("session", null)
        assertEquals(model.state.value, restored.state.value)
        restored.begin("new session", null)
        assertEquals("", restored.state.value.name); assertNull(restored.state.value.photo)
    }
    @Test fun changingEditorSessionCancelsOldPhotoWork() = runTest(dispatcher) {
        val repository = PhotosFake()
        val model = own(ContactEditorViewModel(SavedStateHandle(), repository, dispatcher))
        model.begin("first", null); model.stagePhoto("old")
        model.begin("second", FamilyContact("two", "Bob", "09121234567"))
        advanceUntilIdle()
        assertEquals("Bob", model.state.value.name)
        assertNull(model.state.value.photo); assertFalse(model.state.value.busy)
    }
    @Test fun deleteConfirmationRestoresAndCanOnlyBeConsumedOnce() {
        val saved = SavedStateHandle()
        val settings = own(SettingsViewModel(saved)); settings.requestDelete("one")
        val restored = own(SettingsViewModel(saved))
        assertEquals("one", restored.consumeDelete()); assertNull(restored.consumeDelete())
    }
    @Test fun editorPhotoFailureKeepsPreviousPhotoAndReportsError() = runTest(dispatcher) {
        val repository = PhotosFake().apply { fail = true }
        val model = own(ContactEditorViewModel(SavedStateHandle(), repository, dispatcher))
        model.begin("one", FamilyContact("one", "Alice", "09121234567", "photos/old"))
        model.stagePhoto("bad"); advanceUntilIdle()
        assertFalse(model.state.value.busy); assertEquals("photos/old", model.state.value.photo)
        assertNotNull(model.state.value.error)
    }
    @Test fun photoLibraryCanRetryAfterRepositoryFailure() = runTest(dispatcher) {
        val repository = CapturesFake().apply { fail = true }
        val model = own(PhotoLibraryViewModel(repository, dispatcher))
        model.refresh(); advanceUntilIdle(); assertNotNull(model.state.value.error)
        repository.fail = false; model.refresh(); advanceUntilIdle()
        assertNull(model.state.value.error); assertFalse(model.state.value.loading)
        assertEquals(listOf("captures/one"), model.state.value.photos)
    }
    @Test fun cameraPreventsDuplicateDispatchAndCleansFailedCapture() = runTest(dispatcher) {
        val repository = CapturesFake()
        val model = own(CameraViewModel(repository, dispatcher))
        model.requestCapture(); model.requestCapture(); advanceUntilIdle()
        assertEquals(1, repository.creates)
        val path = requireNotNull(model.takePreparedCapture())
        assertNull(model.takePreparedCapture()); assertTrue(model.state.value.saving)
        model.captureFailed(path); advanceUntilIdle()
        assertEquals(listOf(path), repository.deleted); assertFalse(model.state.value.saving)
        model.requestCapture(); advanceUntilIdle()
        val retry = requireNotNull(model.takePreparedCapture())
        assertTrue(model.captureSaved(retry)); assertFalse(model.captureSaved(retry))
    }
    @Test fun cancelledCapturePreparationCannotDispatchOnReturn() = runTest(dispatcher) {
        val repository = CapturesFake()
        val model = own(CameraViewModel(repository, dispatcher))
        model.requestCapture(); model.cancelPreparation(); advanceUntilIdle()
        assertNull(model.takePreparedCapture()); assertFalse(model.state.value.saving)
        model.requestCapture(); advanceUntilIdle()
        val prepared = requireNotNull(model.state.value.preparedPath)
        model.cancelPreparation(); advanceUntilIdle()
        assertNull(model.takePreparedCapture()); assertEquals(listOf(prepared), repository.deleted)
    }
    @Test fun appSearchAndGuideProgressRestoreWithoutAComposable() {
        val saved = SavedStateHandle()
        val directory = own(AppDirectoryViewModel(saved)); directory.search("Maps")
        val restored = own(AppDirectoryViewModel(saved))
        assertEquals(listOf("maps/Main"), restored.filter(listOf(LaunchableApp("Maps", "maps/Main"),
            LaunchableApp("Phone", "phone/Main"))).map { it.key })
        val helpSaved = SavedStateHandle()
        val help = own(HelpViewModel(helpSaved)); help.select(2); help.next(3)
        val guide = own(HelpViewModel(helpSaved)); assertEquals(1, guide.state.value.index)
        guide.previous(); guide.previous(); assertEquals(-1, guide.state.value.selected)
    }
}
