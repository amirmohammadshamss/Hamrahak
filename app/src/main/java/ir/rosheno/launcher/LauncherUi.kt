package ir.rosheno.launcher

import android.graphics.BitmapFactory
import android.icu.text.DateFormat
import android.icu.util.ULocale
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Date

private val LocalActionTextSize = compositionLocalOf { 22 }

@Composable
fun LauncherApp(model: LauncherViewModel, platform: PlatformActions, homeSignal: Int, resumed: Int) {
    val state by model.state.collectAsStateWithLifecycle()
    var screen by rememberSaveable { mutableStateOf("home") }
    var pageHistory by rememberSaveable { mutableStateOf(arrayListOf<String>()) }
    fun navigate(destination: String) {
        if (destination == "home") pageHistory = arrayListOf()
        else if (destination != screen) pageHistory = ArrayList(pageHistory + screen)
        screen = destination
    }
    var adding by rememberSaveable { mutableStateOf(false) }
    var editingId by rememberSaveable { mutableStateOf<String?>(null) }
    var callingId by rememberSaveable { mutableStateOf<String?>(null) }
    var reset by rememberSaveable { mutableStateOf(false) }
    var viewingPhoto by rememberSaveable { mutableStateOf<String?>(null) }
    var dialedNumber by rememberSaveable { mutableStateOf("") }
    var pendingHelpId by rememberSaveable { mutableStateOf<String?>(null) }
    val calling = state.config.contacts.find { it.id == callingId }
    var homeSelected by remember { mutableStateOf(false) }
    LaunchedEffect(resumed) { homeSelected = platform.isHome() }
    var previousHomeSignal by rememberSaveable { mutableIntStateOf(homeSignal) }
    var observedSaveToken by rememberSaveable { mutableStateOf<String?>(null) }
    LaunchedEffect(homeSignal) {
        if (homeSignal != previousHomeSignal) {
            navigate("home"); adding = false; editingId = null; callingId = null; reset = false; pendingHelpId = null
            previousHomeSignal = homeSignal
        }
    }
    LaunchedEffect(state.contactSaveToken) {
        state.contactSaveToken?.let { token -> if (token != observedSaveToken) {
            adding = false; editingId = null; observedSaveToken = token
        } }
    }
    LaunchedEffect(state.config.helpContactId, pendingHelpId, screen) {
        pendingHelpId?.let { id ->
            if (screen == "help-contact" && state.config.helpContactId == id && state.config.contacts.any { it.id == id }) {
                pendingHelpId = null
                navigate("home")
                callingId = id
            }
        }
    }
    LaunchedEffect(screen) { if (screen != "help-contact") pendingHelpId = null }
    // A launcher should not disappear when Back is pressed at its root.
    BackHandler {
        screen = pageHistory.lastOrNull() ?: "home"
        pageHistory = ArrayList(pageHistory.dropLast(1))
    }
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
                            Text("روشنو", fontSize = 24.sp, fontWeight = FontWeight.Bold)
                            Text(if (!state.config.setupComplete) "راه‌اندازی روشنو" else when (screen) {
                                "dialer" -> "صفحهٔ شماره‌گیر"
                                "contacts" -> "صفحهٔ مخاطبین"
                                "messages" -> "صفحهٔ پیام‌ها"
                                "camera" -> "صفحهٔ دوربین"
                                "photos" -> "صفحهٔ عکس‌ها"
                                "photo-view" -> "نمایش عکس"
                                "family" -> "صفحهٔ خانواده"
                                "apps" -> "صفحهٔ برنامه‌ها"
                                "settings" -> "تنظیمات روشنو"
                                "help" -> "راهنمای روشنو"
                                "help-contact" -> "انتخاب مخاطب کمک"
                                else -> "روزهای ساده‌تر، کنار هم"
                            }, color = Muted, fontSize = 14.sp)
                        }
                        if (state.config.setupComplete) {
                            if (screen != "home") TextButton(onClick = { navigate("home") }) {
                                Text("خانه", fontSize = 18.sp)
                            }
                            if (screen != "settings") TextButton(onClick = { navigate("settings") }) {
                                Text("تنظیمات", fontSize = 18.sp)
                            }
                        }
                    }
                    AnimatedContent(targetState = screen, modifier = Modifier.weight(1f),
                        transitionSpec = {
                            (fadeIn(tween(180)) + slideInVertically(tween(220)) { it / 40 }) togetherWith fadeOut(tween(100))
                        }, label = "page-transition") { page ->
                        key(page, state.config.setupComplete) {
                            when (page) {
                                "dialer" -> DialerScreen(dialedNumber, { dialedNumber = it }, state.config.contacts, state.config.size,
                                    onHome = { navigate("home") }, onCall = platform::startUserCall, onSystemDial = platform::dial)
                                "contacts" -> PhoneContactsScreen(resumed, state.config.size, onHome = { navigate("home") },
                                    onCall = platform::startUserCall, open = platform::open)
                                "messages" -> SmsScreen(state.config.contacts, state.config.size, onHome = { navigate("home") }, openSystemMessages = platform::messages)
                                "camera" -> CameraScreen(onHome = { navigate("home") }, onCaptured = { path ->
                                    if (screen == "camera") { viewingPhoto = path; navigate("photo-view") }
                                })
                                "photos" -> PhotoLibraryScreen(onHome = { navigate("home") }, onCamera = { navigate("camera") },
                                    onPhoto = { viewingPhoto = it; navigate("photo-view") })
                                "photo-view" -> PhotoViewerScreen(viewingPhoto, onHome = { navigate("home") }, onPhotos = { navigate("photos") })
                                else -> Column(Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                if (state.storageError) {
                                    Title("تنظیمات خوانده نشد")
                                    Body("برای جلوگیری از تغییر اطلاعات، ذخیره‌سازی متوقف شده است. می‌توانید صفحهٔ اصلی قبلی را انتخاب کنید یا روشنو را بازنشانی کنید.")
                                    Action("انتخاب صفحهٔ اصلی دیگر", platform::homeSettings)
                                    Action("بازنشانی روشنو", { reset = true }, secondary = true)
                                } else if (!state.config.setupComplete) {
                                    Welcome(state.config, homeSelected, platform, model, { adding = true })
                                } else when (page) {
                                    "home" -> Home(state, homeSelected, { navigate(it) }, { callingId = it }, platform)
                                    "family" -> {
                                        Title("خانوادهٔ من")
                                        Body("برای تماس، نام یا عکس فرد را بزنید.")
                                        if (state.config.contacts.isEmpty()) Empty("هنوز کسی اضافه نشده است.")
                                        state.config.contacts.forEach { c -> ContactRow(c, { callingId = c.id }) }
                                        if (state.config.contacts.size < 9) Action("افزودن یک نفر", { adding = true }, secondary = true)
                                    }
                                    "apps" -> AppDirectory(state, model, platform)
                                    "settings" -> Settings(state, model, platform, homeSelected,
                                        { adding = true }, { contact -> editingId = contact.id; adding = true }, { reset = true })
                                    "help" -> Help()
                                    "help-contact" -> HelpContactSetup(state.config,
                                        choose = { id -> pendingHelpId = id; model.setHelp(id) },
                                        add = { adding = true })
                                }
                                if (page != "home" && state.config.setupComplete) {
                                    Action("بازگشت به خانه", { navigate("home") }, secondary = true)
                                }
                                Spacer(Modifier.height(4.dp))
                            }
                            }
                        }
                    }
                    if (state.config.setupComplete && !state.storageError && screen == "home") {
                        val trusted = state.config.contacts.find { it.id == state.config.helpContactId }
                        Box(Modifier.padding(horizontal = 20.dp, vertical = 8.dp)) {
                            HelpAction(trusted) { if (trusted != null) callingId = trusted.id else navigate("help-contact") }
                        }
                    }
                }
            }
            if (adding) key(editingId) { ContactEditorDialog(
                initial = state.config.contacts.find { it.id == editingId },
                saving = state.savingContact,
                onDismiss = { adding = false; editingId = null },
                onSave = { name, number, photo -> model.saveContact(editingId, name, number, photo) },
                onError = model::message,
            ) }
            calling?.let { contact -> AlertDialog(
                onDismissRequest = { callingId = null },
                title = { Title("تماس با ${contact.name}") },
                text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    PhoneNumber(contact.number)
                    Body("شماره در برنامهٔ تلفن باز می‌شود. برای تماس، دکمهٔ تماس آن برنامه را بزنید.")
                } },
                confirmButton = { TextButton(onClick = { callingId = null; platform.dial(contact.number) }) {
                    Text("رفتن به تلفن", fontSize = LocalActionTextSize.current.sp)
                } },
                dismissButton = { TextButton(onClick = { callingId = null }) { Text("انصراف", fontSize = 20.sp) } },
            ) }
            if (reset) AlertDialog(onDismissRequest = { reset = false },
                title = { Title("تنظیمات پاک شود؟") },
                text = { Body("فقط مخاطبان انتخاب‌شده، عکس مخاطبان و میان‌برهای روشنو پاک می‌شوند. اطلاعات اصلی گوشی پاک نمی‌شود.") },
                confirmButton = { TextButton(onClick = { reset = false; model.reset(); navigate("home") }) {
                    Text("پاک کردن تنظیمات", fontSize = 20.sp)
                } },
                dismissButton = { TextButton(onClick = { reset = false }) { Text("انصراف", fontSize = 20.sp) } })
            state.message?.let { message -> AlertDialog(onDismissRequest = { model.message(null) },
                title = { Title("پیام روشنو") }, text = { Body(message) },
                confirmButton = { TextButton(onClick = { model.message(null) }) { Text("متوجه شدم", fontSize = 22.sp) } }) }
        }
    }
}

@Composable private fun Welcome(config: LauncherConfig, homeSelected: Boolean, platform: PlatformActions,
    model: LauncherViewModel, add: () -> Unit) {
    Title("به روشنو خوش آمدید")
    Body("با کمک یکی از نزدیکان، گوشی را برای خودتان آماده کنید. اطلاعات شما روی همین گوشی می‌ماند.")
    Surface(shape = RoundedCornerShape(24.dp), color = Pale) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Title("۱. صفحهٔ اصلی شما")
            Body(if (homeSelected) "روشنو صفحهٔ اصلی گوشی شماست." else "اگر مایلید، روشنو را صفحهٔ اصلی گوشی کنید. هر وقت خواستید می‌توانید آن را عوض کنید.")
            Action(if (homeSelected) "بررسی صفحهٔ اصلی" else "انتخاب روشنو", platform::chooseHome)
        }
    }
    Title("۲. خانواده و نزدیکان")
    Body(if (config.contacts.isEmpty()) "نام، شماره و عکس یک نفر از نزدیکان را اضافه کنید." else "${persianDigits(config.contacts.size.toString())} نفر اضافه شده است. می‌توانید بعداً افراد بیشتری اضافه کنید.")
    if (config.contacts.size < 9) Action("افزودن یک نفر", add, secondary = true)
    Title("۳. اندازهٔ نوشته‌ها")
    SizePicker(config.size, model::setSize)
    Action("شروع استفاده", model::completeSetup)
    TextButton(onClick = platform::homeSettings) { Text("انتخاب صفحهٔ اصلی دیگر", fontSize = 18.sp) }
}

@Composable private fun Home(state: LauncherState, homeSelected: Boolean, navigate: (String) -> Unit,
    call: (String) -> Unit, platform: PlatformActions) {
    var now by remember { mutableStateOf(Date()) }
    LaunchedEffect(Unit) { while (true) { now = Date(); delay(15_000) } }
    val time = remember(now) { DateFormat.getInstanceForSkeleton("Hm", ULocale("fa_IR")).format(now) }
    val date = remember(now) { DateFormat.getInstanceForSkeleton("yMMMMEEEEd", ULocale("fa_IR@calendar=persian")).format(now) }
    Surface(color = Green, shape = RoundedCornerShape(28.dp), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(horizontal = 20.dp, vertical = 20.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("امروز", fontSize = 16.sp, color = Color(0xFFD5E7DA))
            Text(time, fontSize = 52.sp, fontWeight = FontWeight.Medium, color = Color.White,
                style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
            Text(date, fontSize = 18.sp, color = Color.White, textAlign = TextAlign.Center)
        }
    }
    if (!homeSelected) Surface(color = Pale, shape = RoundedCornerShape(16.dp),
        modifier = Modifier.fillMaxWidth().clickable(onClick = platform::chooseHome)) {
        Text("برای بازگشت آسان، روشنو را صفحهٔ اصلی کنید", Modifier.padding(16.dp), fontSize = 18.sp)
    }
    if (state.config.contacts.isNotEmpty()) {
        Title("تماس سریع")
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            state.config.contacts.take(2).forEach { contact ->
                Surface(onClick = { call(contact.id) }, shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.weight(1f).heightIn(min = 112.dp), color = Pale, border = BorderStroke(1.dp, Hairline)) {
                    Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        ContactPhoto(contact.photo, contact.name)
                        Text(contact.name, fontSize = LocalActionTextSize.current.sp,
                            fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    }
                }
            }
        }
    }
    val tiles = listOf(
        Tile("تماس", "phone", { navigate("dialer") }),
        Tile("مخاطبین", "family", { navigate("contacts") }),
        Tile("خانوادهٔ من", "family", { navigate("family") }),
        Tile("پیام‌ها", "message", { navigate("messages") }),
        Tile("عکس‌ها", "photo", { navigate("photos") }),
        Tile("برنامه‌ها", "apps", { navigate("apps") }),
        Tile("دوربین", "camera", { navigate("camera") }),
    )
    tiles.chunked(2).forEach { row -> Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        row.forEach { tile -> HomeTile(tile, Modifier.weight(1f)) }
    } }
    if (state.config.favorites.isNotEmpty()) {
        Title("برنامه‌های دلخواه")
        state.config.favorites.forEach { key ->
            val app = state.apps.find { it.key == key }
            if (app != null) Surface(onClick = { platform.launch(key) }, shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, Hairline), modifier = Modifier.fillMaxWidth()) {
                Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppArtwork(app)
                    Text(app.label, fontSize = LocalActionTextSize.current.sp, modifier = Modifier.weight(1f))
                }
            } else Action("برنامه در دسترس نیست", { platform.launch(key) }, secondary = true)
        }
    }
    TextButton(onClick = { navigate("help") }, modifier = Modifier.fillMaxWidth()) {
        Text("راهنمای استفاده", fontSize = 20.sp)
    }
}

@Composable private fun HelpAction(trusted: FamilyContact?, click: () -> Unit) {
    Surface(onClick = click,
        color = Color(0xFFF5EADB), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Color(0xFFE9D9C6)),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.size(48.dp).background(Color(0xFFEED9C1), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center) { LauncherIcon("phone", Orange) }
            Column(Modifier.weight(1f)) {
                Text("کمک می‌خواهم", fontSize = LocalActionTextSize.current.sp, color = Orange, fontWeight = FontWeight.Bold)
                Text(trusted?.let { "تماس با ${it.name}" } ?: "ابتدا مخاطب کمک را انتخاب کنید", fontSize = 18.sp, color = Orange,
                    maxLines = 2, overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable private fun HelpContactSetup(config: LauncherConfig, choose: (String) -> Unit, add: () -> Unit) {
    Title("مخاطب کمک را انتخاب کنید")
    Body("دکمهٔ «کمک می‌خواهم» برای تماس با یکی از نزدیکان شماست. ابتدا فردی را انتخاب کنید که هنگام نیاز بتوانید با او تماس بگیرید.")
    if (config.contacts.isEmpty()) {
        Empty("هنوز نام و شمارهٔ کسی را اضافه نکرده‌اید.")
        Action("افزودن مخاطب کمک", add)
    } else {
        config.contacts.forEach { contact -> Surface(shape = RoundedCornerShape(20.dp)) {
            Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    ContactPhoto(contact.photo, contact.name)
                    Text(contact.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.weight(1f))
                }
                PhoneNumber(contact.number)
                Action("انتخاب برای کمک", { choose(contact.id) }, modifier = Modifier.testTag("choose-help-${contact.id}"), secondary = true)
            }
        } }
        if (config.contacts.size < 9) Action("افزودن مخاطب دیگر", add, secondary = true)
    }
    Body("پس از انتخاب، نام و شماره را می‌بینید. با زدن «رفتن به تلفن»، شماره‌گیر گوشی باز می‌شود؛ برای برقراری تماس، دکمهٔ تماس گوشی را بزنید.")
}

private data class Tile(val label: String, val icon: String, val click: () -> Unit)

@Composable private fun HomeTile(tile: Tile, modifier: Modifier = Modifier) {
    val interactions = remember { MutableInteractionSource() }
    val pressed by interactions.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, tween(100), label = "tile-press")
    val primary = tile.icon == "phone"
    val fill = if (primary) Green else Color.White
    val ink = if (primary) Color.White else Green
    Surface(onClick = tile.click, interactionSource = interactions, shape = RoundedCornerShape(24.dp),
        border = if (primary) null else BorderStroke(1.dp, Hairline), color = fill,
        modifier = modifier.scale(scale).heightIn(min = 136.dp)) {
        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterVertically)) {
            Box(Modifier.size(48.dp).background(if (primary) Color.White.copy(alpha = 0.12f) else Pale,
                RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) { LauncherIcon(tile.icon, ink) }
            Text(tile.label, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.SemiBold,
                color = ink, textAlign = TextAlign.Center)
        }
    }
}


@Composable private fun AppDirectory(state: LauncherState, model: LauncherViewModel, platform: PlatformActions) {
    var query by rememberSaveable { mutableStateOf("") }
    Title("برنامه‌ها")
    Body("نام برنامه را بزنید تا باز شود. برای دسترسی از خانه، آن را به دلخواه اضافه کنید؛ تا ۸ برنامه.")
    OutlinedTextField(value = query, onValueChange = { query = it }, label = { Text("جست‌وجوی برنامه") },
        modifier = Modifier.fillMaxWidth(), singleLine = true, textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp))
    val apps = state.apps.filter { it.label.contains(query, ignoreCase = true) }
    if (apps.isEmpty()) Empty("برنامه‌ای پیدا نشد.")
    apps.forEach { app -> Surface(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            TextButton(onClick = { platform.launch(app.key) }, modifier = Modifier.fillMaxWidth().testTag("app-${app.key}")) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    AppArtwork(app)
                    Text(app.label, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Start, modifier = Modifier.weight(1f))
                }
            }
            val selected = app.key in state.config.favorites
            OutlinedButton(onClick = { model.toggleFavorite(app.key) },
                enabled = selected || state.config.favorites.size < 8) {
                Text(if (selected) "حذف از دلخواه" else "افزودن به دلخواه", fontSize = 18.sp)
            }
        }
    } }
}

@Composable private fun Settings(state: LauncherState, model: LauncherViewModel, platform: PlatformActions,
    homeSelected: Boolean, add: () -> Unit, edit: (FamilyContact) -> Unit, reset: () -> Unit) {
    Title("تنظیمات خانواده")
    Body("تغییرها را با رضایت صاحب گوشی انجام دهید.")
    Title("اندازهٔ نوشته‌ها")
    SizePicker(state.config.size, model::setSize)
    Title("خانواده و تماس کمک")
    Body("برای دکمهٔ «کمک می‌خواهم»، یک فرد مورد اعتماد انتخاب کنید. این دکمه خدمات امدادی یا پاسخ قطعی نیست.")
    if (state.config.contacts.isEmpty()) Empty("ابتدا یک نفر را اضافه کنید.")
    state.config.contacts.forEachIndexed { index, c -> Surface(shape = RoundedCornerShape(20.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(c.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold)
            PhoneNumber(c.number)
            Action(if (c.id == state.config.helpContactId) "برای تماس کمک انتخاب شده" else "انتخاب برای تماس کمک",
                { model.setHelp(c.id) }, secondary = true)
            Action("ویرایش نام، شماره یا عکس", { edit(c) }, modifier = Modifier.testTag("edit-contact-${c.id}"), secondary = true)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { model.moveContact(c.id, -1) }, enabled = index > 0,
                    modifier = Modifier.weight(1f).testTag("contact-up-${c.id}")) { Text("بالاتر", fontSize = 18.sp) }
                OutlinedButton(onClick = { model.moveContact(c.id, 1) }, enabled = index < state.config.contacts.lastIndex,
                    modifier = Modifier.weight(1f).testTag("contact-down-${c.id}")) { Text("پایین‌تر", fontSize = 18.sp) }
            }
            var confirmDelete by remember(c.id) { mutableStateOf(false) }
            TextButton(onClick = { confirmDelete = true }) { Text("حذف از روشنو", fontSize = 18.sp) }
            if (confirmDelete) AlertDialog(onDismissRequest = { confirmDelete = false },
                title = { Title("${c.name} حذف شود؟") }, text = { Body("فقط از روشنو حذف می‌شود؛ مخاطب اصلی گوشی پاک نمی‌شود.") },
                confirmButton = { TextButton(onClick = { confirmDelete = false; model.removeContact(c.id) }) { Text("حذف") } },
                dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("انصراف") } })
        }
    } }
    if (state.config.contacts.size < 9) Action("افزودن یک نفر", add, secondary = true)
    if (state.config.favorites.isNotEmpty()) {
        Title("ترتیب برنامه‌های دلخواه")
        state.config.favorites.forEachIndexed { index, key ->
            Text(state.apps.find { it.key == key }?.label ?: "برنامه در دسترس نیست", fontSize = LocalActionTextSize.current.sp)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = { model.moveFavorite(key, -1) }, enabled = index > 0) { Text("بالاتر") }
                OutlinedButton(onClick = { model.moveFavorite(key, 1) }, enabled = index < state.config.favorites.lastIndex) { Text("پایین‌تر") }
                TextButton(onClick = { model.toggleFavorite(key) }) { Text("حذف") }
            }
        }
    }
    Title("صفحهٔ اصلی گوشی")
    Body(if (homeSelected) "روشنو صفحهٔ اصلی است." else "روشنو هنوز صفحهٔ اصلی گوشی نیست.")
    Action("انتخاب روشنو", platform::chooseHome, secondary = true)
    Body("برای خروج، دکمهٔ زیر را بزنید و صفحهٔ اصلی قبلی گوشی را انتخاب کنید. اطلاعات گوشی پاک نمی‌شود.")
    Action("انتخاب صفحهٔ اصلی دیگر", platform::homeSettings, secondary = true)
    HorizontalDivider()
    Body("نسخهٔ آزمایشی ۰.۴ · اطلاعات فقط روی این گوشی ذخیره می‌شود. قفل تنظیمات هنوز در این نسخه فعال نیست.")
    TextButton(onClick = reset) { Text("بازنشانی تنظیمات روشنو", fontSize = 20.sp, color = MaterialTheme.colorScheme.error) }
}

@Composable private fun SizePicker(selected: Int, change: (Int) -> Unit) {
    listOf("خوانا", "درشت", "خیلی درشت").forEachIndexed { index, label ->
        Surface(color = if (selected == index) Pale else Color.White,
            shape = RoundedCornerShape(16.dp), modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)
                .selectable(selected == index, role = Role.RadioButton, onClick = { change(index) })) {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected == index, onClick = null)
                Spacer(Modifier.width(12.dp))
                Text(label, fontSize = (22 + index * 3).sp, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable private fun Help() { VisualHelp() }

@Composable private fun ContactEditorDialog(initial: FamilyContact?, saving: Boolean,
    onDismiss: () -> Unit, onSave: (String, String, String?) -> Unit, onError: (String) -> Unit) {
    var name by rememberSaveable { mutableStateOf(initial?.name ?: "") }
    var number by rememberSaveable { mutableStateOf(initial?.number ?: "") }
    var photo by rememberSaveable { mutableStateOf(initial?.photo) }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val photos = remember(context) { ContactPhotos(context) }
    val scope = rememberCoroutineScope()
    val locked = busy || saving
    fun cancel() { if (!locked) { photos.discardDraft(photo); onDismiss() } }
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            busy = true
            scope.launch {
                val result = withContext(Dispatchers.IO) { runCatching { photos.stage(uri) } }
                busy = false
                result.onSuccess { path -> photos.discardDraft(photo); photo = path }
                    .onFailure { onError("عکس خوانده نشد. عکس دیگری انتخاب کنید یا بدون عکس ادامه دهید.") }
            }
        }
    }
    val validNumber = normalizePhone(number) != null
    AlertDialog(onDismissRequest = ::cancel,
        title = { Title(if (initial == null) "افزودن یکی از نزدیکان" else "ویرایش ${initial.name}") },
        text = { Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Body("نام و شماره را وارد کنید. عکس اختیاری است. دفترچهٔ تلفن گوشی تغییر نمی‌کند.")
            OutlinedTextField(name, { name = it.take(80) }, label = { Text("نام") }, singleLine = true,
                enabled = !locked, modifier = Modifier.fillMaxWidth(),
                textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp))
            OutlinedTextField(number, { number = it.take(32) }, label = { Text("شماره تلفن") }, singleLine = true,
                enabled = !locked, modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                textStyle = LocalTextStyle.current.copy(fontSize = LocalActionTextSize.current.sp, textDirection = TextDirection.Ltr),
                isError = number.isNotEmpty() && !validNumber,
                supportingText = { if (number.isNotEmpty() && !validNumber) Text("شماره را با ارقام وارد کنید؛ مانند ۰۹۱۲۱۲۳۴۵۶۷") })
            if (photo != null) ContactPhoto(photo, name)
            OutlinedButton(onClick = { picker.launch("image/*") }, enabled = !locked) {
                Text(if (busy) "در حال آماده‌سازی عکس…" else if (photo == null) "انتخاب عکس (اختیاری)" else "تغییر عکس", fontSize = 18.sp)
            }
            if (photo != null) TextButton(onClick = { photos.discardDraft(photo); photo = null }, enabled = !locked) {
                Text("حذف عکس", fontSize = 18.sp)
            }
        } },
        confirmButton = { TextButton(onClick = { onSave(name, number, photo) },
            enabled = name.isNotBlank() && validNumber && !locked) {
            Text(if (saving) "در حال ذخیره…" else "ذخیره", fontSize = 22.sp)
        } },
        dismissButton = { TextButton(onClick = ::cancel, enabled = !locked) { Text("انصراف", fontSize = 22.sp) } })
}

@Composable private fun ContactRow(contact: FamilyContact, click: () -> Unit) {
    Surface(onClick = click, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Hairline),
        modifier = Modifier.fillMaxWidth()) {
        Row(Modifier.padding(20.dp), verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            ContactPhoto(contact.photo, contact.name)
            Column(Modifier.weight(1f)) {
                Text(contact.name, fontSize = LocalActionTextSize.current.sp, fontWeight = FontWeight.Bold)
                Text("تماس", fontSize = 18.sp, color = Green)
            }
            LauncherIcon("phone", Green)
        }
    }
}

@Composable private fun ContactPhoto(path: String?, name: String) {
    val bitmap by produceState<android.graphics.Bitmap?>(null, path) {
        value = withContext(Dispatchers.IO) { path?.let { BitmapFactory.decodeFile(it) } }
    }
    Box(Modifier.size(72.dp).clip(CircleShape).background(Pale), contentAlignment = Alignment.Center) {
        if (bitmap != null) Image(bitmap!!.asImageBitmap(), contentDescription = null,
            modifier = Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
        else Text(name.take(1), fontSize = 30.sp, color = Green)
    }
}

@Composable private fun Title(text: String) = Text(text, fontSize = LocalActionTextSize.current.sp,
    fontWeight = FontWeight.Bold, color = Ink, modifier = Modifier.semantics { heading() })
@Composable private fun Body(text: String) = Text(text, fontSize = (LocalActionTextSize.current - 2).sp,
    lineHeight = (LocalActionTextSize.current + 10).sp, color = Muted)
@Composable private fun PhoneNumber(text: String) = Text(text, fontSize = LocalActionTextSize.current.sp,
    style = LocalTextStyle.current.copy(textDirection = TextDirection.Ltr))
@Composable private fun Empty(text: String) = Surface(color = Pale, shape = RoundedCornerShape(20.dp)) {
    Column(Modifier.fillMaxWidth().padding(24.dp), horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        LauncherIcon("family", Green)
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    }
}
@Composable private fun Action(text: String, click: () -> Unit, modifier: Modifier = Modifier, secondary: Boolean = false) {
    val actionModifier = modifier.fillMaxWidth().heightIn(min = 72.dp)
    if (secondary) OutlinedButton(onClick = click, modifier = actionModifier, shape = RoundedCornerShape(18.dp)) {
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    } else Button(onClick = click, modifier = actionModifier, shape = RoundedCornerShape(18.dp)) {
        Text(text, fontSize = LocalActionTextSize.current.sp, textAlign = TextAlign.Center)
    }
}
