# Application architecture

The app uses clean architecture packages inside the existing Android module.
Source packages live under `app/src/main/java/ir/rosheno/launcher/`.

## Package responsibilities

- `domain/model`: framework-independent contacts, launcher configuration, app identifiers, and messages.
- `domain/repository`: interfaces for configuration, app discovery, contact photos, camera captures, phone contacts, and messages.
- `domain/usecase`: contact-save transactions, configuration persistence with photo cleanup, and reset.
- `domain/launcher`, `domain/phone`, `domain/sms`: configuration policies, phone normalization, SMS segmentation, and conversation grouping.
- `data`: Android implementations using SharedPreferences, JSON, Contacts/SMS providers, PackageManager, private files, and SMS transport.
- `presentation`: Compose screens and ViewModels grouped by feature, plus shared components and theme.
- `platform`: Android intent, Home-role, and emergency-dialing integration.
- `di`: `AppContainer` connects repository interfaces to Android implementations.

`MainActivity` and `SmsStatusReceiver` remain in the root package as Android entry
points. Their component names, the application ID, stored configuration format,
preference names, private-storage directories, and SMS callback action remain stable.

## Dependency direction

The domain imports only Kotlin, Java, and other domain packages. Data implements
domain interfaces and never imports presentation. Presentation consumes domain
interfaces and use cases; default construction is delegated to `AppContainer`.
Android permissions, lifecycle events, CameraX preview binding, and bitmap rendering
stay at the Android boundary. ViewModels retain Android lifecycle integration.

`LauncherViewModel` serializes writes and exposes UI state. `SaveFamilyContact`
commits a selected photo, saves the configuration, and rolls back a newly committed
photo if persistence fails. Successful writes clean up obsolete photos. Cleanup
failure does not report an already persisted configuration as unsaved.

App identifiers are flattened component strings in the domain; Android adapters
convert those strings to `ComponentName` when discovering or rendering apps.
`SmsViewModel` observes reports through `MessageRepository`, without depending on
SharedPreferences or the concrete send journal.

The project retains one Gradle module. Package dependency checks enforce these
boundaries without introducing additional build modules or a DI framework.

## MVVM presentation

Compose screens collect immutable `StateFlow` state with `collectAsStateWithLifecycle`
and forward actions to ViewModels. Feature ViewModels have no Compose dependencies.

- `ContactsViewModel` owns provider loading/observation, search, contact creation drafts, save results, and call confirmation. Provider observers are released when the screen stops.
- `ContactEditorViewModel` owns the family-contact form and photo staging. A saved editor-session ID preserves drafts through recreation and resets them for a new edit. Cancelled staging cleans up its newly created draft.
- `PhotoLibraryViewModel` owns loading, failures, and retries. `CameraViewModel` owns capture preparation, duplicate-dispatch prevention, results, and failed-file cleanup.
- `LauncherNavigationViewModel` owns page history, editor sessions, selected photos, dial drafts, help-contact selection, and launcher dialogs. Home-key returns clear navigation and pending launcher actions.
- `DialerViewModel`, `SettingsViewModel`, `AppDirectoryViewModel`, and `HelpViewModel` own confirmation, search, and guide state. `HomeViewModel` publishes clock updates while observed.
- `SmsViewModel` owns inbox/composer navigation, confirmation snapshots, recipient selection, draft replacement, and send state.

`SavedStateHandle` stores small form and navigation values. Provider contents,
bitmaps, and active jobs are reloaded rather than serialized. New feature ViewModels
receive domain repository interfaces through `FeatureViewModelFactory`; worker
dispatchers can be injected for deterministic tests.

Android permission launchers, lifecycle adapters, CameraX preview binding, animations,
and bitmap rendering stay in the View. Camera capture paths are consumed before
dispatch so recomposition cannot start the same capture twice. Permission grants
open call confirmation; only a separate explicit confirmation dispatches a call.

Contact calling and manual dialing share `CallConfirmationPolicy` and its immutable
state while retaining their saved-state keys. `ReturnHomeButton` supplies the common
navigation control, including the contacts screen's outlined variant. Bounded
bitmap decoding lives in `platform/BitmapDecoder`; the existing contact, thumbnail,
and viewer size limits are passed by callers. Private photo deletion uses the
shared canonical-directory ownership check in `data/photos/OwnedFiles`.

User-facing copy lives in `res/values/strings.xml`, including positional format
arguments for names and counts. ViewModels expose resource-backed `UiText` messages
that the View resolves with its current context. The shared palette lives in
`res/values/colors.xml`; Compose theme getters and vector drawables use those
resources. Navigation keys, provider columns, and other internal protocol strings
remain code constants.

## Validation

```sh
# Architecture boundaries and emulator-runner checks
python3 -m unittest discover -s scripts -p '*_test.py'

# JVM domain and ViewModel regression tests
./gradlew :app:testDebugUnitTest

# Android sources, instrumented test sources, and lint
./gradlew :app:compileDebugKotlin :app:compileDebugAndroidTestKotlin :app:lintDebug
```

Domain tests cover contact limits, save rollback, shared-photo retention, reset
ordering, phone normalization, SMS encoding boundaries, and conversation ordering.
The existing [instrumented suite](testing/AUTOMATED_TESTS.md) covers Android behavior.
