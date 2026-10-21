package no.jodal.salat

/** Små vibrasjoner, som i resten av iOS. */
object Haptics {
    /** Settes når appen starter. */
    var bridge: NativeBridge? = null

    /** Et lett trykk, f.eks. på en knapp. */
    fun tap() = play("light")

    /** Et lite klikk når et valg endres, f.eks. en fane eller en tidel i klippingen. */
    fun select() = play("selection")

    /** Et fastere trykk, f.eks. når noe holdes inne eller treffer en kant. */
    fun firm() = play("rigid")

    fun success() = play("success")

    fun warning() = play("warning")

    private fun play(kind: String) {
        bridge?.haptic(kind)
    }
}
