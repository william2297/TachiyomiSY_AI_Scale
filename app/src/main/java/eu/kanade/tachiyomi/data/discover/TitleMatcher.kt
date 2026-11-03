package eu.kanade.tachiyomi.data.discover

import java.text.Normalizer

/**
 * Finds the same series under different names, for example an AniList title in a source's search results.
 *
 * A match is [Quality.EXACT] when the names are equal after cleaning (case, accents, apostrophes, brackets, a leading
 * "the" and punctuation are ignored). [Quality.CLOSE] is a near miss such as a one-letter spelling difference in a
 * long title. Short titles never match loosely, and word order matters, so "Sweet Home" is not "Home Sweet Home".
 */
object TitleMatcher {

    enum class Quality { EXACT, CLOSE, NONE }

    private const val CLOSE_MIN_LENGTH = 8
    private const val CLOSE_MIN_SIMILARITY = 0.9

    private val combiningMarks = Regex("\\p{Mn}+")
    private val bracketed = Regex("\\(.*?\\)|\\[.*?]")
    private val apostrophes = Regex("['’‘`´]")
    private val leadingArticle = Regex("^(the|a|an)\\s+")

    fun normalize(title: String): String {
        val decomposed = Normalizer.normalize(title, Normalizer.Form.NFKD).replace(combiningMarks, "")
        val cleaned = decomposed
            .lowercase()
            .replace(bracketed, " ")
            .replace(apostrophes, "")
            .map { if (it.isLetterOrDigit()) it else ' ' }
            .joinToString("")
            .trim()
            .replace(Regex("\\s+"), " ")
        return cleaned.replace(leadingArticle, "")
    }

    fun match(knownNames: Collection<String>, candidate: String): Quality {
        val wanted = normalize(candidate)
        if (wanted.isEmpty()) return Quality.NONE
        var best = Quality.NONE
        for (name in knownNames) {
            val known = normalize(name)
            if (known.isEmpty()) continue
            if (known == wanted) return Quality.EXACT
            if (best == Quality.NONE && isClose(known, wanted)) best = Quality.CLOSE
        }
        return best
    }

    /**
     * The candidate that is the same series, or null. An exact match wins; a close match is accepted only when it is
     * the only candidate that is at least close, so a wrong guess is not shown over a missing result.
     */
    fun <T> best(knownNames: Collection<String>, candidates: List<T>, titleOf: (T) -> String): Pair<T, Quality>? {
        val scored = candidates.map { it to match(knownNames, titleOf(it)) }
        val exact = scored.firstOrNull { it.second == Quality.EXACT }
        if (exact != null) return exact
        val close = scored.filter { it.second == Quality.CLOSE }
        return close.singleOrNull()
    }

    private fun isClose(a: String, b: String): Boolean {
        if (a.length < CLOSE_MIN_LENGTH || b.length < CLOSE_MIN_LENGTH) return false
        val longest = maxOf(a.length, b.length)
        if (kotlin.math.abs(a.length - b.length) > longest * (1 - CLOSE_MIN_SIMILARITY)) return false
        return 1.0 - distance(a, b).toDouble() / longest >= CLOSE_MIN_SIMILARITY
    }

    private fun distance(a: String, b: String): Int {
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in 1..a.length) {
            current[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                current[j] = minOf(current[j - 1] + 1, previous[j] + 1, previous[j - 1] + cost)
            }
            val swap = previous
            previous = current
            current = swap
        }
        return previous[b.length]
    }
}
