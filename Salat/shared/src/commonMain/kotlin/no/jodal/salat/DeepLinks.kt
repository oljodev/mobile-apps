package no.jodal.salat

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** En lenke inn i appen, f.eks. fra en widget: `salat://kompis/start` åpner Kompis og starter en samtale. */
data class DeepLink(val app: String, val action: String?)

object DeepLinks {
    /** Lenken som er åpnet, men ikke tatt hånd om ennå. */
    var pending by mutableStateOf<DeepLink?>(null)
        private set

    fun open(url: String) {
        pending = parse(url)
    }

    fun parse(url: String): DeepLink? {
        if (!url.startsWith("salat://")) return null
        val parts = url.removePrefix("salat://").substringBefore('?').split('/').filter { it.isNotEmpty() }
        val app = parts.firstOrNull()?.lowercase() ?: return null
        return DeepLink(app, parts.getOrNull(1))
    }

    /** Om [action] i [app] venter. Den blir da tatt, så den bare skjer én gang. */
    fun consume(app: String, action: String): Boolean {
        if (pending != DeepLink(app, action)) return false
        pending = null
        return true
    }

    fun clear() {
        pending = null
    }
}

/** Kalles fra Swift når appen åpnes med en `salat://`-lenke. */
fun openDeepLink(url: String) = DeepLinks.open(url)
