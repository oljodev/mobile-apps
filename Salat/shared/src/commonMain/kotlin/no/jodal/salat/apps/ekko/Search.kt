package no.jodal.salat.apps.ekko

/** Søket og filteret over lydene. [person] er hvem som sier det, eller `null` for alle. */
data class ClipFilter(val query: String = "", val person: String? = null) {
    val isActive: Boolean get() = query.isNotBlank() || person != null

    fun matches(clip: Clip): Boolean {
        if (person != null && !clip.artist.equals(person, ignoreCase = true)) return false
        val words = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        val text = "${clip.title} ${clip.artist}".lowercase()
        return words.all { it in text }
    }

    fun matches(playlist: Playlist): Boolean {
        val words = query.trim().lowercase().split(' ').filter { it.isNotEmpty() }
        return words.all { it in playlist.name.lowercase() }
    }

    /** Navnet på køen når man spiller det som er funnet. */
    val label: String
        get() = when {
            person != null && query.isNotBlank() -> "$person: «${query.trim()}»"
            person != null -> person
            else -> "«${query.trim()}»"
        }
}

/** Hvem som sier noe i lydene, de med flest lyder først. */
fun people(clips: List<Clip>): List<String> = clips
    .map { it.artist.trim() }
    .filter { it.isNotEmpty() }
    .groupBy { it.lowercase() }
    .values
    .sortedWith(compareByDescending<List<String>> { it.size }.thenBy { it.first().lowercase() })
    .map { it.first() }
