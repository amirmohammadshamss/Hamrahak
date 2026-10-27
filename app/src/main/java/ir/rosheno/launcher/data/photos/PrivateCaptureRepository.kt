package ir.rosheno.launcher.data.photos

import android.content.Context
import ir.rosheno.launcher.domain.repository.CaptureRepository
import java.io.File
import java.util.UUID

class PrivateCaptureRepository(private val context: Context) : CaptureRepository {
    private val directory get() = context.filesDir.resolve("captures")

    override fun list(): List<String> = directory.listFiles()
        ?.filter { it.extension.equals("jpg", true) && it.length() > 0 }
        ?.sortedByDescending { it.lastModified() }
        ?.map { it.absolutePath } ?: emptyList()

    override fun create(): String {
        directory.mkdirs()
        return File(directory, "${UUID.randomUUID()}.jpg").absolutePath
    }

    override fun delete(path: String) {
        File(path).takeIf { it.parentFile?.canonicalFile == directory.canonicalFile }?.delete()
    }
}
