package ir.rosheno.launcher.data.photos

import java.io.File
import java.nio.file.Files
import org.junit.Assert.*
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class OwnedFilesTest {
    @get:Rule val temporary = TemporaryFolder()

    @Test fun deletesOnlyOwnedDirectChildren() {
        val directory = temporary.newFolder("owned")
        val owned = File(directory, "photo.jpg").apply { writeText("fixture") }
        val outside = temporary.newFile("outside.jpg")
        val nested = File(directory, "nested").apply { mkdir() }.resolve("photo.jpg").apply { writeText("fixture") }
        deleteOwnedFile(owned.path, directory)
        deleteOwnedFile(outside.path, directory)
        deleteOwnedFile(nested.path, directory)
        assertFalse(owned.exists()); assertTrue(outside.exists()); assertTrue(nested.exists())
    }

    @Test fun traversalAndSymlinkParentsCannotDeleteOutsideFiles() {
        val directory = temporary.newFolder("owned")
        val outsideDirectory = temporary.newFolder("outside")
        val outside = outsideDirectory.resolve("photo.jpg").apply { writeText("fixture") }
        deleteOwnedFile(directory.resolve("../outside/photo.jpg").path, directory)
        val link = directory.resolve("linked")
        Files.createSymbolicLink(link.toPath(), outsideDirectory.toPath())
        try {
            deleteOwnedFile(link.resolve("photo.jpg").path, directory)
            assertTrue(outside.exists())
        } finally { Files.deleteIfExists(link.toPath()) }
    }

    @Test fun nullAndMissingPathsAreHarmless() {
        val directory = temporary.newFolder("owned")
        val retained = File(directory, "retained.jpg").apply { writeText("fixture") }
        deleteOwnedFile(null, directory)
        deleteOwnedFile(File(directory, "missing.jpg").path, directory)
        assertTrue(retained.exists())
    }
}
