package no.jodal.salat.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import no.jodal.salat.Haptics

/**
 * Trykkbar, som i iOS: det man trykker på krymper litt og spretter tilbake, med en liten
 * vibrasjon, i stedet for Android-bølgen. Hold inne for [onLongClick].
 */
fun Modifier.pressable(
    onClick: () -> Unit,
    enabled: Boolean = true,
    pressedScale: Float = 0.95f,
    onLongClick: (() -> Unit)? = null,
    haptic: () -> Unit = Haptics::tap,
): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed && enabled) pressedScale else 1f,
        animationSpec = spring(dampingRatio = 0.55f, stiffness = 700f),
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }.combinedClickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        onLongClick = onLongClick?.let { long ->
            {
                Haptics.firm()
                long()
            }
        },
        onClick = {
            haptic()
            onClick()
        },
    )
}
