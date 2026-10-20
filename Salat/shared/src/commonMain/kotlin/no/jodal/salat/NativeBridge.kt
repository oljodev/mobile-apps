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

    /** Leser en hemmelighet, f.eks. en API-nøkkel, fra nøkkelringen, eller `null`. */
    fun loadSecret(key: String): String?

    /** Lagrer en hemmelighet i nøkkelringen. Tom tekst sletter den. */
    fun saveSecret(key: String, value: String)

    /**
     * Gjør alarmene og varslene fra Rise lik [planJson], en `RisePlan` som JSON.
     * [onResult] kalles med en feilmelding, eller `null` hvis alt ble planlagt.
     */
    fun applyRiseSchedule(planJson: String, onResult: (String?) -> Unit)

    /** Spiller av en lyd fra Rise, f.eks. `rise-myk`. */
    fun previewSound(sound: String)

    fun stopSoundPreview()

    /**
     * Starter en samtale med Gemini Live. [configJson] er en `LiveConfig` som JSON.
     * Lyd og tilkobling håndteres i Swift, og [listener] får vite hva som skjer.
     */
    fun startLiveSession(configJson: String, listener: LiveListener)

    fun stopLiveSession()

    fun setLiveMuted(muted: Boolean)

    /**
     * Starter en ny økt i den pågående samtalen, med [historyJson] (en liste av `HistoryTurn`) som
     * tekst. Det er billigere enn at Gemini husker samtalen som lyd. Skjer først når det er stille.
     */
    fun rollLiveSession(historyJson: String)
}

/** Det Swift forteller om en samtale med Gemini Live. Kalles alltid på hovedtråden. */
interface LiveListener {
    /** `connecting`, `listening`, `thinking`, `speaking`, `reconnecting` eller `ended`. */
    fun onStatus(status: String)

    /** Lydnivået til den som snakker, fra 0 til 1. */
    fun onLevel(level: Float)

    /** En bit av det brukeren sier. */
    fun onUserText(text: String)

    /** En bit av det Kompis sier. */
    fun onKompisText(text: String)

    /** Kompis er ferdig med å snakke. */
    fun onTurnComplete()

    /** Brukeren avbrøt Kompis. */
    fun onInterrupted()

    /** Hvor lyden går, f.eks. «AirPods Pro». */
    fun onAudioRoute(name: String)

    /** Noe gikk galt, og samtalen er stoppet. */
    fun onError(message: String)

    /** En kort oppsummering av hva som skjer i tilkoblingen, for å finne feil. */
    fun onDebug(text: String)
}
