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

    /**
     * Planlegger en testalarm med AlarmKit etter [delaySeconds] sekunder. [snoozeMinutes] = 0 gir ingen slumring.
     * [onResult] kalles med en feilmelding, eller `null` hvis alarmen ble planlagt.
     */
    fun scheduleTestAlarm(delaySeconds: Int, snoozeMinutes: Int, onResult: (String?) -> Unit)

    /** Leser tekst som er lagret med [saveText], eller `null`. */
    fun loadText(key: String): String?

    /** Lagrer tekst på telefonen. */
    fun saveText(key: String, value: String)

    /**
     * Gjør alarmene og varslene fra Rise lik [planJson], en JSON-liste av `PlannedItem`.
     * [onResult] kalles med en feilmelding, eller `null` hvis alt ble planlagt.
     */
    fun applyRiseSchedule(planJson: String, onResult: (String?) -> Unit)

    /** Spiller av en lyd fra Rise, f.eks. `rise-myk`. */
    fun previewSound(sound: String)

    fun stopSoundPreview()

    /** Tittelen på vekkealarmen som venter på «Jeg er oppe», eller `null`. */
    fun pendingAwakeCheck(): String?

    /** Bekrefter at man er oppe, og avlyser reservealarmen. */
    fun confirmAwake()
}
