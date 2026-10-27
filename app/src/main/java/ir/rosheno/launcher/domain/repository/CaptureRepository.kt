package ir.rosheno.launcher.domain.repository

interface CaptureRepository {
    fun list(): List<String>
    fun create(): String
    fun delete(path: String)
}
