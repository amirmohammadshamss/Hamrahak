package ir.rosheno.launcher.data.photos

import java.io.File

/** Delete only direct children of the owning directory, including canonical-path checks. */
internal fun deleteOwnedFile(path: String?, directory: File) {
    path?.let { File(it) }
        ?.takeIf { it.parentFile?.canonicalFile == directory.canonicalFile }
        ?.delete()
}
