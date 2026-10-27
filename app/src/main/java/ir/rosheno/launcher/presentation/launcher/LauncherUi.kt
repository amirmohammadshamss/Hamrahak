package ir.rosheno.launcher.presentation.launcher

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.platform.PlatformActions
import ir.rosheno.launcher.presentation.apps.AppDirectory
import ir.rosheno.launcher.presentation.common.resolve
import ir.rosheno.launcher.presentation.components.Action
import ir.rosheno.launcher.presentation.components.Body
import ir.rosheno.launcher.presentation.components.Empty
import ir.rosheno.launcher.presentation.components.LocalActionTextSize
import ir.rosheno.launcher.presentation.components.PhoneNumber
import ir.rosheno.launcher.presentation.components.Title
import ir.rosheno.launcher.presentation.contacts.PhoneContactsScreen
import ir.rosheno.launcher.presentation.dialer.DialerScreen
import ir.rosheno.launcher.presentation.family.ContactEditorDialog
import ir.rosheno.launcher.presentation.family.ContactRow
import ir.rosheno.launcher.presentation.help.HelpAction
import ir.rosheno.launcher.presentation.help.HelpContactSetup
import ir.rosheno.launcher.presentation.help.VisualHelp
import ir.rosheno.launcher.presentation.home.Home
import ir.rosheno.launcher.presentation.photos.CameraScreen
import ir.rosheno.launcher.presentation.photos.PhotoLibraryScreen
import ir.rosheno.launcher.presentation.photos.PhotoViewerScreen
import ir.rosheno.launcher.presentation.settings.Settings
import ir.rosheno.launcher.presentation.setup.Welcome
import ir.rosheno.launcher.presentation.sms.SmsScreen
import ir.rosheno.launcher.presentation.theme.Muted
import ir.rosheno.launcher.presentation.theme.Paper
import ir.rosheno.launcher.presentation.theme.RoshenoTheme

@Composable
fun LauncherApp(model: LauncherViewModel, platform: PlatformActions, homeSignal: Int, resumed: Int,
    navigation: LauncherNavigationViewModel = viewModel()) {
    val context = LocalContext.current
    val state by model.state.collectAsStateWithLifecycle()
    val nav by navigation.state.collectAsStateWithLifecycle()
    val screen = nav.screen
    val calling = state.config.contacts.find { it.id == nav.callingId }
    var homeSelected by remember { mutableStateOf(false) }
    LaunchedEffect(resumed) { homeSelected = platform.isHome() }
    LaunchedEffect(homeSignal) { navigation.homeSignal(homeSignal) }
    LaunchedEffect(state.config, state.contactSaveToken) {
        navigation.configurationChanged(state.config, state.contactSaveToken)
    }
    BackHandler(onBack = navigation::back)
    RoshenoTheme {
        CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl,
            LocalActionTextSize provides (22 + state.config.size * 3)) {
            Surface(color = Paper, modifier = Modifier.fillMaxSize()) {
                if (state.loading) Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                } else Column(Modifier.fillMaxSize().safeDrawingPadding()) {
                    Row(Modifier.fillMaxWidth().testTag("page-header").padding(horizontal = 20.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically) {
                        Image(painterResource(R.drawable.ic_launcher), contentDescription = null,
                            modifier = Modifier.size(44.dp))
                        Spacer(Modifier.width(12.dp))
                        Column(Modifier.weight(1f)) {
                            Text(context.getString(R.string.brand_name), fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(if (!state.config.setupComplete) context.getString(R.string.setup_title) else when (screen) {
                                "dialer" -> context.getString(R.string.page_dialer)
                                "contacts" -> context.getString(R.string.page_contacts)
                                "messages" -> context.getString(R.string.page_messages)
                                "camera" -> context.getString(R.string.page_camera)
                                "photos" -> context.getString(R.string.page_photos)
                                "photo-view" -> context.getString(R.string.page_photo_view)
                                "family" -> context.getString(R.string.page_family)
                                "apps" -> context.getString(R.string.page_apps)
                                "settings" -> context.getString(R.string.settings_title)
                                "help" -> context.getString(R.string.page_guide)
                                "help-contact" -> context.getString(R.string.page_help_contact)
                                else -> context.getString(R.string.brand_tagline)
                            }, color = Muted, fontSize = 14.sp)
                        }
                        if (state.config.setupComplete) {
                            if (screen != "home") TextButton(onClick = { navigation.navigate("home") }) {
                                Text(context.getString(R.string.home_label), fontSize = 18.sp)
                            }
                            if (screen != "settings") TextButton(onClick = { navigation.navigate("settings") }) {
                                Text(context.getString(R.string.settings_label), fontSize = 18.sp)
                            }
                        }
                    }
                    AnimatedContent(targetState = screen, modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 40 }) togetherWith fadeOut(tween(100))
                        }, label = "page-transition") { page ->
                        key(page, state.config.setupComplete) {
                            when (page) {
                                "dialer" -> DialerScreen(nav.dialedNumber, navigation::dialNumber, state.config.contacts, state.config.size,
                                    onHome = { navigation.navigate("home") }, onCall = platform::startUserCall, onSystemDial = platform::dial)
                                "contacts" -> PhoneContactsScreen(resumed, state.config.size, onHome = { navigation.navigate("home") },
                                    onCall = platform::startUserCall, open = platform::open)
                                "messages" -> SmsScreen(state.config.contacts, state.config.size, onHome = { navigation.navigate("home") }, openSystemMessages = platform::messages)
                                "camera" -> CameraScreen(onHome = { navigation.navigate("home") }, onCaptured = navigation::photoCaptured)
                                "photos" -> PhotoLibraryScreen(onHome = { navigation.navigate("home") }, onCamera = { navigation.navigate("camera") },
                                    onPhoto = navigation::viewPhoto)
                                "photo-view" -> PhotoViewerScreen(nav.viewingPhoto, onHome = { navigation.navigate("home") }, onPhotos = { navigation.navigate("photos") })
                                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (state.storageError) {
                                    Title(context.getString(R.string.storage_error_title))
                                    Body(context.getString(R.string.storage_error_description))
                                    Action(context.getString(R.string.action_choose_other_home), platform::homeSettings)
                                    Action(context.getString(R.string.action_reset_rosheno), { navigation.confirmReset(true) }, secondary = true)
                                } else if (!state.config.setupComplete) {
                                    Welcome(state.config, homeSelected, platform, model, { navigation.editContact() })
                                } else when (page) {
                                    "home" -> Home(state, homeSelected, { navigation.navigate(it) }, { navigation.callContact(it) }, platform)
                                    "family" -> {
                                        Title(context.getString(R.string.family_title))
                                        Body(context.getString(R.string.family_call_description))
                                        if (state.config.contacts.isEmpty()) Empty(context.getString(R.string.family_empty))
                                        state.config.contacts.forEach { c -> ContactRow(c, { navigation.callContact(c.id) }) }
                                        if (state.config.contacts.size < 9) Action(context.getString(R.string.action_add_person), { navigation.editContact() }, secondary = true)
                                    }
                                    "apps" -> AppDirectory(state, model, platform)
                                    "settings" -> Settings(state, model, platform, homeSelected,
                                        { navigation.editContact() }, { contact -> navigation.editContact(contact.id) }, { navigation.confirmReset(true) })
                                    "help" -> VisualHelp()
                                    "help-contact" -> HelpContactSetup(state.config,
                                        choose = { id -> navigation.selectHelper(id); model.setHelp(id) },
                                        add = { navigation.editContact() })
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            }
                        }
                    }
                    if (state.config.setupComplete && !state.storageError && screen == "home") {
                        val trusted = state.config.contacts.find { it.id == state.config.helpContactId }
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            HelpAction(trusted) { if (trusted != null) navigation.callContact(trusted.id) else navigation.navigate("help-contact") }
                        }
                    }
                }
            }
            if (nav.adding) key(nav.editingId) { ContactEditorDialog(
                initial = state.config.contacts.find { it.id == nav.editingId },
                saving = state.savingContact,
                session = nav.editorSession,
                onDismiss = navigation::dismissEditor,
                onSave = { name, number, photo -> model.saveContact(nav.editingId, name, number, photo) },
                onError = model::message,
            ) }
            calling?.let { contact -> AlertDialog(
                onDismissRequest = { navigation.callContact(null) },
                title = { Title(context.getString(R.string.call_contact_title, contact.name)) },
                text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhoneNumber(contact.number)
                    Body(context.getString(R.string.family_call_confirmation_description))
                } },
                confirmButton = { TextButton(onClick = { navigation.callContact(null); platform.dial(contact.number) }) {
                    Text(context.getString(R.string.action_go_to_phone), fontSize = LocalActionTextSize.current.sp)
                } },
                dismissButton = { TextButton(onClick = { navigation.callContact(null) }) { Text(context.getString(R.string.action_cancel), fontSize = 20.sp) } },
            ) }
            if (nav.reset) AlertDialog(onDismissRequest = { navigation.confirmReset(false) },
                title = { Title(context.getString(R.string.reset_confirmation_title)) },
                text = { Body(context.getString(R.string.reset_confirmation_description)) },
                confirmButton = { TextButton(onClick = { navigation.confirmReset(false); model.reset(); navigation.navigate("home") }) {
                    Text(context.getString(R.string.action_clear_settings), fontSize = 20.sp)
                } },
                dismissButton = { TextButton(onClick = { navigation.confirmReset(false) }) { Text(context.getString(R.string.action_cancel), fontSize = 20.sp) } })
            state.message?.let { message -> AlertDialog(onDismissRequest = { model.message(null) },
                title = { Title(context.getString(R.string.app_message_title)) }, text = { Body(message.resolve(context)) },
                confirmButton = { TextButton(onClick = { model.message(null) }) { Text(context.getString(R.string.action_understood), fontSize = 22.sp) } }) }
        }
    }
}
