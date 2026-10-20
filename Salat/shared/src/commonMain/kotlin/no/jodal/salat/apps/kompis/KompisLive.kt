package no.jodal.salat.apps.kompis

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import no.jodal.salat.LiveListener
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.kompis.ui.LiveStatus
import no.jodal.salat.apps.kompis.ui.LiveUiState

/** Modellen Kompis bruker. */
const val LiveModel = "gemini-3.8-live"

/** Det Swift trenger for å starte en samtale. Sendes som JSON. */
@Serializable
data class LiveConfig(
    val apiKey: String,
    val model: String,
    val voice: String,
    val systemInstruction: String,
    /** Tidligere meldinger, når en samtale fortsettes. */
    val history: List<HistoryTurn>,
)

@Serializable
data class HistoryTurn(
    /** `user` eller `model`, som i Gemini. */
    val role: String,
    val text: String,
)

/**
 * Den pågående samtalen med Gemini Live. Det finnes bare én om gangen, og den fortsetter selv om
 * man går ut av samtaleskjermen.
 */
class KompisLive private constructor(private val store: KompisStore, private val bridge: NativeBridge) : LiveListener {
    var state by mutableStateOf(LiveUiState())
        private set

    /** Samtalen som pågår, eller `null`. */
    var conversationId by mutableStateOf<String?>(null)
        private set

    /** Når samtalen startet, for å vise hvor lenge den har vart. */
    var startedAtMillis by mutableStateOf(0L)
        private set

    val isActive: Boolean get() = conversationId != null

    /** Starter en ny samtale, eller fortsetter [continuing]. */
    fun start(continuing: Conversation?) {
        if (isActive) return
        val now = store.nowMillis()
        val conversation = continuing ?: Conversation(
            id = newId(),
            title = "Ny samtale",
            createdAtMillis = now,
            updatedAtMillis = now,
        )
        conversationId = conversation.id
        startedAtMillis = now
        state = LiveUiState(
            title = conversation.title,
            status = if (store.hasApiKey) LiveStatus.Connecting else LiveStatus.NeedsApiKey,
            messages = conversation.messages,
            message = if (store.hasApiKey) null else "Legg inn en Gemini-nøkkel i innstillingene for å snakke med Kompis.",
        )
        if (!store.hasApiKey) {
            conversationId = null
            return
        }

        val config = LiveConfig(
            apiKey = store.apiKey,
            model = LiveModel,
            voice = store.data.settings.voice,
            systemInstruction = systemInstruction(store.data.settings.personality),
            history = conversation.messages.takeLast(MaxHistory).map {
                HistoryTurn(role = if (it.role == Role.User) "user" else "model", text = it.text)
            },
        )
        // En ny samtale lagres først når noe er sagt, så tomme samtaler ikke blir liggende.
        if (continuing != null) save(conversation)
        bridge.startLiveSession(json.encodeToString(LiveConfig.serializer(), config), this)
    }

    fun end() {
        if (!isActive) return
        bridge.stopLiveSession()
        finishTurn()
        saveDuration()
        conversationId = null
        state = state.copy(status = LiveStatus.Ended, level = 0f)
    }

    fun toggleMute() {
        val muted = !state.muted
        bridge.setLiveMuted(muted)
        state = state.copy(muted = muted, status = if (muted) LiveStatus.Muted else LiveStatus.Listening)
    }

    // LiveListener, kalles fra Swift.

    override fun onStatus(status: String) {
        val newStatus = when (status) {
            "connecting" -> LiveStatus.Connecting
            "listening" -> if (state.muted) LiveStatus.Muted else LiveStatus.Listening
            "thinking" -> LiveStatus.Thinking
            "speaking" -> LiveStatus.Speaking
            "reconnecting" -> LiveStatus.Reconnecting
            "ended" -> LiveStatus.Ended
            else -> return
        }
        state = state.copy(status = newStatus, message = null)
    }

    override fun onLevel(level: Float) {
        state = state.copy(level = level)
    }

    override fun onUserText(text: String) {
        // Brukeren begynner å snakke igjen: det Kompis sa før er ferdig.
        if (state.kompisDraft != null) finishTurn()
        state = state.copy(userDraft = (state.userDraft.orEmpty() + text).trimStart())
    }

    override fun onKompisText(text: String) {
        // Kompis begynner å svare: det brukeren sa er ferdig.
        state.userDraft?.let { addMessage(Role.User, it) }
        state = state.copy(userDraft = null, kompisDraft = (state.kompisDraft.orEmpty() + text).trimStart())
    }

    override fun onTurnComplete() = finishTurn()

    override fun onInterrupted() = finishTurn()

    override fun onAudioRoute(name: String) {
        state = state.copy(audioRoute = name)
    }

    override fun onDebug(text: String) {
        state = state.copy(debug = text)
    }

    override fun onError(message: String) {
        finishTurn()
        saveDuration()
        conversationId = null
        state = state.copy(status = LiveStatus.Failed, message = message, level = 0f)
    }

    // Lagring

    private fun finishTurn() {
        state.userDraft?.let { addMessage(Role.User, it) }
        state.kompisDraft?.let { addMessage(Role.Kompis, it) }
        state = state.copy(userDraft = null, kompisDraft = null)
    }

    private fun addMessage(role: Role, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) {
            return
        }
        val message = Message(role, trimmed, store.nowMillis())
        state = state.copy(messages = state.messages + message)
        val id = conversationId ?: return
        val existing = store.conversation(id)
        val conversation = (existing ?: Conversation(id, state.title, startedAtMillis, startedAtMillis)).let {
            val title = if (it.messages.none { m -> m.role == Role.User } && role == Role.User) titleFrom(trimmed) else it.title
            it.copy(title = title, messages = it.messages + message, updatedAtMillis = message.atMillis)
        }
        state = state.copy(title = conversation.title)
        save(conversation)
    }

    private fun saveDuration() {
        val id = conversationId ?: return
        val conversation = store.conversation(id) ?: return
        val seconds = ((store.nowMillis() - startedAtMillis) / 1000).toInt()
        save(conversation.copy(durationSeconds = conversation.durationSeconds + seconds))
    }

    private fun save(conversation: Conversation) {
        store.update { data ->
            data.copy(conversations = data.conversations.filter { it.id != conversation.id } + conversation)
        }
    }

    companion object {
        /** Hvor mange tidligere meldinger som sendes med når en samtale fortsettes. */
        private const val MaxHistory = 60
        private val json = Json { encodeDefaults = true }
        private var instance: KompisLive? = null

        fun get(store: KompisStore, bridge: NativeBridge): KompisLive =
            instance ?: KompisLive(store, bridge).also { instance = it }

        private fun newId(): String = (1..12).map { "abcdefghijklmnopqrstuvwxyz0123456789".random() }.joinToString("")

        /** Tittelen på en samtale: starten på det første brukeren sa. */
        fun titleFrom(text: String): String {
            val firstSentence = text.split('.', '?', '!').first().trim()
            return if (firstSentence.length <= 48) firstSentence else firstSentence.take(45).trimEnd() + "…"
        }

        fun systemInstruction(personality: String): String =
            personality.trim() + "\n\n" +
                "Dette er en muntlig samtale. Svarene dine blir lest opp, så hold dem korte og naturlige, " +
                "uten lister, overskrifter eller emojier."
    }
}
