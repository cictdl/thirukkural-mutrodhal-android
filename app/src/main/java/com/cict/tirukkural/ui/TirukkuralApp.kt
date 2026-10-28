package com.cict.tirukkural.ui

import android.graphics.Color as AndroidColor
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.windowInsetsTopHeight
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cict.tirukkural.R
import com.cict.tirukkural.ui.theme.BrandNight

/** From this width the player stays docked beside the list instead of opening over it. */
private val TwoPaneMinWidth = 840.dp

@Composable
fun TirukkuralApp(vm: PlayerViewModel, openPlayerSignal: Int) {
    val ui by vm.ui.collectAsStateWithLifecycle()
    val position by vm.position.collectAsStateWithLifecycle()
    val offline by vm.offline.collectAsStateWithLifecycle()

    var playerOpen by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(openPlayerSignal) {
        if (openPlayerSignal > 0) playerOpen = true
    }

    val context = LocalContext.current
    val snackbar = remember { SnackbarHostState() }
    LaunchedEffect(Unit) {
        vm.downloadFailures.collect { number ->
            snackbar.showSnackbar(context.getString(R.string.download_failed, number))
        }
    }

    val listState = rememberLazyListState()
    val heroVisible by remember { derivedStateOf { listState.firstVisibleItemIndex == 0 } }
    val darkTheme = isSystemInDarkTheme()
    val navBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    BoxWithConstraints(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
    ) {
        val twoPane = maxWidth >= TwoPaneMinWidth
        val playerPaneWidth = maxWidth * 0.42f
        val playerVisible = playerOpen && !twoPane
        BackHandler(enabled = playerVisible) { playerOpen = false }
        // In two panes the docked player sits under the right half of the status bar, so keep
        // light icons there and give the list's status bar strip the same navy.
        SystemBarAppearance(
            lightStatusIcons = twoPane || playerVisible || heroVisible || darkTheme,
            lightNavIcons = playerVisible || darkTheme,
        )
        val showMiniPlayer = !twoPane && ui.connected &&
            (ui.playWhenReady || position.positionMs > 0 || ui.chapterNumber > 1)

        Row(Modifier.fillMaxSize()) {
            Box(
                Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                HomeScreen(
                    vm = vm,
                    ui = ui,
                    position = position,
                    offline = offline,
                    listState = listState,
                    bottomPadding = navBottom + if (showMiniPlayer) 104.dp else 24.dp,
                    onOpenPlayer = { if (!twoPane) playerOpen = true },
                    modifier = Modifier.windowInsetsPadding(WindowInsets.statusBars),
                )

                // Status bar backdrop: matches the hero while it is visible, then the list background.
                val statusBarColor by animateColorAsState(
                    targetValue = if (heroVisible || twoPane) BrandNight else MaterialTheme.colorScheme.background,
                    animationSpec = tween(250),
                    label = "statusBar",
                )
                Box(
                    Modifier
                        .fillMaxWidth()
                        .windowInsetsTopHeight(WindowInsets.statusBars)
                        .background(statusBarColor),
                )

                MiniPlayerHost(
                    visible = showMiniPlayer && !playerOpen,
                    vm = vm,
                    ui = ui,
                    position = position,
                    onOpen = { playerOpen = true },
                )

                SnackbarHost(
                    hostState = snackbar,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = if (showMiniPlayer) 92.dp else 16.dp),
                )
            }

            if (twoPane) {
                Box(
                    Modifier
                        .width(playerPaneWidth)
                        .fillMaxHeight(),
                ) {
                    NowPlayingScreen(
                        vm = vm,
                        ui = ui,
                        position = position,
                        offline = offline,
                        onCollapse = null,
                    )
                }
            }
        }

        AnimatedVisibility(
            visible = playerVisible,
            enter = slideInVertically(tween(380)) { it } + fadeIn(tween(250)),
            exit = slideOutVertically(tween(300)) { it } + fadeOut(tween(250)),
        ) {
            NowPlayingScreen(
                vm = vm,
                ui = ui,
                position = position,
                offline = offline,
                onCollapse = { playerOpen = false },
            )
        }
    }
}

@Composable
private fun BoxScope.MiniPlayerHost(
    visible: Boolean,
    vm: PlayerViewModel,
    ui: PlayerUiState,
    position: PlaybackPosition,
    onOpen: () -> Unit,
) {
    AnimatedVisibility(
        visible = visible,
        modifier = Modifier.align(Alignment.BottomCenter),
        enter = slideInVertically { it } + fadeIn(),
        exit = slideOutVertically { it } + fadeOut(),
    ) {
        MiniPlayer(
            ui = ui,
            position = position,
            onOpen = onOpen,
            onPlayPause = vm::togglePlayPause,
            onNext = { vm.next() },
            modifier = Modifier
                .navigationBarsPadding()
                .padding(horizontal = 12.dp, vertical = 10.dp)
                .widthIn(max = 640.dp),
        )
    }
}

/**
 * Re-applies edge-to-edge with the wanted icon colours. Going through enableEdgeToEdge (rather than
 * the insets controller directly) keeps the choice after the activity is recreated, e.g. on rotation.
 */
@Composable
private fun SystemBarAppearance(lightStatusIcons: Boolean, lightNavIcons: Boolean) {
    val view = LocalView.current
    if (view.isInEditMode) return
    val activity = view.context as? ComponentActivity ?: return
    DisposableEffect(activity, lightStatusIcons, lightNavIcons) {
        val apply = Runnable {
            activity.enableEdgeToEdge(
                statusBarStyle = barStyle(lightIcons = lightStatusIcons),
                navigationBarStyle = barStyle(lightIcons = lightNavIcons),
            )
        }
        apply.run()
        // After a recreation (rotation, resize) the new window can fall back to the activity's
        // initial bar style, so apply again after the first frame and whenever it regains focus.
        view.post(apply)
        val onFocus = ViewTreeObserver.OnWindowFocusChangeListener { hasFocus -> if (hasFocus) apply.run() }
        view.viewTreeObserver.addOnWindowFocusChangeListener(onFocus)
        onDispose {
            view.removeCallbacks(apply)
            view.viewTreeObserver.removeOnWindowFocusChangeListener(onFocus)
        }
    }
}

private fun barStyle(lightIcons: Boolean): SystemBarStyle =
    if (lightIcons) {
        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
    } else {
        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
    }
