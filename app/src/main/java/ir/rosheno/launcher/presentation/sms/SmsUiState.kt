package ir.rosheno.launcher.presentation.sms

import ir.rosheno.launcher.domain.model.SmsSendReport
import ir.rosheno.launcher.domain.model.TextMessage
import ir.rosheno.launcher.presentation.common.UiText

data class SmsUiState(val messages: List<TextMessage> = emptyList(), val loading: Boolean = false,
    val canRead: Boolean = false, val hasMore: Boolean = false, val error: UiText? = null,
    val recipient: String = "", val draft: String = "", val submitting: Boolean = false,
    val report: SmsSendReport? = null, val reportExpired: Boolean = false,
    val mode: String = "inbox", val composeOrigin: String = "inbox", val threadId: Long? = null,
    val confirmationAddress: String? = null, val confirmationBody: String = "",
    val choosingContact: Boolean = false, val replaceDraftFor: String? = null, val clearingDraft: Boolean = false)
