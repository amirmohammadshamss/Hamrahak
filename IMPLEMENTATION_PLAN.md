# Hamrahak / Hamrah Sadeh — implementation plan

Based on `RFP_Android_Senior_Launcher_EN.pdf`, version 1.0, 25 October 2026.

Execution started: a discovery Android prototype and research protocol are now in this repository. See [delivery status](docs/DELIVERY_STATUS.md) for implemented versus pending requirements and [verification](docs/VERIFICATION.md) for test evidence. Gate A remains pending participant research; the original roadmap below is the baseline.

User-directed additions during implementation: version 0.3 includes an in-app camera and photo viewer with explicit return-to-launcher controls, actual app icons, illustrated tutorials, and visual/animation polish. These replace the original external-camera/gallery recommendation for those two functions. Camera access is requested only at use; account/network/direct-call permissions remain excluded. Discovery and physical-device acceptance gates are still pending.

Version 0.4 user-directed scope change: Messages includes an experimental sideload SMS inbox/reader/composer with runtime read/send consent and explicit per-message confirmation. This supersedes the original external-messaging-only boundary for direct-install evaluation. The existing default SMS/MMS handler is retained. Restricted permission/store eligibility is a release blocker documented in [SMS_FEATURE.md](docs/SMS_FEATURE.md), not an accepted MVP/store capability.

Version 0.5 user-directed scope change: Home Call opens an internal manual dial pad and supports explicitly confirmed ACTION_CALL with just-in-time CALL_PHONE consent. It retains the existing phone app for ongoing/incoming calls and uses ACTION_DIAL for emergency paths. This is not a full default-dialer/InCallService implementation; see [DIALER_FEATURE.md](docs/DIALER_FEATURE.md).

## Recommendation

Plan for a 12-week, offline-first native Android MVP, with a prototype validation gate at the end of week 2. Build only the home screen, photo-contact calling, and simplified app selection for that gate. Continue to full implementation if testing demonstrates easier, more independent use; otherwise revise the interaction design before expanding scope.

This is a delivery plan, not a fixed-price commitment. The repository currently contains the RFP only. No application implementation is assumed. Weeks are relative to project kickoff; recruitment, device access, and publishing accounts must be arranged early.

## 1. Scope and decisions

MVP includes all P0 requirements FR01–FR14: Home launcher selection, six to eight configurable primary actions, four to nine photo contacts, confirmed calling, favorite apps, three size levels, optional family PIN, Persian/RTL/Solar Hijri dates, help calling, guided setup, reset and exit, offline use, readable app directory, and illustrated offline help.

Recommended interpretations to confirm during discovery:

- **Simple mode only in MVP.** The onboarding journey mentions Ultra-simple mode, but FR19 assigns it to release 1.1. Prototype it only if research needs it; production implementation remains P1.
- **Open existing apps for messages, camera, photos, and WhatsApp.** The launcher does not replace their interfaces. Show a clear explanation when an app is missing or cannot handle the action.
- **Use the system dialer initially.** Show the selected person's name and number before opening the dialer. Define “initiate a call” as reaching the dialer with the correct number; measure actual connection separately. Direct calling is an optional later decision, not a dependency for MVP.
- **Count taps per journey.** The RFP's Family → contact → confirmation path exceeds its general one-to-two-tap goal. Keep confirmation for error prevention; compare direct home photo shortcuts with the Family screen during prototype testing and agree a measurable task-specific target.
- **Family assistance is local and consent-based.** The PIN protects in-app configuration against accidental changes. App filtering does not enforce a device-wide block. Exit instructions remain accessible, including when the PIN is forgotten.
- **No backend, registration, ads, or paid upgrade implementation in MVP.** Research willingness to pay without delaying core usability work.
- **Naming:** Hamrahak is the repository name; Hamrah Sadeh is the RFP's working product name. Confirm the Persian display name, application ID, and publishing owner before signing the first distributed build.

## 2. Delivery sequence

### Weeks 1–2: discovery and prototype validation

Owner: product/UX lead with Android developer.

1. Recruit 10–15 Persian-speaking older adults across differing visual, motor, literacy, and smartphone experience levels; involve family helpers with consent.
2. Inventory representative phones, Android versions, navigation modes, connectivity, and commonly used apps. Secure at least six physical devices from three manufacturers for later acceptance.
3. Observe the same core tasks on users' current home screens: call a relative, find an app, and return Home. Record completion, time, mis-taps, and assistance.
4. Test a Persian interactive prototype for home, photo calling, and app selection. Compare wording, layout, sizes, and call confirmation; use paper/Figma first and a small Android spike where system interaction matters.
5. Spike default-Home selection, Android 9 fallback, launching apps, package visibility, contacts/photo access, dialer handoff, and return-from-app behavior on different manufacturers.
6. Agree minimum supported devices, permission strategy, measurement definitions, privacy approach, and the final MVP boundary.

Deliverables: research notes, task baseline, prototype, feasibility notes, requirement backlog, device matrix, and revised estimate.

**Gate A:** Review evidence against baseline. Proceed if users show improved independent task completion without recurring critical confusion. This is a proposed decision rule, not a claimed RFP numerical threshold. Revise and retest if results are inconclusive; do not automatically consume the remaining budget.

### Week 3: design and engineering foundation

Owner: UX designer and Android developer.

1. Finalize Persian terminology, RTL layouts, three size presets, high contrast, TalkBack order, and visible navigation.
2. Specify empty, loading, missing-app, missing-photo, denied-permission, invalid-number, forgotten-PIN, and reset states.
3. Complete clickable onboarding, family settings, calling, help, and exit flows; hand off reusable Figma components.
4. Create the Android project, build configuration, CI checks, local data schema, platform adapters, and a traceable P0 backlog.
5. Produce an installable shell that can become Home and safely return to the previous launcher.

**Gate B:** Approved MVP flows and an installable Home-role shell on an older and a recent reference device. No unresolved feasibility blocker for P0 work.

### Weeks 4–6: core launcher vertical slices

Owner: Android developer, with early QA and UX review.

- **Week 4 — home and app launch:** home layout, Persian dates/time, favorite shortcuts, readable app directory, customization, persistence, missing/uninstalled app behavior. Covers FR02, FR05, FR08, FR13 and the main FR01 integration.
- **Week 5 — family and calling:** four to nine contacts, manual entry, optional contact import, photo selection, number selection/validation, named call confirmation, dialer handoff, and trusted help contact. Covers FR03, FR04, FR09.
- **Week 6 — configuration and accessibility:** three size presets with preview, family settings, optional PIN, reset behavior, TalkBack semantics, focus order, large-font layout, and process-death/reboot persistence. Covers FR06, FR07 and part of FR11.

**Gate C:** A family can configure a working phone, and an older adult can open a favorite app and reach the correct contact's dialer offline. Review on physical devices each week.

### Weeks 7–8: complete and harden MVP

Owner: Android developer, UX/content designer, QA.

1. Finish onboarding with deferred permissions and understandable Home selection guidance: FR10.
2. Add illustrated, bundled Persian tutorials with a clear return-home path: FR14.
3. Finish reset, PIN recovery policy, and manufacturer-aware instructions for replacing the launcher: FR11.
4. Verify every core journey without internet and without an account: FR12.
5. Handle revoked permissions, deleted source contacts, unavailable photo URIs, changed phone numbers, removed apps, cancelled system dialogs, and interrupted onboarding.
6. Run the complete P0 regression and prepare the first release candidate.

**Gate D:** FR01–FR14 are demonstrable; no unresolved critical failure in Home, calling, help, persistence, or exit.

### Weeks 9–10: usability acceptance and compatibility

Owner: research/UX lead and QA; Android developer fixes findings.

1. Run structured acceptance with at least 15 older adults without step-by-step prompting. This can include returning discovery participants, but record prior exposure and include first-time users where possible.
2. Test at least six physical devices across three manufacturers, including budget and mid-range phones, Android 9, recent Android versions, gesture navigation, and three-button navigation.
3. Check largest text preset combined with system font scaling, TalkBack, Persian numerals and mixed-direction phone numbers, offline operation, low memory, process recreation, reboot, and app updates.
4. Measure performance and stability with a defined test protocol. Fix critical issues first, then repeat affected journeys and regressions.

**Gate E:** Meet the acceptance criteria below, or explicitly record a revised plan and obtain product-owner acceptance of any changed target. Do not silently treat missed targets as passed.

### Weeks 11–12: pilot, packaging, and handover

Owner: Android developer and product/release owner.

1. Distribute a signed pilot APK; observe real usage with consent and a documented support channel.
2. Resolve pilot defects and verify upgrade migration and recovery on the release candidate.
3. Produce signed APK/AAB, store assets, permission rationale, privacy policy, architecture/setup docs, Persian guides, test reports, and usability findings.
4. Confirm source/design ownership, application ID, signing-key custody, publishing accounts, and store-specific submission requirements.
5. Hand over Git history, Figma files, reproducible build instructions, release notes, and secure signing documentation.

**Gate F:** Accepted pilot and complete handover. Store review timing is external to the 12-week engineering estimate. Begin the proposed 30-day post-acceptance defect-fix period under agreed severity and response terms.

## 3. Technical implementation

Use Kotlin, Jetpack Compose, and customized Material 3. Start with one application module and clear feature packages; extract Gradle modules only when isolation materially helps. Suggested packages: `home`, `contacts`, `apps`, `help`, `settings`, `onboarding`, `data`, `platform`, and shared `ui`.

- Compose screens expose state through ViewModels; repositories own local data; platform adapters isolate Home role, app discovery, contacts, photo access, and dialer behavior.
- Room stores selected contact snapshots and ordered app shortcuts. DataStore holds size, layout, onboarding, and display preferences. Define schema migrations before distributed builds.
- Persist only selected contact fields. Support manual entry when contacts access is declined. Store selected photos in app-private storage where needed, with placeholders and documented cleanup behavior.
- Never store a plaintext PIN; use a salted password-derived verifier and retry throttling. Use Android Keystore for cryptographic keys when required. Document reset and backup behavior; the PIN does not restrict Android settings or replace device security.
- Minimum Android 9 / API 28 is provisional pending the device audit. Request Home role on supported versions and provide a tested Android 9 system-settings path. RoleManager's request API starts at API 29: [official documentation](https://developer.android.com/reference/android/app/role/RoleManager).
- Target API 36 or later as currently required for new Play submissions; recheck at release: [Google Play target API requirements](https://support.google.com/googleplay/android-developer/answer/11926878?hl=en).
- Discover launchable apps using official APIs and narrowly scoped visibility declarations. Validate the directory on real devices before considering broad package access: [Android package visibility](https://developer.android.com/training/package-visibility).
- Avoid SMS, call-log, notification-listener, location, Accessibility Service, and direct-call permissions in the baseline MVP. Ask for contacts/photo access only at the relevant action; validate platform photo-selection behavior across supported versions.
- No always-running background service. No scheduling infrastructure until reminder scope is approved. Keep essential app assets bundled and do not upload contact data or phone numbers.

## 4. Acceptance and measurement

Maintain one checklist linking FR01–FR14 and NFR01–NFR10 to a demo, test case, device evidence, and status.

Functional release gates:

- Explicitly selected as default Home; Home returns to the launcher after opening other apps and after reboot.
- Correct selected contact name and number shown before dialer handoff; help uses the configured trusted person.
- Four to nine contacts, six to eight main actions, app shortcuts, ordering, and all three size settings persist across restart and update.
- Core tasks work without internet. Telephone service is still needed to connect a normal phone call; offline acceptance measures launcher behavior and dialer handoff separately.
- Permission denial, missing apps, invalid data, and cancelled setup remain understandable and recoverable.
- PIN protects advanced settings when enabled; reset and restoring another launcher remain documented and possible.
- Largest text setting and system font scaling leave essential controls reachable; core tasks are usable with TalkBack.
- No personal contact data leaves the phone. Review logs, crash reports, backup configuration, and any included SDKs.

Usability gates from the RFP, using at least 15 participants:

- Favorite contact calling success: at least 90% (at least 14 of 15).
- Requested app discovery success: at least 85% (at least 13 of 15).
- Return to Home success: at least 90% (at least 14 of 15).
- Median time to initiate calling: no more than 30 seconds, with start/end events defined before testing.
- Family-assisted setup: under five minutes, excluding download/install and third-party sign-in; report median and distribution because the RFP does not define an aggregate pass rule.

Reliability and performance gates:

- At least 99.5% crash-free pilot sessions. Define session boundaries, consent-based collection, observation window, and a minimum usable sample before pilot; report numerator and denominator. A tiny crash-free sample alone is insufficient evidence of stability.
- Home visible within two seconds of process readiness on an agreed budget reference device. Define “visible” and readiness consistently; also record cold startup and return-to-Home timings so slow startup is not hidden by the metric.
- No unnecessary persistent background service; inspect idle battery behavior and investigate regressions against the agreed baseline.

Use focused automated tests for data migrations, settings persistence, contact/number handling, date formatting, and PIN behavior; Compose/instrumentation tests for critical in-app flows. Use physical-device manual tests for Home-role dialogs, OEM behavior, actual dialer integration, and reboot. Track release blockers by severity and retain evidence with the build identifier.

## 5. Staffing, effort, and dependencies

The RFP estimates **370–570 total person-hours**, comprising discovery 30–50, design 40–70, implementation 200–300, testing/fixes 80–120, and release/docs 20–30. Treat these as preliminary cross-role totals, not all developer hours. At 12 weeks this is approximately 31–48 aggregate hours per week, with QA and design effort concentrated at particular milestones.

Recommended team: one experienced Android developer, part-time Persian UX/research designer, part-time QA with physical-device access, and a named product owner who resolves scope and recruits participants. Re-estimate after Gate A. Price participant recruitment, devices, design tools, publishing accounts, and support separately. Protect weeks 11–12 for pilot and fixes; they are not spare capacity for P1 features.

Critical dependency chain: participant/device access → validated prototype → approved flows and platform spike → persistent core journeys → complete P0 build → acceptance testing → pilot and handover.

Primary risks and responses:

- **Recruitment or device delays:** book participants and borrow/buy devices during week 1; move dates if evidence cannot be collected.
- **OEM differences:** test Home, navigation, and recovery from the first build rather than waiting until week 9.
- **Confusing labels or excessive taps:** compare actual tasks against baseline and revise before adding features.
- **Permission/store problems:** prefer system handoff, minimize permissions, and audit declarations before release.
- **PIN causes lockout:** agree recovery policy and test the owner exit path before pilot.
- **Scope expansion:** keep FR15–FR25 in a separate backlog; any promotion changes effort and acceptance explicitly.
- **Small pilot hides reliability issues:** disclose sample size and extend observation when confidence is insufficient.

## 6. After MVP

Prioritize release 1.1 using observed needs rather than implementing all P1 items at once: Ultra-simple mode, local backup/restore, Persian voice feedback, supported internet-calling shortcuts, medication reminders, and simplified notifications. Assess each feature's permissions, accessibility, scheduling constraints, testing cost, and offline behavior separately.

Remote family configuration, accounts/synchronization, and AI guidance require their own discovery, consent/security model, architecture, estimate, and approval. They are excluded from the MVP schedule.

## 7. First actionable backlog

1. Confirm product owner, naming, application ownership, and available weekly staffing.
2. Recruit discovery participants and reserve the six-device test pool.
3. Create the task-observation script and baseline measurements.
4. Design the Persian three-flow prototype and test it.
5. Run the Home/app-discovery/contacts/dialer platform spike.
6. Review Gate A evidence and lock the MVP backlog and acceptance definitions.
7. Initialize the production Android project after the validation decision.

The original planning change did not include application work. Execution now starts with the limited discovery prototype above; no production publishing or Gate A acceptance has occurred.
