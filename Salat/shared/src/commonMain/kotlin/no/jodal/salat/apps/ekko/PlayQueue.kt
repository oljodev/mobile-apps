package no.jodal.salat.apps.ekko

import kotlin.random.Random

/**
 * Køen som spilles av: lydene i rekkefølgen de skal spilles ([order]) og hvor man er ([index]).
 * [original] er rekkefølgen i spillelisten, så man kan slå av tilfeldig rekkefølge igjen.
 */
data class PlayQueue(
    val original: List<String>,
    val order: List<String>,
    val index: Int,
) {
    val current: String? get() = order.getOrNull(index)
    val hasNext: Boolean get() = index < order.lastIndex

    /**
     * Neste lyd, eller `null` når køen er ferdig. Med [Repeat.All] starter den på nytt, og i
     * tilfeldig rekkefølge stokkes den på nytt, uten at samme lyd kommer to ganger på rad.
     */
    fun next(repeat: Repeat, shuffle: Boolean, random: Random = Random): PlayQueue? = when {
        hasNext -> copy(index = index + 1)
        repeat == Repeat.Off -> null
        shuffle -> {
            var reshuffled = original.shuffled(random)
            if (reshuffled.size > 1 && reshuffled.first() == current) {
                reshuffled = reshuffled.drop(1) + reshuffled.first()
            }
            copy(order = reshuffled, index = 0)
        }
        else -> copy(order = original, index = 0)
    }

    fun previous(): PlayQueue = copy(index = (index - 1).coerceAtLeast(0))

    /** Slår tilfeldig rekkefølge av eller på, uten å bytte lyden som spilles nå. */
    fun withShuffle(shuffle: Boolean, random: Random = Random): PlayQueue {
        val now = current ?: return this
        return if (shuffle) {
            copy(order = listOf(now) + (original - now).shuffled(random), index = 0)
        } else {
            copy(order = original, index = original.indexOf(now).coerceAtLeast(0))
        }
    }

    /** Køen uten [id], f.eks. når lyden slettes. `null` hvis køen blir tom. */
    fun without(id: String): PlayQueue? {
        if (id !in order) return this
        val removedBefore = order.take(index).count { it == id }
        val newOrder = order - id
        if (newOrder.isEmpty()) return null
        return PlayQueue(original - id, newOrder, (index - removedBefore).coerceIn(0, newOrder.lastIndex))
    }

    companion object {
        /** Starter en kø med [ids]. [startAt] spilles først, også når rekkefølgen er tilfeldig. */
        fun start(ids: List<String>, startAt: String?, shuffle: Boolean, random: Random = Random): PlayQueue? {
            if (ids.isEmpty()) return null
            val first = startAt?.takeIf { it in ids }
            return when {
                shuffle && first != null -> PlayQueue(ids, listOf(first) + (ids - first).shuffled(random), 0)
                shuffle -> PlayQueue(ids, ids.shuffled(random), 0)
                else -> PlayQueue(ids, ids, first?.let(ids::indexOf) ?: 0)
            }
        }
    }
}
