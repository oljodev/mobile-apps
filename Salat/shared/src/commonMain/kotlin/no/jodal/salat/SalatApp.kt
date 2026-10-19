package no.jodal.salat

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import no.jodal.salat.ui.BackButton
import no.jodal.salat.ui.HomeScreen
import no.jodal.salat.ui.MiniAppIconImage
import no.jodal.salat.ui.SalatTheme
import no.jodal.salat.ui.tint

@Composable
fun SalatApp(bridge: NativeBridge, darkTheme: Boolean = isSystemInDarkTheme()) {
    SalatTheme(darkTheme) {
        var openApp by remember { mutableStateOf<MiniApp?>(null) }
        AnimatedContent(
            targetState = openApp,
            transitionSpec = {
                // Mini-appen glir inn fra høyre, og ut igjen til høyre når man går tilbake.
                val opening = targetState != null
                val duration = 280
                if (opening) {
                    slideInHorizontally(tween(duration)) { it } togetherWith
                        slideOutHorizontally(tween(duration)) { -it / 4 } + fadeOut(tween(duration))
                } else {
                    slideInHorizontally(tween(duration)) { -it / 4 } + fadeIn(tween(duration)) togetherWith
                        slideOutHorizontally(tween(duration)) { it }
                }
            },
        ) { app ->
            if (app == null) {
                HomeScreen(bridge, darkTheme, onOpen = { openApp = it })
            } else {
                MiniAppScreen(app, bridge, darkTheme, onBack = { openApp = null })
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MiniAppScreen(app: MiniApp, bridge: NativeBridge, darkTheme: Boolean, onBack: () -> Unit) {
    val context = remember(bridge, onBack) { MiniAppContext(bridge, onBack) }
    if (app.customTopBar) {
        Box {
            app.content?.invoke(context)
            BackSwipeEdge(onBack)
        }
        return
    }
    Box {
        Scaffold(
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            MiniAppIconImage(app.icon, size = 22.dp)
                            Text(app.title)
                        }
                    },
                    navigationIcon = { BackButton(onBack) },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.tint(app.color, darkTheme),
                    ),
                )
            },
        ) { padding ->
            Box(Modifier.fillMaxSize().padding(padding)) {
                app.content?.invoke(context)
            }
        }
        BackSwipeEdge(onBack)
    }
}

/** Sveip fra venstre kant for å gå tilbake, som ellers i iOS. */
@Composable
private fun BackSwipeEdge(onBack: () -> Unit) {
    Box(
        Modifier.fillMaxHeight().width(24.dp).pointerInput(Unit) {
            var dragged = 0f
            detectHorizontalDragGestures(
                onDragStart = { dragged = 0f },
                onDragEnd = { if (dragged > 80.dp.toPx()) onBack() },
            ) { _, amount -> dragged += amount }
        },
    )
}
