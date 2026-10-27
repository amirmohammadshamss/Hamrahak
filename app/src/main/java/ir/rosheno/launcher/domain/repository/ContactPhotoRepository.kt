package ir.rosheno.launcher.domain.repository

interface ContactPhotoRepository {
    fun stage(source: String): String
    fun commit(path: String?): String?
    fun discardDraft(path: String?)
    fun deleteCommitted(path: String?)
    fun clear()
}
