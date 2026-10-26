# Delivery status

Milestone: discovery prototype. Gate A is pending real participant evidence.

Current version: **0.7.0 prototype**. All device contacts are available from the new Contacts tile, with search, multi-number selection, confirmed calling, and an in-app contact creation form backed by a local Contacts Provider transaction. See [contacts](CONTACTS_FEATURE.md). Build/lint succeed (zero errors, six style/toolchain warnings). Sixteen tests pass on Android 17 at 150% system font scale with animations disabled. Manual dialing is now inside the launcher and confirmed calls use Android Telecom through the existing phone app. Call intents are intercepted in tests; no real call/SMS was sent. Existing SMS publication constraints still apply. See [dialer](DIALER_FEATURE.md), [verification](VERIFICATION.md) and [SMS constraints](SMS_FEATURE.md). Physical-device and participant acceptance remain pending.

## Implemented for evaluation

- FR01: Home manifest integration, consent-based role request, older-system Home-settings fallback. OEM/Android 9 testing pending.
- FR02: six fixed labeled primary tiles plus help. Custom primary-action arrangement pending.
- FR03: manually entered contacts, up to nine, optional locally copied photo, editing and reordering, and photo removal. Import remains pending. The first two contacts also appear as quick-call photo cards on Home.
- FR04: named confirmation and safe system-dialer handoff; no direct calling.
- FR05: launchable app favorites, add/remove and reorder, up to eight; real application icons are displayed and tappable.
- FR06: three text presets; scrolling keeps actions reachable. Core journeys passed with the largest preset and 150% system fonts on the emulator. Broader accessibility/user validation remains pending.
- FR08: Persian text, RTL composition, Solar Hijri date via Android ICU. Full localization review pending.
- FR09: explicitly selected trusted contact and confirmed dialer handoff. Help remains fixed below the Home scroll area. Missing/deleted helper contacts open a dedicated selection screen rather than general settings; a saved choice leads to explicit call confirmation.
- FR10: introductory Home selection, manual contacts, size choice. Iterative full onboarding pending.
- FR11: reset, other-Home settings link, written recovery route. Manufacturer testing pending.
- FR12: no network permission or account; local features and assets.
- FR13: searchable list of launchable apps subject to Android visibility restrictions.
- FR14: offline step-by-step illustrated Persian tutorials with phone mockups, highlighted touch targets, pointer illustrations, and next/previous controls. Usability validation pending.

## Pending MVP work after validation

- FR07 optional family PIN and documented recovery that preserves owner autonomy.
- Room/DataStore storage, migrations, secure PIN verifier, and deletion/retention review.
- Contact import and broader photo-provider/process-death testing. Editing, ordering, recreation-safe draft photos, bounded image decoding, and save/cancel cleanup are implemented.
- Customizable six-to-eight main actions and clearer app aliases validated with participants.
- Dedicated accessibility audit: TalkBack, focus order, touch targets, contrast, mixed direction numbers, system font scaling.
- Six physical devices / three manufacturers, Android 9 and current OS testing, performance and battery measurements, pilot stability evidence.
- Approved naming, production identity/signing custody, privacy policy/legal ownership, Figma components, store assets, signed release APK/AAB, handover and support agreement.

## External discovery inputs still needed

Named product/research owner; recruitment and consent arrangements; device pool; Persian wording review; availability of family helpers; publishing/signing owner. Technical prototyping can proceed independently, but no participant validation or production-release acceptance can be claimed without these inputs.

P1/P2 remain outside this prototype: Ultra-simple mode, reminders, voice commands, notification access, cloud accounts, remote family control, monetization, and AI assistance.

## Version 0.2 improvements

- Contact editing preserves IDs, trusted-help selection, and order. Cancel does not change saved details.
- The contact form closes after successful persistence; a failed save retains entered details.
- Selected photos are temporary cache drafts until save succeeds; cancellation cannot delete the existing committed photo. Images are decoded to at most 512 pixels on the longest edge using Android ImageDecoder.
- Draft fields and the editing screen survive activity recreation.
- Each screen opens with its own scroll state; secondary screens have a visible Home control.
- First two family contacts get direct Home shortcuts, each retaining explicit call confirmation.
- The Help action remains visible while the Home content scrolls.

## Version 0.3 additions (user-directed)

- New green/cream visual system, bundled Vazirmatn font and OFL notice, adaptive/themed app icon, native splash branding, and a short skippable animated welcome for manual cold launches. Home launches bypass the welcome.
- Page fade/slide and press feedback, with system animation preferences respected.
- Actual installed-app icons in the directory and favorites.
- In-app CameraX camera, private photo library, selected-photo viewer, and a persistent «بازگشت به روشنو» action. Camera permission is deferred until requested inside the feature.
- User-requested camera/photo screens supersede the original plan’s external-camera/gallery handoff for these two functions. Original messaging and other app shortcuts still use system intents.
- Captured photos are app-private, preserved by launcher-settings reset, and removed by app uninstall. Export/share and comprehensive photo retention controls remain future work.

## Version 0.4 — user-requested SMS

- Home Messages now has an in-app conversation list, complete message text, reply/new-message composer, family recipient selection and a persistent return-to-launcher control.
- Read/send permissions are deferred to their feature actions. The default messaging application is preserved; no incoming SMS/MMS takeover or read-state modification occurs.
- Exact recipient/text confirmation and segment/cost information precede every send. Status is based on per-part callbacks, not merely a successful API call. No automatic retries; pending sends block duplicate submissions; failure/unknown results retain drafts.
- The result journal stores only random IDs, counts, times and result codes. Message bodies are read from the system provider and not copied into an app inbox.
- Illustrated SMS help was added. Android 9 compatibility and Persian/GSM/emoji segment handling were checked by compilation/lint and targeted tests. Real SIM delivery/billing remains unverified.
- READ_SMS/SEND_SMS are hard-restricted and distribution-dependent. This ordinary-launcher implementation is not Play-eligible with these permissions; publication requires a different eligible handler architecture or restricted-permission removal. See SMS_FEATURE.md.

## Version 0.5 — user-requested internal dial pad

- Home Call opens an app-owned keypad with fixed number display, large digit keys, delete/clear, family-number selection and a persistent return control.
- CALL_PHONE permission is requested only at the call action. A separate confirmation snapshots the normalized number; cancel never calls. Numeric input is preserved across rotation and launcher navigation.
- Known/platform-recognized emergency numbers use the system dialer. Ordinary confirmed numbers use ACTION_CALL; no call-log/identity/audio permissions or default phone-role takeover were added.
- Connected/incoming call screens, SIM/network handling and hangup remain controlled by the existing phone application. Photo-contact/help handoffs are unchanged.
- Illustrated manual-call instructions were added. Actual SIM call success/quality remains unverified; tests block the phone intents before dispatch to a phone activity.

Automated regression coverage now includes 41 instrumented tests plus two runner checks. The complete suite passes on Android 17 at normal and 150% system text size. The ordered runner restores temporary permissions/display settings and saves phase logs/JSON summaries. GitHub Actions is configured for API 28/API 37; hosted execution remains pending. See [automated testing](AUTOMATED_TESTS.md).
