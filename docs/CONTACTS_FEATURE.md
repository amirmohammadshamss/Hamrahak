# Phone contacts — 0.6.0 prototype

- Home → مخاطبین lists all contacts returned by Android Contacts Provider, including contacts without numbers. The family favorites list remains separate.
- READ_CONTACTS is requested only when the user selects “نمایش مخاطبین گوشی”. Denial leaves the app usable and provides an app settings shortcut. Contacts are queried on an IO dispatcher and held in memory; nothing is uploaded or copied to app storage.
- Search by name or number; Persian/Arabic full phone numbers are normalized by the existing phone helper. Large scrollable rows use the configured text size.
- Tap a contact with one valid number to open call confirmation. Multiple numbers open a number chooser. Contacts with no number show an explanation, and unsafe dialing strings are disabled.
- CALL_PHONE is requested on a user call action. Granting it opens confirmation only. Cancel does not call. Emergency numbers retain system dialer routing.
- “افزودن مخاطب جدید” opens Android’s contact editor using ACTION_INSERT. The user chooses account/name/number and saves or cancels there. No WRITE_CONTACTS permission is used. The list reloads on app resume and provider changes. Missing editors are handled by the existing platform error message.
- Read failure has retry; empty and no-search-results states are explicit.

Validation: debug app/test APK build and lint passed. Emulator UI test uses fixture contacts and callback call interception: name search, no-number contact, multiple numbers, cancel, exactly one confirmed call callback, and correct create-contact intent. Separate provider test reads real emulator contacts without writing or printing personal data. No real phone call or contact was created by automated tests. Saving a new contact through the device editor still needs physical-device acceptance testing.

Android reference: https://developer.android.com/identity/providers/contacts-provider
