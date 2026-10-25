package ir.hamrahak.launcher

import android.app.Application
import android.content.Intent
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

data class LauncherState(
    val config: LauncherConfig = LauncherConfig(),
    val apps: List<LaunchableApp> = emptyList(),
    val loading: Boolean = true,
    val storageError: Boolean = false,
    val message: String? = null,
    val savingContact: Boolean = false,
    val contactSaveToken: String? = null,
)

class LauncherViewModel(application: Application) : AndroidViewModel(application) {
    private val store = ConfigStore(application)
    private val mutable = MutableStateFlow(LauncherState())
    val state = mutable.asStateFlow()
    private val mutex = Mutex()
    private val photos = ContactPhotos(application)

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
                val context = getApplication<Application>()
                val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
                context.packageManager.queryIntentActivities(intent, 0)
                    .filter { it.activityInfo.packageName != context.packageName }
                    .map { LaunchableApp(it.loadLabel(context.packageManager).toString(),
                        android.content.ComponentName(it.activityInfo.packageName, it.activityInfo.name)) }
                    .distinctBy { it.key }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.label })
            }
            mutable.value = mutable.value.copy(apps = apps)
        }
    }

    private fun update(transform: (LauncherConfig) -> LauncherConfig) {
        viewModelScope.launch {
            mutex.withLock {
                if (mutable.value.storageError) return@withLock
                val config = transform(mutable.value.config)
                val saved = withContext(Dispatchers.IO) { runCatching { store.save(config) } }
                if (saved.isSuccess) {
                    val obsoletePhotos = mutable.value.config.contacts.mapNotNull { it.photo }.toSet() -
                        config.contacts.mapNotNull { it.photo }.toSet()
                    mutable.value = mutable.value.copy(config = config)
                    withContext(Dispatchers.IO) {
                        val photoDir = getApplication<Application>().filesDir.resolve("photos")
                        obsoletePhotos.forEach { path ->
                            val file = java.io.File(path)
                            if (file.parentFile?.canonicalFile == photoDir.canonicalFile) file.delete()
                        }
                    }
                } else mutable.value = mutable.value.copy(message = "ذخیره نشد. دوباره تلاش کنید.")
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
                val previous = current.contacts.find { it.id == id }
                val result = withContext(Dispatchers.IO) { runCatching {
                    check(!mutable.value.storageError)
                    check(if (id == null) current.contacts.size < 9 else previous != null)
                    val committed = photos.commit(photo)
                    val contact = FamilyContact(id = id ?: java.util.UUID.randomUUID().toString(),
                        name = name.trim(), number = clean, photo = committed)
                    val config = current.copy(contacts = if (id == null) current.contacts + contact
                        else current.contacts.map { if (it.id == id) contact else it })
                    try { store.save(config) }
                    catch (error: Exception) {
                        if (committed != photo) photos.deleteCommitted(committed)
                        throw error
                    }
                    // A cleanup failure must not turn a successful configuration write into a failed save.
                    runCatching {
                        photos.discardDraft(photo)
                        if (previous?.photo != committed) photos.deleteCommitted(previous?.photo)
                    }
                    config
                } }
                mutable.value = result.fold(
                    onSuccess = { mutable.value.copy(config = it, savingContact = false,
                        contactSaveToken = java.util.UUID.randomUUID().toString()) },
                    onFailure = { mutable.value.copy(savingContact = false,
                        message = "ذخیره نشد. اطلاعات شما در فرم مانده است. شماره و عکس را بررسی کنید و دوباره تلاش کنید.") })
            }
        }
    }
    fun moveContact(id: String, offset: Int) = update { c ->
        val contacts = c.contacts.toMutableList()
        val index = contacts.indexOfFirst { it.id == id }
        val next = index + offset
        if (index >= 0 && next in contacts.indices) java.util.Collections.swap(contacts, index, next)
        c.copy(contacts = contacts)
    }
    fun removeContact(id: String) = update { c -> c.copy(
        contacts = c.contacts.filterNot { it.id == id },
        helpContactId = c.helpContactId.takeUnless { it == id }) }
    fun setHelp(id: String) {
        if (mutable.value.config.contacts.none { it.id == id && normalizePhone(it.number) != null }) {
            message("شمارهٔ این مخاطب معتبر نیست. نام و شماره را در تنظیمات خانواده بررسی کنید.")
            return
        }
        update { config -> if (config.contacts.any { it.id == id && normalizePhone(it.number) != null })
            config.copy(helpContactId = id) else config }
    }
    fun setSize(size: Int) = update { it.copy(size = size.coerceIn(0, 2)) }
    fun completeSetup() = update { it.copy(setupComplete = true) }
    fun toggleFavorite(key: String) = update { c -> c.copy(favorites =
        if (key in c.favorites) c.favorites - key else (c.favorites + key).take(8)) }
    fun moveFavorite(key: String, offset: Int) = update { c ->
        val keys = c.favorites.toMutableList()
        val index = keys.indexOf(key)
        val next = index + offset
        if (index >= 0 && next in keys.indices) {
            java.util.Collections.swap(keys, index, next)
        }
        c.copy(favorites = keys)
    }
    fun message(text: String?) { mutable.value = mutable.value.copy(message = text) }
    fun reset() {
        viewModelScope.launch {
            mutex.withLock {
                val result = withContext(Dispatchers.IO) { runCatching {
                    store.save(LauncherConfig())
                    getApplication<Application>().filesDir.resolve("photos").deleteRecursively()
                } }
                mutable.value = if (result.isSuccess) mutable.value.copy(config = LauncherConfig(), storageError = false)
                else mutable.value.copy(message = "بازنشانی انجام نشد. دوباره تلاش کنید.")
            }
        }
    }
}
