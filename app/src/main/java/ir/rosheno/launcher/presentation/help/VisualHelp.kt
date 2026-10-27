package ir.rosheno.launcher.presentation.help

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import ir.rosheno.launcher.R
import ir.rosheno.launcher.domain.phone.persianDigits
import ir.rosheno.launcher.presentation.components.LauncherIcon
import ir.rosheno.launcher.presentation.theme.Green
import ir.rosheno.launcher.presentation.theme.Hairline
import ir.rosheno.launcher.presentation.theme.Ink
import ir.rosheno.launcher.presentation.theme.Muted
import ir.rosheno.launcher.presentation.theme.Orange
import ir.rosheno.launcher.presentation.theme.Pale
import ir.rosheno.launcher.presentation.theme.Paper

private data class GuideStep(val title: String, val caption: String, val scene: String, val target: String)
private data class VisualTopic(val title: String, val icon: String, val steps: List<GuideStep>)
private fun guideTopics(context: android.content.Context) = listOf(
    VisualTopic(context.getString(R.string.guide_topic_family), "family", listOf(
        GuideStep(context.getString(R.string.guide_family_find_title), context.getString(R.string.guide_family_find_description), "home", context.getString(R.string.family_title)),
        GuideStep(context.getString(R.string.guide_family_choose_title), context.getString(R.string.guide_family_choose_description), "family", context.getString(R.string.guide_sample_contact_name)),
        GuideStep(context.getString(R.string.guide_family_review_title), context.getString(R.string.guide_family_review_description), "confirm", context.getString(R.string.action_go_to_phone)),
        GuideStep(context.getString(R.string.guide_family_call_title), context.getString(R.string.guide_family_call_description), "dialer", context.getString(R.string.call_label)))),
    VisualTopic(context.getString(R.string.guide_topic_apps), "apps", listOf(
        GuideStep(context.getString(R.string.guide_apps_open_title), context.getString(R.string.guide_apps_open_description), "home", context.getString(R.string.apps_title)),
        GuideStep(context.getString(R.string.guide_apps_choose_title), context.getString(R.string.guide_apps_choose_description), "apps", context.getString(R.string.guide_favorite_app)))),
    VisualTopic(context.getString(R.string.guide_topic_camera), "camera", listOf(
        GuideStep(context.getString(R.string.guide_camera_open_title), context.getString(R.string.guide_camera_open_description), "home", context.getString(R.string.camera_title)),
        GuideStep(context.getString(R.string.guide_camera_capture_title), context.getString(R.string.guide_camera_capture_description), "camera", context.getString(R.string.action_take_photo)),
        GuideStep(context.getString(R.string.guide_camera_return_title), context.getString(R.string.guide_camera_return_description), "photo", context.getString(R.string.action_return_to_rosheno)))),
    VisualTopic(context.getString(R.string.guide_topic_return), "home", listOf(
        GuideStep(context.getString(R.string.guide_return_button_title), context.getString(R.string.guide_return_button_description), "photo", context.getString(R.string.action_return_to_rosheno)),
        GuideStep(context.getString(R.string.guide_return_home_title), context.getString(R.string.guide_return_home_description), "navigation", context.getString(R.string.guide_system_home)))),
    VisualTopic(context.getString(R.string.guide_topic_help), "help", listOf(
        GuideStep(context.getString(R.string.guide_help_button_title), context.getString(R.string.guide_help_button_description), "home", context.getString(R.string.help_action_label)),
        GuideStep(context.getString(R.string.guide_help_choose_title), context.getString(R.string.guide_help_choose_description), "family", context.getString(R.string.action_choose_help_contact)),
        GuideStep(context.getString(R.string.guide_help_continue_title), context.getString(R.string.guide_help_continue_description), "confirm", context.getString(R.string.action_go_to_phone)))),
    VisualTopic(context.getString(R.string.guide_topic_sms), "message", listOf(
        GuideStep(context.getString(R.string.guide_sms_open_title), context.getString(R.string.guide_sms_open_description), "home", context.getString(R.string.messages_label)),
        GuideStep(context.getString(R.string.guide_sms_conversation_title), context.getString(R.string.guide_sms_conversation_description), "sms-list", context.getString(R.string.action_new_message)),
        GuideStep(context.getString(R.string.guide_sms_compose_title), context.getString(R.string.guide_sms_compose_description), "sms-compose", context.getString(R.string.action_review_send)),
        GuideStep(context.getString(R.string.guide_sms_confirm_title), context.getString(R.string.guide_sms_confirm_description), "sms-confirm", context.getString(R.string.action_send_sms)))),
    VisualTopic(context.getString(R.string.guide_topic_dialer), "phone", listOf(
        GuideStep(context.getString(R.string.guide_dialer_open_title), context.getString(R.string.guide_dialer_open_description), "home", context.getString(R.string.call_label)),
        GuideStep(context.getString(R.string.guide_dialer_number_title), context.getString(R.string.guide_dialer_number_description), "manual-dialer", context.getString(R.string.action_call_number)),
        GuideStep(context.getString(R.string.guide_dialer_confirm_title), context.getString(R.string.guide_dialer_confirm_description), "start-call", context.getString(R.string.action_start_call)))),
)

@Composable
fun VisualHelp(model: HelpViewModel = viewModel()) {
    val context = LocalContext.current
    val topics = remember(context, androidx.compose.ui.platform.LocalConfiguration.current) { guideTopics(context) }
    val state by model.state.collectAsStateWithLifecycle()
    val selected = state.selected
    val index = state.index
    BackHandler(enabled = selected in topics.indices) {
        model.previous()
    }
    Text(context.getString(R.string.guide_title), fontSize = 26.sp, fontWeight = FontWeight.Bold, color = Ink)
    if (selected !in topics.indices) {
        Text(context.getString(R.string.guide_choose_topic), fontSize = 22.sp, color = Muted)
        topics.forEachIndexed { i, topic -> Surface(onClick = { model.select(i) },
            color = colorResource(R.color.white), shape = RoundedCornerShape(24.dp), border = BorderStroke(1.dp, Hairline)) {
            Row(Modifier.fillMaxWidth().padding(20.dp), verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                LauncherIcon(topic.icon, Green)
                Text(topic.title, fontSize = 22.sp, modifier = Modifier.weight(1f))
            }
        } }
    } else {
        val topic = topics[selected]
        val step = topic.steps[index.coerceIn(topic.steps.indices)]
        Text(context.getString(R.string.guide_step_progress, persianDigits((index + 1).toString()), persianDigits(topic.steps.size.toString()), topic.title), fontSize = 18.sp, color = Muted)
        AnimatedContent(targetState = index, label = "guide-step") { i ->
            val illustrated = topic.steps[i.coerceIn(topic.steps.indices)]
            PhoneIllustration(illustrated)
        }
        Text(step.title, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Text(step.caption, fontSize = 22.sp, color = Ink)
        Button(onClick = { model.next(topic.steps.lastIndex) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 72.dp)) {
            Text(if (index < topic.steps.lastIndex) context.getString(R.string.action_next_step) else context.getString(R.string.action_finish_guide), fontSize = 22.sp)
        }
        if (index > 0) OutlinedButton(onClick = { model.previous() }, modifier = Modifier.fillMaxWidth().heightIn(min = 64.dp)) {
            Text(context.getString(R.string.action_previous_step), fontSize = 20.sp)
        }
        TextButton(onClick = { model.topics() }) { Text(context.getString(R.string.action_other_guides), fontSize = 20.sp) }
    }
}

@Composable private fun PhoneIllustration(step: GuideStep) {
    val context = LocalContext.current
    Box(Modifier.fillMaxWidth().testTag("guide-illustration"), contentAlignment = Alignment.Center) {
        Surface(color = Paper, shape = RoundedCornerShape(32.dp), border = BorderStroke(5.dp, Ink),
            modifier = Modifier.widthIn(max = 320.dp).fillMaxWidth()
                .clearAndSetSemantics { contentDescription = context.getString(R.string.guide_illustration_description, step.caption) }) {
            Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Box(Modifier.size(44.dp, 5.dp).background(Ink, CircleShape))
                Text(if (step.scene == "dialer") context.getString(R.string.system_dialer_title) else context.getString(R.string.brand_name), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                when (step.scene) {
                    "home" -> {
                        Text(context.getString(R.string.guide_sample_time), fontSize = 30.sp, color = Green)
                        listOf(context.getString(R.string.call_label) to "phone", context.getString(R.string.family_title) to "family", context.getString(R.string.messages_label) to "message", context.getString(R.string.apps_title) to "apps", context.getString(R.string.camera_title) to "camera", context.getString(R.string.help_action_label) to "help")
                            .forEach { (label, icon) -> MockAction(label, icon, label == step.target) }
                    }
                    "family" -> {
                        Box(Modifier.size(72.dp).background(Pale, CircleShape), contentAlignment = Alignment.Center) { LauncherIcon("family", Green) }
                        MockAction(context.getString(R.string.guide_sample_contact_name), "phone", step.target == context.getString(R.string.guide_sample_contact_name))
                        MockAction(context.getString(R.string.action_choose_help_contact), "help", step.target == context.getString(R.string.action_choose_help_contact))
                        MockAction(context.getString(R.string.guide_another_contact), "family", false)
                    }
                    "apps" -> {
                        Text(context.getString(R.string.apps_title), fontSize = 20.sp)
                        MockAction(context.getString(R.string.guide_favorite_app), "apps", true)
                        MockAction(context.getString(R.string.messages_label), "message", false)
                        MockAction(context.getString(R.string.photos_label), "photo", false)
                    }
                    "sms-list", "sms-compose", "sms-confirm" -> {
                        MockAction(context.getString(R.string.action_return_to_rosheno), "home", false)
                        if (step.scene == "sms-list") {
                            MockAction(context.getString(R.string.guide_sample_contact_name), "message", false)
                            MockAction(context.getString(R.string.action_new_message), "message", true)
                        } else {
                            Text(context.getString(R.string.sms_recipient_label), fontSize = 18.sp)
                            Surface(color = Pale, shape = RoundedCornerShape(16.dp)) {
                                Text(context.getString(R.string.guide_sample_message), Modifier.padding(16.dp), fontSize = 18.sp)
                            }
                            MockAction(step.target, "message", true)
                        }
                    }
                    "manual-dialer", "start-call" -> {
                        MockAction(context.getString(R.string.action_return_to_rosheno), "home", false)
                        Text(context.getString(R.string.guide_entered_number), fontSize = 22.sp, color = Green)
                        if (step.scene == "manual-dialer") {
                            listOf(context.getString(R.string.guide_keypad_first_row), context.getString(R.string.guide_keypad_second_row), context.getString(R.string.guide_keypad_third_row), context.getString(R.string.guide_keypad_last_row)).forEach {
                                Text(it, fontSize = 24.sp)
                            }
                        }
                        MockAction(step.target, "phone", true)
                    }
                    "confirm", "dialer" -> {
                        Text(context.getString(R.string.guide_sample_call), fontSize = 22.sp)
                        Text(context.getString(R.string.guide_selected_number), fontSize = 18.sp)
                        MockAction(if (step.scene == "dialer") context.getString(R.string.call_label) else context.getString(R.string.action_go_to_phone), "phone", true)
                        MockAction(context.getString(R.string.action_cancel), "home", false)
                    }
                    "camera", "photo", "navigation" -> {
                        MockAction(context.getString(R.string.action_return_to_rosheno), "home", step.target == context.getString(R.string.action_return_to_rosheno))
                        Surface(color = Pale, shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            Box(contentAlignment = Alignment.Center) { LauncherIcon("photo", Green) }
                        }
                        if (step.scene == "camera") MockAction(context.getString(R.string.action_take_photo), "camera", true)
                        if (step.scene == "navigation") {
                            Text(context.getString(R.string.guide_navigation_up), fontSize = 36.sp, color = Orange)
                            MockAction(context.getString(R.string.guide_system_home), "home", true)
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
        Surface(color = if (highlighted) Pale else colorResource(R.color.white), shape = RoundedCornerShape(14.dp),
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
