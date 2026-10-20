package no.jodal.salat.apps.ekko

import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import no.jodal.salat.FakeBridge

class EkkoPlayerTest {
    private val bridge = FakeBridge
    private val store = EkkoStore.get(bridge)
    private val player = EkkoPlayer.get(store, bridge)
    private val clips = listOf(clip("a"), clip("b"), clip("c"))

    private fun clip(id: String) = Clip(id, title = id, sourceSeconds = 10.0, startSeconds = 2.0, endSeconds = 5.0, addedAtMillis = 0)

    @BeforeTest
    fun setUp() {
        player.stop()
        store.update { EkkoData(clips = clips) }
        bridge.ekkoCalls.clear()
    }

    @Test
    fun playsFromTheStartOfTheClipAndMovesOn() {
        player.play(clips, EkkoPlayer.QueueSource.All, startAt = clips[1])
        assertEquals("play b from 2.0", bridge.ekkoCalls.last())
        assertTrue(player.isPlaying)

        player.onPosition(3.5)
        assertEquals(1.5, player.position)

        player.onFinished()
        assertEquals("c", player.current?.id)
        assertEquals("play c from 2.0", bridge.ekkoCalls.last())
    }

    @Test
    fun stopsAfterTheLastClipWithoutRepeat() {
        player.play(clips, EkkoPlayer.QueueSource.All, startAt = clips[2])
        player.onFinished()
        assertFalse(player.isPlaying)
        assertEquals("a", player.current?.id, "køen går tilbake til starten")
    }

    @Test
    fun repeatOnePlaysTheSameClipAgain() {
        store.setRepeat(Repeat.One)
        player.play(clips, EkkoPlayer.QueueSource.All, startAt = clips[0])
        player.onFinished()
        assertEquals("a", player.current?.id)
        assertEquals(listOf("play a from 2.0", "play a from 2.0"), bridge.ekkoCalls.filter { it.startsWith("play") })
        store.setRepeat(Repeat.Off)
    }

    @Test
    fun resumesWhereItPausedAfterAPreview() {
        player.play(clips, EkkoPlayer.QueueSource.All, startAt = clips[0])
        player.onPosition(3.0)
        player.pause()
        player.startPreview("b", 1.0, 4.0)
        player.resume()
        assertNull(player.preview)
        assertEquals("play a from 3.0", bridge.ekkoCalls.last())
    }

    @Test
    fun deletingThePlayingClipSkipsToTheNext() {
        player.play(clips, EkkoPlayer.QueueSource.All, startAt = clips[0])
        store.deleteClip("a")
        player.forget("a")
        assertEquals("b", player.current?.id)
        assertEquals("play b from 2.0", bridge.ekkoCalls.last())
    }
}
