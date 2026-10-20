package no.jodal.salat.apps.ekko

/** En lenke Ekko kan hente lyd fra. */
sealed interface EkkoLink {
    val url: String

    data class YouTube(val videoId: String, override val url: String) : EkkoLink

    /** En annen nettside, f.eks. Snapchat Spotlight. Ekko leter etter en video på siden. */
    data class Web(override val url: String) : EkkoLink {
        val isSnapchat: Boolean get() = hostOf(url).let { it == "snapchat.com" || it.endsWith(".snapchat.com") }
    }

    companion object {
        /**
         * Finner lenken i [text], som kan være mer enn bare lenken (f.eks. «Se denne! https://…» fra
         * delingsarket). Gir `null` hvis det ikke er noen lenke der.
         */
        fun parse(text: String): EkkoLink? {
            val url = findUrl(text) ?: return null
            val id = youTubeId(url)
            return if (id != null) YouTube(id, url) else Web(url)
        }

        private val urlPattern = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val bareYouTube = Regex("""(?:www\.|m\.)?(?:youtube\.com|youtu\.be)/[^\s<>"']+""", RegexOption.IGNORE_CASE)
        private val idPattern = Regex("""^[A-Za-z0-9_-]{11}$""")

        private fun findUrl(text: String): String? {
            urlPattern.find(text)?.let { return it.value.trimEnd('.', ',', ')', '!', '?') }
            return bareYouTube.find(text)?.let { "https://" + it.value }
        }

        /** IDen til en YouTube-video, fra vanlige lenker, Shorts, youtu.be og innebygde videoer. */
        fun youTubeId(url: String): String? {
            val host = hostOf(url)
            val path = url.substringAfter("://").substringAfter('/', "").substringBefore('#')
            val pathOnly = path.substringBefore('?')
            val query = path.substringAfter('?', "")
            val candidate = when (host) {
                "youtu.be" -> pathOnly.substringBefore('/')
                "youtube.com", "m.youtube.com", "music.youtube.com", "youtube-nocookie.com" -> {
                    val parts = pathOnly.split('/')
                    when (parts.firstOrNull()) {
                        "watch" -> query.split('&').firstOrNull { it.startsWith("v=") }?.removePrefix("v=")
                        "shorts", "embed", "live", "v" -> parts.getOrNull(1)
                        else -> null
                    }
                }
                else -> null
            }
            return candidate?.takeIf { idPattern.matches(it) }
        }

        internal fun hostOf(url: String): String =
            url.substringAfter("://").substringBefore('/').substringBefore('?').substringBefore(':').lowercase().removePrefix("www.")
    }
}
