package ir.rosheno.launcher.presentation.launcher

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import ir.rosheno.launcher.R
import ir.rosheno.launcher.di.AppContainer
import ir.rosheno.launcher.domain.launcher.LauncherConfigRules
import ir.rosheno.launcher.domain.model.LauncherConfig
import ir.rosheno.launcher.domain.phone.normalizePhone
import ir.rosheno.launcher.domain.repository.AppRepository
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.domain.usecase.ResetLauncher
import ir.rosheno.launcher.domain.usecase.SaveFamilyContact
import ir.rosheno.launcher.domain.usecase.SaveLauncherConfig
import ir.rosheno.launcher.presentation.common.UiText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class LauncherViewModel @JvmOverloads constructor(
    application: Application,
    private val store: ConfigRepository = AppContainer(application).config,
    private val appRepository: AppRepository = AppContainer(application).apps,
    private val photos: ContactPhotoRepository = AppContainer(application).photos,
) : AndroidViewModel(application) {
    private val rules = LauncherConfigRules()
    private val saveFamilyContact = SaveFamilyContact(store, photos)
    private val saveLauncherConfig = SaveLauncherConfig(store, photos)
    private val resetLauncher = ResetLauncher(store, photos)
    private val mutable = MutableStateFlow(LauncherState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()

    init {
        viewModelScope.launch {
            val loaded = withContext(Dispatchers.IO) { runCatching { store.read() } }
            mutable.value = mutable.value.copy(config = loaded.getOrDefault(LauncherConfig()),
                storageError = loaded.isFailure, loading = false)
            refreshApps()
        }
    }

    fun refreshApps() {
        viewModelScope.launch {
            val apps = withContext(Dispatchers.IO) {
                appRepository.load()
            }
            mutable.value = mutable.value.copy(apps = apps)
        }
    }

    private fun update(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch {
            mutex.withLock {
                if (mutable.value.storageError) return@withLock
                val config = transform(mutable.value.config)
                val saved = withContext(Dispatchers.IO) {
                    runCatching { saveLauncherConfig(mutable.value.config, config) }
                }
                if (saved.isSuccess) mutable.value = mutable.value.copy(config = config)
                else mutable.value = mutable.value.copy(message = UiText.Resource(R.string.error_config_save))
            }
        }
    }

    fun saveContact(id: String?, name: String, number: String, photo: String?) {
        if (mutable.value.savingContact) return
        val clean = normalizePhone(number) ?: return
        if (name.isBlank()) return
        mutable.value = mutable.value.copy(savingContact = true)
        viewModelScope.launch {
            mutex.withLock {
                val current = mutable.value.config
                val result = withContext(Dispatchers.IO) { runCatching {
                    check(!mutable.value.storageError)
                    saveFamilyContact(current, id, name, clean, photo)
                } }
                mutable.value = result.fold(
                    onSuccess = { mutable.value.copy(config = it, savingContact = false,
                        contactSaveToken = java.util.UUID.randomUUID().toString()) },
                    onFailure = { mutable.value.copy(savingContact = false,
                        message = UiText.Resource(R.string.error_family_contact_save)) })
            }
        }
    }
    fun moveContact(id: String, offset: Int) = update { rules.moveContact(it, id, offset) }
    fun removeContact(id: String) = update { rules.removeContact(it, id) }
    fun setHelp(id: String) {
        if (!rules.canSelectHelp(mutable.value.config, id)) {
            message(UiText.Resource(R.string.error_help_contact_number))
            return
        }
        update { rules.selectHelp(it, id) }
    }
    fun setSize(size: Int) = update { rules.setSize(it, size) }
    fun completeSetup() = update(rules::completeSetup)
    fun toggleFavorite(key: String) = update { rules.toggleFavorite(it, key) }
    fun moveFavorite(key: String, offset: Int) = update { rules.moveFavorite(it, key, offset) }
    fun message(text: String?) { mutable.value = mutable.value.copy(message = text?.let { UiText.Dynamic(it) }) }
    fun message(text: UiText) { mutable.value = mutable.value.copy(message = text) }
    fun reset() {
        viewModelScope.launch {
            mutex.withLock {
                val result = withContext(Dispatchers.IO) { runCatching {
                    resetLauncher()
                } }
                mutable.value = if (result.isSuccess) mutable.value.copy(config = LauncherConfig(), storageError = false)
                else mutable.value.copy(message = UiText.Resource(R.string.error_reset))
            }
        }
    }
}
