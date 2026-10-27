package ir.rosheno.launcher.domain.model

data class TextMessage(val id: Long, val thread: Long, val address: String, val body: String,
    val date: Long, val type: Int)
data class MessagePage(val messages: List<TextMessage>, val hasMore: Boolean)
data class MessageThread(val id: Long, val address: String, val messages: List<TextMessage>)
