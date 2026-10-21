package no.jodal.salat.apps.ekko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class SearchTest {
    private fun clip(title: String, artist: String = "") =
        Clip(title, title, artist, sourceSeconds = 5.0, endSeconds = 5.0, addedAtMillis = 0)

    @Test
    fun matchesEveryWordInTitleOrPerson() {
        val clip = clip("Jeg er så lei av mandager", "Garfield")
        assertTrue(ClipFilter("mandager").matches(clip))
        assertTrue(ClipFilter("garfield lei").matches(clip))
        assertTrue(ClipFilter("  ").matches(clip))
        assertFalse(ClipFilter("tirsdager").matches(clip))
    }

    @Test
    fun filtersByPerson() {
        assertTrue(ClipFilter(person = "garfield").matches(clip("Hei", "Garfield")))
        assertFalse(ClipFilter(person = "Garfield").matches(clip("Hei", "Odie")))
        assertFalse(ClipFilter(person = "Garfield").matches(clip("Hei")))
    }

    @Test
    fun peopleWithMostSoundsComeFirst() {
        val clips = listOf(clip("a", "Odie"), clip("b", "Garfield"), clip("c", "garfield"), clip("d"), clip("e", "Jon"))
        assertEquals(listOf("Garfield", "Jon", "Odie"), people(clips))
    }
}
