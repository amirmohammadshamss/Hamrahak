package ir.rosheno.launcher.domain.repository

import ir.rosheno.launcher.domain.model.MessagePage

interface SmsSource { fun load(limit: Int): MessagePage }
