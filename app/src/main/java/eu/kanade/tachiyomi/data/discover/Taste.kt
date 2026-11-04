package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.Serializable
import kotlin.math.sqrt

/** A tag AniList gives a series, with how strongly it fits (0 to 100). Spoiler and format tags are left out. */
@Serializable
data class TitleTag(val name: String, val rank: Int)

/** A tag of a title the user marked "Not interested", with the time, so its effect fades. */
@Serializable
data class NegativeTag(val tag: String, val atMillis: Long)

/** What the app remembers about the user's taste between refreshes. */
@Serializable
data class TasteData(
    /** Normalized title to its tags; an empty list means AniList was asked and has none. */
    val titles: Map<String, List<TitleTag>> = emptyMap(),
    val negatives: List<NegativeTag> = emptyList(),
)

/**
 * Taste from tags: tag weights from what the user read, how well a title fits them, and a re-ranking that keeps a
 * row from filling up with near copies of the same series.
 */
object Taste {

    /** Tags nearly every series has, so they say little about taste. */
    private val COMMON_TAGS = setOf("Male Protagonist", "Female Protagonist")
    private const val COMMON_TAG_FACTOR = 0.35f
    private const val NEGATIVE_STRENGTH = 0.4f
    private const val NEGATIVE_DAYS = 30f
    private const val DAY_MILLIS = 86_400_000f
    private const val MIN_WEIGHT = 0.05f

    /**
     * Tag weights from the tags of recently read titles, newest first (newer counts more), minus a fading penalty for
     * tags of titles the user rejected. Scaled so the strongest tag is 1.
     */
    fun weights(recentTags: List<List<TitleTag>>, negatives: List<NegativeTag>, nowMillis: Long): Map<String, Float> {
        val sums = mutableMapOf<String, Float>()
        val n = recentTags.size
        recentTags.forEachIndexed { index, tags ->
            val recency = if (n <= 1) 1f else 1f - 0.6f * index / (n - 1)
            for (tag in tags) {
                val factor = if (tag.name in COMMON_TAGS) COMMON_TAG_FACTOR else 1f
                sums[tag.name] = (sums[tag.name] ?: 0f) + recency * (tag.rank / 100f) * factor
            }
        }
        for (negative in negatives) {
            val current = sums[negative.tag] ?: continue
            val fade = (1f - (nowMillis - negative.atMillis) / DAY_MILLIS / NEGATIVE_DAYS).coerceAtLeast(0f)
            if (fade > 0f) sums[negative.tag] = (current - NEGATIVE_STRENGTH * fade).coerceAtLeast(0f)
        }
        val max = sums.values.maxOrNull()?.takeIf { it > 0f } ?: return emptyMap()
        return sums.mapValues { it.value / max }.filterValues { it >= MIN_WEIGHT }
    }

    /** How well the tags of a title fit the weights, 0 to 1 (cosine of the two tag vectors). */
    fun fit(weights: Map<String, Float>, tags: List<TitleTag>): Float {
        if (weights.isEmpty() || tags.isEmpty()) return 0f
        var dot = 0f
        var titleNorm = 0f
        for (tag in tags) {
            val v = tag.rank / 100f
            dot += (weights[tag.name] ?: 0f) * v
            titleNorm += v * v
        }
        val profileNorm = sqrt(weights.values.sumOf { (it * it).toDouble() }.toFloat())
        val denominator = sqrt(titleNorm) * profileNorm
        return if (denominator <= 0f) 0f else dot / denominator
    }

    /** How alike two titles are by their tags, 0 to 1. */
    fun similarity(a: List<TitleTag>, b: List<TitleTag>): Float {
        if (a.isEmpty() || b.isEmpty()) return 0f
        val bMap = b.associate { it.name to it.rank / 100f }
        var dot = 0f
        var an = 0f
        for (tag in a) {
            val v = tag.rank / 100f
            dot += v * (bMap[tag.name] ?: 0f)
            an += v * v
        }
        val bn = b.sumOf { (it.rank / 100f * it.rank / 100f).toDouble() }.toFloat()
        val denominator = sqrt(an) * sqrt(bn)
        return if (denominator <= 0f) 0f else dot / denominator
    }

    /**
     * Picks [count] items one by one, each time the one with the best score minus its likeness to what was already
     * picked (maximal marginal relevance), so the row stays varied. [lambda] near 1 favors score, near 0 variety.
     */
    fun <T> diversify(
        candidates: List<T>,
        count: Int,
        lambda: Float,
        score: (T) -> Float,
        tagsOf: (T) -> List<TitleTag>,
    ): List<T> {
        val pool = candidates.toMutableList()
        val picked = mutableListOf<T>()
        while (picked.size < count && pool.isNotEmpty()) {
            var best = pool.first()
            var bestValue = Float.NEGATIVE_INFINITY
            for (candidate in pool) {
                val closest = picked.maxOfOrNull { similarity(tagsOf(candidate), tagsOf(it)) } ?: 0f
                val value = lambda * score(candidate) - (1f - lambda) * closest
                if (value > bestValue) {
                    bestValue = value
                    best = candidate
                }
            }
            picked += best
            pool -= best
        }
        return picked
    }
}
