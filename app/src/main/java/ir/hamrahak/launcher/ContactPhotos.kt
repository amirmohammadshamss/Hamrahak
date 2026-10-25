package ir.hamrahak.launcher

import android.content.Context
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import java.io.File
import java.util.UUID

/** Drafts survive recreation in cache; only committed photos enter durable private storage. */
class ContactPhotos(private val context: Context) {
    private val drafts get() = context.cacheDir.resolve("contact-drafts")
    private val photos get() = context.filesDir.resolve("photos")

    fun stage(uri: Uri): String {
        val bitmap = ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val scale = minOf(1f, 512f / maxOf(info.size.width, info.size.height))
            decoder.setTargetSize(maxOf(1, (info.size.width * scale).toInt()), maxOf(1, (info.size.height * scale).toInt()))
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE
        }
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

    fun commit(path: String?): String? {
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

    fun discardDraft(path: String?) {
        path?.let { File(it) }?.takeIf { it.parentFile?.canonicalFile == drafts.canonicalFile }?.delete()
    }
    fun deleteCommitted(path: String?) {
        path?.let { File(it) }?.takeIf { it.parentFile?.canonicalFile == photos.canonicalFile }?.delete()
    }
}
