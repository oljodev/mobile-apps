package no.jodal.salat.apps.kompis

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import no.jodal.salat.LiveListener
import no.jodal.salat.NativeBridge
import no.jodal.salat.apps.kompis.ui.LiveStatus

class KompisLiveTest {
    // KompisStore og KompisLive finnes bare én gang, så alle testene må bruke samme bro.
    private val bridge = FakeBridge
    private lateinit var store: KompisStore
    private lateinit var live: KompisLive

    @BeforeTest
    fun setUp() {
        bridge.startedWith = null
        store = KompisStore.get(bridge)
        store.saveApiKey("test-key")
        store.update { KompisData() }
        live = KompisLive.get(store, bridge)
        live.end()
        bridge.startedWith = null
    }

    @Test
    fun savesTheConversationTurnByTurn() {
        live.start(continuing = null)
        assertEquals(LiveStatus.Connecting, live.state.status)
        assertEquals(emptyList(), store.data.conversations, "en tom samtale skal ikke lagres")

        live.onUserText("Hei! Jeg går ")
        live.onUserText("tur nå.")
        live.onKompisText("Så ")
        live.onKompisText("fint!")
        assertEquals("Så fint!", live.state.kompisDraft)
        live.onTurnComplete()

        val conversation = store.data.conversations.single()
        assertEquals("Hei", conversation.title)
        assertEquals(listOf(Role.User to "Hei! Jeg går tur nå.", Role.Kompis to "Så fint!"), conversation.messages.map { it.role to it.text })
        assertNull(live.state.userDraft)
        assertNull(live.state.kompisDraft)
    }

    @Test
    fun interruptingKeepsWhatKompisHadSaid() {
        live.start(continuing = null)
        live.onUserText("Fortell en lang historie")
        live.onKompisText("Det var en gang")
        live.onInterrupted()
        live.onUserText("Stopp")
        live.end()

        assertEquals(
            listOf("Fortell en lang historie", "Det var en gang", "Stopp"),
            store.data.conversations.single().messages.map { it.text },
        )
        assertEquals(LiveStatus.Ended, live.state.status)
    }

    @Test
    fun continuingSendsTheHistory() {
        val earlier = Conversation(
            id = "c1",
            title = "Tidligere",
            createdAtMillis = 0,
            updatedAtMillis = 0,
            messages = listOf(Message(Role.User, "Hei", 0), Message(Role.Kompis, "Hallo!", 0)),
        )
        store.update { it.copy(conversations = listOf(earlier)) }
        live.start(continuing = earlier)

        val config = Json.decodeFromString(LiveConfig.serializer(), bridge.startedWith!!)
        assertEquals(listOf("user" to "Hei", "model" to "Hallo!"), config.history.map { it.role to it.text })
        assertEquals(LiveModel, config.model)

        live.onUserText("Husker du meg?")
        live.onTurnComplete()
        val messages = store.conversation("c1")!!.messages
        assertEquals(3, messages.size)
        assertEquals("Tidligere", store.conversation("c1")!!.title, "tittelen skal ikke endres når samtalen fortsetter")
    }

    @Test
    fun withoutKeyNothingStarts() {
        store.saveApiKey("")
        live.start(continuing = null)
        assertEquals(LiveStatus.NeedsApiKey, live.state.status)
        assertNull(bridge.startedWith)
        assertTrue(!live.isActive)
    }

    @Test
    fun titles() {
        assertEquals("Kan du hjelpe meg med middag", KompisLive.titleFrom("Kan du hjelpe meg med middag? Jeg har egg."))
        assertEquals(46, KompisLive.titleFrom("a".repeat(80)).length)
    }
}

private object FakeBridge : NativeBridge {
    private val storage = mutableMapOf<String, String>()
    var startedWith: String? = null

    override fun signingExpiresAtMillis(): Long? = null
    override fun scheduleExpiryReminder() = Unit
    override fun sendTestNotification(delaySeconds: Int) = Unit
    override fun scheduleTestAlarm(delaySeconds: Int, snoozeMinutes: Int, onResult: (String?) -> Unit) = Unit
    override fun loadText(key: String): String? = storage[key]
    override fun saveText(key: String, value: String) {
        storage[key] = value
    }
    override fun loadSecret(key: String): String? = storage["secret.$key"]
    override fun saveSecret(key: String, value: String) {
        storage["secret.$key"] = value
    }
    override fun applyRiseSchedule(planJson: String, onResult: (String?) -> Unit) = Unit
    override fun previewSound(sound: String) = Unit
    override fun stopSoundPreview() = Unit
    override fun startLiveSession(configJson: String, listener: LiveListener) {
        startedWith = configJson
    }
    override fun stopLiveSession() = Unit
    override fun setLiveMuted(muted: Boolean) = Unit
}
