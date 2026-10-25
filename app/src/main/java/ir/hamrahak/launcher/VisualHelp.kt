package ir.hamrahak.launcher

import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private data class GuideStep(val title: String, val caption: String, val scene: String, val target: String)
private data class VisualTopic(val title: String, val icon: String, val steps: List<GuideStep>)
private val topics = listOf(
    VisualTopic("تماس با خانواده", "family", listOf(
        GuideStep("خانواده را پیدا کنید", "در خانه، «خانوادهٔ من» را بزنید.", "home", "خانوادهٔ من"),
        GuideStep("یک نفر را انتخاب کنید", "عکس یا نام فرد موردنظر را بزنید.", "family", "مریم"),
        GuideStep("شماره را بررسی کنید", "اگر نام و شماره درست است، «رفتن به تلفن» را بزنید.", "confirm", "رفتن به تلفن"),
        GuideStep("تماس بگیرید", "در شماره‌گیر گوشی، دکمهٔ سبز تماس را بزنید.", "dialer", "تماس"))),
    VisualTopic("باز کردن برنامه", "apps", listOf(
        GuideStep("برنامه‌ها را باز کنید", "در خانه، دکمهٔ «برنامه‌ها» را بزنید.", "home", "برنامه‌ها"),
        GuideStep("عکس برنامه را بزنید", "برنامه‌ها با عکس و نام دیده می‌شوند؛ روی برنامهٔ دلخواه بزنید.", "apps", "برنامهٔ دلخواه"))),
    VisualTopic("عکس گرفتن و برگشتن", "camera", listOf(
        GuideStep("دوربین را باز کنید", "در خانه، «دوربین» را بزنید. اگر گوشی اجازه خواست، استفاده از دوربین را تأیید کنید.", "home", "دوربین"),
        GuideStep("عکس بگیرید", "گوشی را رو به سوژه بگیرید و «عکس گرفتن» را بزنید.", "camera", "عکس گرفتن"),
        GuideStep("به همراهک برگردید", "پس از دیدن عکس، دکمهٔ «بازگشت به همراهک» را بزنید.", "photo", "بازگشت به همراهک"))),
    VisualTopic("برگشتن به همراهک", "home", listOf(
        GuideStep("دکمهٔ بازگشت را بزنید", "در عکس‌ها و دوربین همراهک، دکمهٔ «بازگشت به همراهک» همیشه بالای صفحه است.", "photo", "بازگشت به همراهک"),
        GuideStep("از برنامه‌های دیگر برگردید", "دکمهٔ خانهٔ گوشی را بزنید؛ در گوشی بدون دکمه، از لبهٔ پایین به بالا بکشید.", "navigation", "خانهٔ گوشی"))),
    VisualTopic("کمک از نزدیکان", "help", listOf(
        GuideStep("دکمهٔ کمک را بزنید", "در خانه، «کمک می‌خواهم» را بزنید.", "home", "کمک می‌خواهم"),
        GuideStep("بار اول، یک نفر را انتخاب کنید", "اگر مخاطب کمک انتخاب نشده، یک نفر را اضافه و برای کمک انتخاب کنید.", "family", "انتخاب برای کمک"),
        GuideStep("تماس را ادامه دهید", "نام و شماره را بررسی کنید، «رفتن به تلفن» و سپس دکمهٔ تماس گوشی را بزنید.", "confirm", "رفتن به تلفن"))),
    VisualTopic("خواندن و فرستادن پیامک", "message", listOf(
        GuideStep("پیام‌ها را باز کنید", "در خانه، «پیام‌ها» را بزنید. برای دیدن متن‌ها، اجازهٔ خواندن پیامک را تأیید کنید.", "home", "پیام‌ها"),
        GuideStep("یک گفت‌وگو را باز کنید", "نام یا شماره را بزنید تا متن کامل پیام‌ها را ببینید. برای نوشتن، «نوشتن پیام جدید» را بزنید.", "sms-list", "نوشتن پیام جدید"),
        GuideStep("شماره و متن را بنویسید", "شمارهٔ گیرنده و متن را وارد کنید و «بررسی و ارسال» را بزنید.", "sms-compose", "بررسی و ارسال"),
        GuideStep("ارسال را تأیید کنید", "شماره و متن را دوباره بررسی کنید. ارسال پیامک ممکن است هزینهٔ سیم‌کارت داشته باشد؛ اگر درست است، «ارسال پیامک» را بزنید.", "sms-confirm", "ارسال پیامک"))),
)

@Composable
fun VisualHelp() {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    var index by rememberSaveable { mutableIntStateOf(0) }
    Text("راهنمای تصویری", fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
    if (selected !in topics.indices) {
        Text("چه کاری می‌خواهید انجام دهید؟", fontSize = 22.sp, color = Muted)
        topics.forEachIndexed { i, topic -> Surface(onClick = { selected = i; index = 0 },
            color = Color.White, shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Hairline)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LauncherIcon(topic.icon, Green)
                Text(topic.title, fontSize = 22.sp, modifier = Modifier.weight(1f))
            }
        } }
    } else {
        val topic = topics[selected]
        val step = topic.steps[index.coerceIn(topic.steps.indices)]
        Text("${persianDigits((index + 1).toString())} از ${persianDigits(topic.steps.size.toString())} · ${topic.title}", fontSize = 18.sp, color = Muted)
        AnimatedContent(targetState = index, label = "guide-step") { i ->
            val illustrated = topic.steps[i.coerceIn(topic.steps.indices)]
            PhoneIllustration(illustrated)
        }
        Text(step.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(step.caption, fontSize = 22.sp, color = Ink)
        Button(onClick = { if (index < topic.steps.lastIndex) index++ else selected = -1 },
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text(if (index < topic.steps.lastIndex) "مرحلهٔ بعد" else "پایان راهنما", fontSize = 22.sp)
        }
        if (index > 0) OutlinedButton(onClick = { index-- }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text("مرحلهٔ قبل", fontSize = 20.sp)
        }
        TextButton(onClick = { selected = -1 }) { Text("راهنماهای دیگر", fontSize = 20.sp) }
    }
}

@Composable private fun PhoneIllustration(step: GuideStep) {
    Box(Modifier.fillMaxWidth().testTag("guide-illustration"), contentAlignment = Alignment.Center) {
        Surface(color = Paper, shape = RoundedCornerShape(32.dp), border = BorderStroke(5.dp, Ink),
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
                .clearAndSetSemantics { contentDescription = "تصویر راهنما: ${step.caption}" }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp, 5.dp).background(Ink, CircleShape))
                Text(if (step.scene == "dialer") "شماره‌گیر گوشی" else "همراهک", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                when (step.scene) {
                    "home" -> {
                        Text("۱۴:۳۰", fontSize = 30.sp, color = Green)
                        listOf("تماس" to "phone", "خانوادهٔ من" to "family", "پیام‌ها" to "message", "برنامه‌ها" to "apps", "دوربین" to "camera", "کمک می‌خواهم" to "help")
                            .forEach { (label, icon) -> MockAction(label, icon, label == step.target) }
                    }
                    "family" -> {
                        Box(Modifier.size(72.dp).background(Pale, CircleShape), contentAlignment = Alignment.Center) { LauncherIcon("family", Green) }
                        MockAction("مریم", "phone", step.target == "مریم")
                        MockAction("انتخاب برای کمک", "help", step.target == "انتخاب برای کمک")
                        MockAction("یکی از نزدیکان", "family", false)
                    }
                    "apps" -> {
                        Text("برنامه‌ها", fontSize = 20.sp)
                        MockAction("برنامهٔ دلخواه", "apps", true)
                        MockAction("پیام‌ها", "message", false)
                        MockAction("عکس‌ها", "photo", false)
                    }
                    "sms-list", "sms-compose", "sms-confirm" -> {
                        MockAction("بازگشت به همراهک", "home", false)
                        if (step.scene == "sms-list") {
                            MockAction("مریم", "message", false)
                            MockAction("نوشتن پیام جدید", "message", true)
                        } else {
                            Text("شمارهٔ گیرنده", fontSize = 18.sp)
                            Surface(color = Pale, shape = RoundedCornerShape(16.dp)) {
                                Text("سلام، حال شما خوب است؟", Modifier.padding(16.dp), fontSize = 18.sp)
                            }
                            MockAction(step.target, "message", true)
                        }
                    }
                    "confirm", "dialer" -> {
                        Text("تماس با مریم", fontSize = 22.sp)
                        Text("شمارهٔ انتخاب‌شده", fontSize = 18.sp)
                        MockAction(if (step.scene == "dialer") "تماس" else "رفتن به تلفن", "phone", true)
                        MockAction("انصراف", "home", false)
                    }
                    "camera", "photo", "navigation" -> {
                        MockAction("بازگشت به همراهک", "home", step.target == "بازگشت به همراهک")
                        Surface(color = Pale, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            Box(contentAlignment = Alignment.Center) { LauncherIcon("photo", Green) }
                        }
                        if (step.scene == "camera") MockAction("عکس گرفتن", "camera", true)
                        if (step.scene == "navigation") {
                            Text("↑", fontSize = 36.sp, color = Orange)
                            MockAction("خانهٔ گوشی", "home", true)
                        }
                    }
                }
                Box(Modifier.size(64.dp, 4.dp).background(Ink, CircleShape))
            }
        }
    }
}

@Composable private fun MockAction(label: String, icon: String, highlighted: Boolean) {
    Box(Modifier.fillMaxWidth()) {
        Surface(color = if (highlighted) Pale else Color.White, shape = RoundedCornerShape(14.dp),
            border = BorderStroke(if (highlighted) 2.dp else 1.dp, if (highlighted) Orange else Hairline)) {
            Row(Modifier.fillMaxWidth().padding(12.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                LauncherIcon(icon, Green)
                Text(label, fontSize = 16.sp, color = Ink, modifier = Modifier.weight(1f), textAlign = TextAlign.Start)
            }
        }
        if (highlighted) Image(painterResource(R.drawable.tap_hand), contentDescription = null,
            modifier = Modifier.align(Alignment.BottomEnd).offset(x = 5.dp, y = 12.dp).size(36.dp))
    }
}
