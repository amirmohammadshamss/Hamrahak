package ir.rosheno.launcher.domain.repository

interface SmsTransport { fun submit(address: String, body: String, token: String, parts: Int) }
