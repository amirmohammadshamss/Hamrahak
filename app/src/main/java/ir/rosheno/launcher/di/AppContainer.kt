package ir.rosheno.launcher.di

import android.content.Context
import ir.rosheno.launcher.data.apps.AndroidAppRepository
import ir.rosheno.launcher.data.config.ConfigStore
import ir.rosheno.launcher.data.contacts.AndroidPhoneContactsRepository
import ir.rosheno.launcher.data.photos.ContactPhotos
import ir.rosheno.launcher.data.photos.PrivateCaptureRepository
import ir.rosheno.launcher.data.sms.SmsRepository
import ir.rosheno.launcher.domain.repository.AppRepository
import ir.rosheno.launcher.domain.repository.CaptureRepository
import ir.rosheno.launcher.domain.repository.ConfigRepository
import ir.rosheno.launcher.domain.repository.ContactPhotoRepository
import ir.rosheno.launcher.domain.repository.MessageRepository
import ir.rosheno.launcher.domain.repository.PhoneContactsRepository

class AppContainer(context: Context) {
    val config: ConfigRepository by lazy { ConfigStore(context) }
    val apps: AppRepository by lazy { AndroidAppRepository(context) }
    val captures: CaptureRepository by lazy { PrivateCaptureRepository(context) }
    val photos: ContactPhotoRepository by lazy { ContactPhotos(context) }
    val phoneContacts: PhoneContactsRepository by lazy { AndroidPhoneContactsRepository(context) }
    val messages: MessageRepository by lazy { SmsRepository(context) }
}
