package ir.rosheno.launcher

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class GuideFeatureTest {
    @get:Rule val compose = createComposeRule()
    @Test fun everyIllustratedGuideCanBeCompletedAndReturnsToTopics() {
        compose.setContent { RoshenoTheme { Column(Modifier.verticalScroll(rememberScrollState())) { VisualHelp() } } }
        val topics = listOf("تماس با خانواده" to 4, "باز کردن برنامه" to 2, "عکس گرفتن و برگشتن" to 3,
            "برگشتن به روشنو" to 2, "کمک از نزدیکان" to 3, "خواندن و فرستادن پیامک" to 4,
            "شماره گرفتن و تماس" to 3)
        topics.forEach { (title, steps) ->
            compose.onNodeWithText(title).performScrollTo().performClick()
            repeat(steps) { index ->
                compose.onNodeWithTag("guide-illustration").performScrollTo().assertIsDisplayed()
                compose.onNodeWithText(if (index == steps - 1) "پایان راهنما" else "مرحلهٔ بعد")
                    .performScrollTo().performClick()
            }
            compose.onNodeWithText("چه کاری می‌خواهید انجام دهید؟").performScrollTo().assertIsDisplayed()
        }
    }
}
