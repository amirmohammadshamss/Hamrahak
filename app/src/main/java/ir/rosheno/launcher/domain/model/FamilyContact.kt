package ir.rosheno.launcher.domain.model

import java.util.UUID

data class FamilyContact(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val number: String,
    val photo: String? = null,
)
