package no.jodal.salat

/**
 * Det appen trenger fra iOS som ikke kan gjøres i Kotlin.
 *
 * Implementeres i Swift i `iosApp/Salat/IosNativeBridge.swift`.
 */
interface NativeBridge {
    /** Når SideStore-signeringen går ut, i millisekunder siden 1970, eller `null` hvis ukjent. */
    fun signingExpiresAtMillis(): Long?

    /** Planlegger et varsel dagen før signeringen går ut. Erstatter et tidligere planlagt varsel. */
    fun scheduleExpiryReminder()

    /** Sender et testvarsel etter [delaySeconds] sekunder. */
    fun sendTestNotification(delaySeconds: Int)
}
