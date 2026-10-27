package ir.rosheno.launcher.domain.repository

import ir.rosheno.launcher.domain.model.MessagePage
import ir.rosheno.launcher.domain.model.SmsSendReport

interface MessageRepository {
    fun load(limit: Int): MessagePage
    fun send(address: String, body: String): String
    fun readReport(): SmsSendReport?
    /** Returns a function that releases the observer. */
    fun observeReports(onChange: () -> Unit): () -> Unit
}
