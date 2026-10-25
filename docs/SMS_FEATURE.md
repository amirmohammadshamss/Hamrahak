# SMS feature — version 0.4 prototype

## User flow

Home → «پیام‌ها» now opens an in-app text-message screen. The user can grant read access, view recent conversations, open complete message text, select/copy text, load older messages, write a new message, or reply. Names are resolved only from the family's selected contacts; no address-book permission is requested.

The composer accepts Persian/Arabic phone digits, offers selected family recipients, preserves drafts through activity recreation, and previews the exact recipient, text and segment count before an explicit «ارسال پیامک» action. Carrier charges are stated before sending. A permission grant alone never sends a message.

The system's existing SMS application stays selected. Hamrahak does not take over incoming SMS/MMS handling, modify read/unread state, delete messages, or claim to be a complete SMS/MMS replacement. The existing SMS application remains available through the fallback button.

## Installation and permissions

This is an experimental **direct-install / sideload feature**. `READ_SMS` and `SEND_SMS` are requested only inside Messages at the relevant action. Android classifies both as hard-restricted: the installer must permit them in addition to the user's runtime grant. Some installers/devices will refuse access; the UI then offers the existing messaging application and app permission settings. See [Android permission reference](https://developer.android.com/reference/android/Manifest.permission).

**This build is not Google Play eligible as an ordinary launcher with these declarations.** Google Play restricts SMS permissions to qualified default handlers and reviewed exceptions. Before any Play submission, remove restricted permissions and use the system messaging flow, or separately implement and validate a genuinely eligible SMS/MMS handler with informed role selection and the required review. The prototype does not bypass these restrictions or request a default-SMS role with placeholder receivers. See [Google Play SMS/Call Log policy](https://support.google.com/googleplay/android-developer/answer/10208820?hl=en).

## Data and reliability

- Inbox access uses Android's SMS content provider off the UI thread. Initial query is bounded to 200 messages, with older-message expansion up to 5,000. Some protected messages can be hidden by Android. MMS content is not part of this screen.
- Provider observation runs only while the Messages UI is resumed. Permission revocation clears the displayed inbox. No internet permission, cloud, analytics, private-message logs, or SMS database copies are introduced.
- Sending uses the system's default SMS SIM. If no default SIM is available, the user gets an explanation and retains the draft. The app does not silently select a SIM or request phone-identity permission.
- Standard GSM-7 and UTF-16 segment limits are applied without querying SIM identity; surrogate pairs and GSM extension characters stay intact. Inputs are capped at 5,000 UTF-16 units.
- A private journal persists only random job tokens, segment counts, result codes and timestamps, bounded to 20 jobs. It contains no phone number or message body. The receiver is non-exported and each segment uses a unique immutable one-shot PendingIntent.
- «ارسال شد» requires successful sent callbacks for every segment; it is **not** a recipient delivery receipt. Partial failure and unknown status have distinct labels. No automatic retries occur. Unknown results time out after five minutes; checking the phone's messaging application is recommended before retrying.
- Confirmed drafts are cleared only after a successful result and only if they still match the submitted content. Failure retains the draft. Double submission is blocked while a result is pending.

## Verification boundaries

The tests query the real provider read-only after a test permission grant. Message UI tests use a fake mailbox and injected fake transport: no carrier SMS is sent and no recipient is contacted. They exercise full-text reading, replies, confirmation/cancellation, segment results, failed-draft retention and duplicate-submit protection. Real SIM delivery, operator billing, multi-SIM devices, installer differences and store eligibility still require separate validation.
