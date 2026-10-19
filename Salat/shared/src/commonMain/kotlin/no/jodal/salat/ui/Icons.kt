package no.jodal.salat.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBackIos
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import no.jodal.salat.MiniAppIcon
import org.jetbrains.compose.resources.painterResource

/** Logoen eller ikonet til en mini-app. Ikoner får fargen [tint]. */
@Composable
fun MiniAppIconImage(icon: MiniAppIcon, size: Dp, tint: Color = LocalContentColor.current) {
    when (icon) {
        is MiniAppIcon.Logo -> Image(painterResource(icon.image), contentDescription = null, modifier = Modifier.size(size))
        is MiniAppIcon.Symbol -> Icon(icon.vector, contentDescription = null, tint = tint, modifier = Modifier.size(size))
    }
}

/** «‹ Salat», for å gå tilbake til forsiden. */
@Composable
fun BackButton(onBack: () -> Unit, color: Color = MaterialTheme.colorScheme.primary) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onBack)
            .padding(horizontal = 10.dp, vertical = 8.dp),
    ) {
        Icon(Icons.AutoMirrored.Rounded.ArrowBackIos, contentDescription = "Tilbake", tint = color, modifier = Modifier.size(18.dp))
        Text("Salat", style = MaterialTheme.typography.bodyLarge, color = color)
    }
}
