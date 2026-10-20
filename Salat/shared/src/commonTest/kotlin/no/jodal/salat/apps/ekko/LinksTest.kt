package no.jodal.salat.apps.ekko

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinksTest {
    private fun youTubeId(text: String) = (EkkoLink.parse(text) as? EkkoLink.YouTube)?.videoId

    @Test
    fun findsYouTubeIdsInAllKindsOfLinks() {
        val id = "dQw4w9WgXcQ"
        assertEquals(id, youTubeId("https://www.youtube.com/watch?v=$id"))
        assertEquals(id, youTubeId("https://m.youtube.com/watch?feature=share&v=$id&t=12"))
        assertEquals(id, youTubeId("https://youtube.com/shorts/$id?si=abc123"))
        assertEquals(id, youTubeId("https://youtu.be/$id?si=abc123"))
        assertEquals(id, youTubeId("https://www.youtube.com/embed/$id"))
        assertEquals(id, youTubeId("https://music.youtube.com/watch?v=$id&list=RD"))
    }

    @Test
    fun findsTheLinkInSharedText() {
        assertEquals("abcdefghijk", youTubeId("Se denne! https://youtube.com/shorts/abcdefghijk?si=x."))
        assertEquals("abcdefghijk", youTubeId("youtube.com/shorts/abcdefghijk"))
    }

    @Test
    fun otherLinksAreWebPages() {
        val link = EkkoLink.parse("https://www.snapchat.com/spotlight/W7_EDlXWTBiXAEEniNoMPwAAYZm1qaWZ5eW91AZmsWo6NAZmsWo57AAAAAQ")
        assertTrue(link is EkkoLink.Web && link.isSnapchat)
        assertTrue(EkkoLink.parse("https://example.com/video.mp4") is EkkoLink.Web)
        // En YouTube-lenke uten video er ikke noe å hente lyd fra som YouTube.
        assertTrue(EkkoLink.parse("https://www.youtube.com/@kanal") is EkkoLink.Web)
    }

    @Test
    fun textWithoutLinkGivesNothing() {
        assertNull(EkkoLink.parse("bare litt tekst"))
        assertNull(EkkoLink.parse(""))
    }
}
