package ir.rosheno.launcher.data.photos

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.platform.decodeBoundedBitmap
import java.io.File
import java.util.UUID

/** Drafts survive recreation in cache; only committed photos enter durable private storage. */
class ContactPhotos(private val context: Context) : ContactPhotoRepository {
    private val drafts get() = context.cacheDir.resolve("contact-drafts")
    private val photos get() = context.filesDir.resolve("photos")

    override fun stage(source: String): String = stage(Uri.parse(source))

    fun stage(uri: Uri): String {
        val bitmap = decodeBoundedBitmap(context, uri, maxSide = 512)
        drafts.mkdirs()
        val file = File(drafts, "${UUID.randomUUID()}.jpg")
        try {
            file.outputStream().use { check(bitmap.compress(Bitmap.CompressFormat.JPEG, 85, it)) }
            return file.absolutePath
        } catch (error: Exception) {
            file.delete()
            throw error
        } finally { bitmap.recycle() }
    }

    override fun commit(path: String?): String? {
        if (path == null) return null
        val file = File(path)
        require(file.isFile)
        if (file.parentFile?.canonicalFile == photos.canonicalFile) return path
        require(file.parentFile?.canonicalFile == drafts.canonicalFile)
        photos.mkdirs()
        val destination = File(photos, "${UUID.randomUUID()}.jpg")
        try { file.copyTo(destination); return destination.absolutePath }
        catch (error: Exception) { destination.delete(); throw error }
    }

    override fun discardDraft(path: String?) {
        deleteOwnedFile(path, drafts)
    }
    override fun deleteCommitted(path: String?) {
        deleteOwnedFile(path, photos)
    }
    override fun clear() { photos.deleteRecursively() }
}
