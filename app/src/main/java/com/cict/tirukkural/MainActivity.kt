package com.cict.tirukkural

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.viewmodel.compose.viewModel
import com.cict.tirukkural.ui.PlayerViewModel
import com.cict.tirukkural.ui.TirukkuralApp
import com.cict.tirukkural.ui.theme.TirukkuralTheme

class MainActivity : ComponentActivity() {

    /** Bumped whenever the player should open, e.g. after tapping the playback notification. */
    private var openPlayerSignal by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        // Light status bar icons by default: the screen opens on the navy hero (or the player).
        enableEdgeToEdge(statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        super.onCreate(savedInstanceState)
        if (savedInstanceState == null) handleIntent(intent)
        setContent {
            TirukkuralTheme {
                val vm: PlayerViewModel = viewModel()
                TirukkuralApp(vm = vm, openPlayerSignal = openPlayerSignal)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent?.getBooleanExtra(EXTRA_OPEN_PLAYER, false) == true) openPlayerSignal++
    }

    companion object {
        const val EXTRA_OPEN_PLAYER = "com.cict.tirukkural.OPEN_PLAYER"
    }
}
