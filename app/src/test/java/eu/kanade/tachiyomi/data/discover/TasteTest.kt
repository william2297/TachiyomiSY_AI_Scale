package eu.kanade.tachiyomi.data.discover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class TasteTest {

    private fun tags(vararg pairs: Pair<String, Int>) = pairs.map { TitleTag(it.first, it.second) }

    private val now = 1_700_000_000_000L
    private val day = 86_400_000L

    @Test
    fun `the tag most of the read titles share gets the top weight`() {
        val weights = Taste.weights(
            listOf(
                tags("Martial Arts" to 90, "Revenge" to 70),
                tags("Martial Arts" to 80, "Romance" to 60),
            ),
            emptyList(),
            now,
        )
        assertEquals(1f, weights.getValue("Martial Arts"), 0.0001f)
        assertTrue(weights.getValue("Martial Arts") > weights.getValue("Revenge"))
    }

    @Test
    fun `tags nearly every series has count for less`() {
        val weights = Taste.weights(listOf(tags("Male Protagonist" to 100, "Wuxia" to 90)), emptyList(), now)
        assertTrue(weights.getValue("Wuxia") > weights.getValue("Male Protagonist"))
    }

    @Test
    fun `newer reads count more than older ones`() {
        val weights = Taste.weights(listOf(tags("New Tag" to 80), tags("Old Tag" to 80)), emptyList(), now)
        assertTrue(weights.getValue("New Tag") > weights.getValue("Old Tag"))
    }

    @Test
    fun `a rejected tag is lowered and the penalty fades with time`() {
        val read = listOf(tags("Romance" to 80, "Wuxia" to 80))
        val fresh = Taste.weights(read, listOf(NegativeTag("Romance", now)), now)
        assertTrue(fresh.getValue("Wuxia") > (fresh["Romance"] ?: 0f))
        val old = Taste.weights(read, listOf(NegativeTag("Romance", now - 40 * day)), now)
        assertEquals(old.getValue("Wuxia"), old.getValue("Romance"), 0.0001f)
    }

    @Test
    fun `no read titles with tags gives no weights`() {
        assertTrue(Taste.weights(emptyList(), emptyList(), now).isEmpty())
        assertTrue(Taste.weights(listOf(emptyList()), emptyList(), now).isEmpty())
    }

    @Test
    fun `fit is highest for the same tags and zero for unrelated ones`() {
        val weights = mapOf("Wuxia" to 1f, "Cultivation" to 0.8f)
        val same = Taste.fit(weights, tags("Wuxia" to 90, "Cultivation" to 80))
        val other = Taste.fit(weights, tags("Romance" to 90))
        assertTrue(same > 0.9f)
        assertEquals(0f, other, 0.0001f)
    }

    @Test
    fun `diversify prefers a different series over a near copy`() {
        data class Item(val name: String, val score: Float, val tags: List<TitleTag>)
        val a1 = Item("A1", 0.90f, tags("Wuxia" to 90, "Revenge" to 90))
        val a2 = Item("A2", 0.89f, tags("Wuxia" to 90, "Revenge" to 90))
        val other = Item("Other", 0.70f, tags("Sports" to 90))
        val picked = Taste.diversify(listOf(a1, a2, other), 2, 0.7f, { it.score }, { it.tags })
        assertEquals(listOf("A1", "Other"), picked.map { it.name })
        assertFalse(picked.any { it.name == "A2" })
    }
}
