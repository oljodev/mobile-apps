package no.jodal.salat

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DeepLinksTest {
    @Test
    fun parsesAppAndAction() {
        assertEquals(DeepLink("kompis", "start"), DeepLinks.parse("salat://kompis/start"))
        assertEquals(DeepLink("rise", null), DeepLinks.parse("salat://rise"))
        assertEquals(DeepLink("ekko", null), DeepLinks.parse("salat://Ekko/?fra=widget"))
        assertNull(DeepLinks.parse("https://example.com"))
    }

    @Test
    fun anActionOnlyHappensOnce() {
        DeepLinks.open("salat://kompis/start")
        assertTrue(DeepLinks.consume("kompis", "start"))
        assertFalse(DeepLinks.consume("kompis", "start"))
    }
}
