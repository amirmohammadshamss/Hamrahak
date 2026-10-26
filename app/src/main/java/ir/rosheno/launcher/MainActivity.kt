package ir.rosheno.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf

class MainActivity : ComponentActivity() {
    private val model: LauncherViewModel by viewModels()
    private val homeSignal = mutableIntStateOf(0)
    private val resumed = mutableIntStateOf(0)
    private val showingSplash = mutableStateOf(false)
    private val roleRequest = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        resumed.intValue++
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.Theme_Rosheno)
        super.onCreate(savedInstanceState)
        if (android.os.Build.VERSION.SDK_INT >= 31) {
            splashScreen.setOnExitAnimationListener { splash ->
                val motionEnabled = android.animation.ValueAnimator.areAnimatorsEnabled()
                if (motionEnabled) splash.animate().alpha(0f).setDuration(180L)
                    .withEndAction { splash.remove() }.start()
                else splash.remove()
            }
        }
        homeSignal.intValue = savedInstanceState?.getInt("homeSignal") ?: 0
        showingSplash.value = savedInstanceState == null && !intent.hasCategory(Intent.CATEGORY_HOME)
        val platform = PlatformActions(this, model::message, roleRequest::launch)
        setContent {
            if (showingSplash.value) {
                BackHandler { showingSplash.value = false }
                LaunchSplash { showingSplash.value = false }
            }
            else LauncherApp(model, platform, homeSignal.intValue, resumed.intValue)
        }
    }
    override fun onResume() {
        super.onResume()
        resumed.intValue++
        model.refreshApps()
    }
    override fun onSaveInstanceState(outState: Bundle) {
        outState.putInt("homeSignal", homeSignal.intValue)
        super.onSaveInstanceState(outState)
    }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        if (Intent.CATEGORY_HOME in (intent.categories ?: emptySet())) {
            showingSplash.value = false
            homeSignal.intValue++
        }
    }
}
