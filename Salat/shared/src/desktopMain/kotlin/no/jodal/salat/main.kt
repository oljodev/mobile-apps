package no.jodal.salat

import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

/** Forhåndsvisning av UI-et på Linux: `./gradlew :shared:run`. */
fun main() = application {
    Window(
        onCloseRequest = ::exitApplication,
        title = "Salat (forhåndsvisning)",
        state = rememberWindowState(width = 390.dp, height = 844.dp),
    ) {
        SalatApp(PreviewBridge)
    }
}

/** Later som appen ble signert i går, og skriver ut i stedet for å sende varsler. */
private object PreviewBridge : NativeBridge {
    private val expiresAt = (Clock.System.now() + 6.days).toEpochMilliseconds()

    override fun signingExpiresAtMillis(): Long = expiresAt

    override fun scheduleExpiryReminder() = println("Forhåndsvisning: påminnelse planlagt")

    override fun sendTestNotification(delaySeconds: Int) =
        println("Forhåndsvisning: testvarsel om $delaySeconds sekunder")

    override fun scheduleTestAlarm(delaySeconds: Int, snoozeMinutes: Int, onResult: (String?) -> Unit) {
        println("Forhåndsvisning: testalarm om $delaySeconds sekunder, slumring $snoozeMinutes min")
        onResult(null)
    }

    private val storage = mutableMapOf<String, String>()

    override fun loadText(key: String): String? = storage[key]

    override fun saveText(key: String, value: String) {
        storage[key] = value
    }

    override fun loadSecret(key: String): String? = storage["secret.$key"]

    override fun saveSecret(key: String, value: String) {
        storage["secret.$key"] = value
    }

    override fun applyRiseSchedule(planJson: String, onResult: (String?) -> Unit) {
        println("Forhåndsvisning: Rise-plan på ${planJson.length} tegn")
        onResult(null)
    }

    override fun previewSound(sound: String) = println("Forhåndsvisning: spiller $sound")

    override fun stopSoundPreview() = Unit

    override fun startLiveSession(configJson: String, listener: LiveListener) =
        listener.onError("Samtaler med Gemini virker bare på iPhone.")

    override fun stopLiveSession() = Unit

    override fun setLiveMuted(muted: Boolean) = Unit

    override fun rollLiveSession(historyJson: String) = Unit
}
