package no.jodal.salat.apps.ekko

import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull

class PlayQueueTest {
    private val ids = listOf("a", "b", "c", "d", "e")

    @Test
    fun playsInOrderFromTheChosenSound() {
        val queue = PlayQueue.start(ids, startAt = "c", shuffle = false)!!
        assertEquals("c", queue.current)
        assertEquals("d", queue.next(Repeat.Off, false)!!.current)
    }

    @Test
    fun stopsAtTheEndUnlessRepeating() {
        val last = PlayQueue.start(ids, startAt = "e", shuffle = false)!!
        assertNull(last.next(Repeat.Off, false))
        assertEquals("a", last.next(Repeat.All, false)!!.current)
    }

    @Test
    fun shuffleStartsWithTheChosenSoundAndPlaysEveryoneOnce() {
        val queue = PlayQueue.start(ids, startAt = "c", shuffle = true, Random(1))!!
        assertEquals("c", queue.current)
        assertEquals(ids.toSet(), queue.order.toSet())
        assertEquals(ids.size, queue.order.size)
    }

    @Test
    fun reshufflingNeverRepeatsTheSameSoundTwiceInARow() {
        repeat(50) { seed ->
            val queue = PlayQueue.start(ids, null, shuffle = true, Random(seed))!!.copy(index = ids.lastIndex)
            val next = queue.next(Repeat.All, shuffle = true, Random(seed + 100))!!
            assertNotEquals(queue.current, next.current)
            assertEquals(ids.toSet(), next.order.toSet())
        }
    }

    @Test
    fun turningShuffleOffGoesBackToTheListOrder() {
        val shuffled = PlayQueue.start(ids, startAt = "d", shuffle = true, Random(3))!!
        val ordered = shuffled.withShuffle(false)
        assertEquals("d", ordered.current)
        assertEquals("e", ordered.next(Repeat.Off, false)!!.current)
    }

    @Test
    fun removingASoundKeepsThePlace() {
        val queue = PlayQueue.start(ids, startAt = "c", shuffle = false)!!
        assertEquals("c", queue.without("a")!!.current)
        assertEquals("d", queue.without("c")!!.current)
        assertNull(PlayQueue.start(listOf("a"), null, false)!!.without("a"))
    }
}
