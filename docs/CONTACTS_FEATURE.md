# Phone contacts — 0.7.0 prototype

- Home → مخاطبین lists all contacts returned by Android Contacts Provider, including contacts without numbers. The family favorites list remains separate.
- READ_CONTACTS is requested when the user selects “نمایش مخاطبین گوشی”, or together with WRITE_CONTACTS when saving a new contact. Denial leaves the app usable and provides an app settings shortcut. Contacts are queried on an IO dispatcher and held in memory; nothing is uploaded or copied to app storage.
- Search by name or number; Persian/Arabic full phone numbers are normalized by the existing phone helper. Large scrollable rows use the configured text size.
- Tap a contact with one valid number to open call confirmation. Multiple numbers open a number chooser. Contacts with no number show an explanation, and unsafe dialing strings are disabled.
- CALL_PHONE is requested on a user call action. Granting it opens confirmation only. Cancel does not call. Emergency numbers retain system dialer routing.
- “افزودن مخاطب جدید” opens a form inside Rosheno: name, phone number, Save and Cancel. Draft fields survive rotation and are retained on permission denial or save failure. Empty names and invalid numbers cannot be saved; Persian/Arabic numbers are normalized.
- Saving requests WRITE_CONTACTS and READ_CONTACTS if needed. A permission grant does not itself write: the user presses Save again. The transaction creates a local unassigned-account raw contact, name and mobile number together. It does not choose or sync a Google/account contact. Errors are shown in the form.
- A ViewModel retains the in-flight save across rotation and blocks repeated save requests. Success clears the form, refreshes the list and shows a confirmation. The app no longer launches an external contact editor.
- Current target SDK 36 supports this WRITE_CONTACTS workflow. Before raising the target SDK for the upcoming Android release, review the new contact-write requirements: https://developer.android.com/blog/posts/reliable-way-to-add-and-edit-contacts-android

- Read failure has retry; empty and no-search-results states are explicit.

Validation: debug app/test APK build and lint pass. Tests cover provider reads, search, multi-number calls, invalid-number rejection, opening/canceling the internal form without external intents, and saving a synthetic contact through the form then verifying its normalized number in the real provider. Synthetic records are deleted in finally cleanup. The permission-denied regression is run separately with READ_CONTACTS and WRITE_CONTACTS revoked. No phone calls are placed.

Android reference: https://developer.android.com/identity/providers/contacts-provider
