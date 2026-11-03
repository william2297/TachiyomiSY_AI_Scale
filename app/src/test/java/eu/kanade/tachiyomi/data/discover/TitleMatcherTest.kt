package eu.kanade.tachiyomi.data.discover

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class TitleMatcherTest {
    private fun quality(known: List<String>, candidate: String) = TitleMatcher.match(known, candidate)

    @Test
    fun `curly and straight apostrophes are the same title`() {
        // Found missing in a check against real titles: the curly apostrophe turned into a space
        val known = listOf("The Extra’s Academy Survival Guide")
        assertEquals(TitleMatcher.Quality.EXACT, quality(known, "Extra's Academy Survival Guide"))
        assertEquals(TitleMatcher.Quality.EXACT, quality(listOf("Lout of Count’s Family"), "Lout of Counts Family"))
    }

    @Test
    fun `leading article, case, accents and brackets are ignored`() {
        val known = listOf("The Greatest Estate Developer")
        assertEquals(TitleMatcher.Quality.EXACT, quality(known, "Greatest Estate Developer"))
        assertEquals(TitleMatcher.Quality.EXACT, quality(known, "THE GREATEST ESTATE DEVELOPER (Manhwa)"))
        assertEquals(TitleMatcher.Quality.EXACT, quality(listOf("Café Mystery"), "Cafe Mystery"))
    }

    @Test
    fun `any alternative name can match`() {
        val known = listOf("Omniscient Reader", "Jeonjijeok Dokja Sijeom", "전지적 독자 시점")
        assertEquals(TitleMatcher.Quality.EXACT, quality(known, "Jeonjijeok Dokja Sijeom"))
        assertEquals(TitleMatcher.Quality.EXACT, quality(known, "전지적 독자 시점"))
    }

    @Test
    fun `word order matters so a different series is not matched`() {
        assertEquals(TitleMatcher.Quality.NONE, quality(listOf("Sweet Home"), "Home Sweet Home (Bori)"))
    }

    @Test
    fun `short titles never match loosely`() {
        assertEquals(TitleMatcher.Quality.NONE, quality(listOf("Bastard"), "Bastards"))
    }

    @Test
    fun `a one letter difference in a long title is close, not exact`() {
        assertEquals(
            TitleMatcher.Quality.CLOSE,
            quality(listOf("Revenge of the Baskerville Bloodhound"), "Revenge of the Baskerville Bloodhounds"),
        )
    }

    @Test
    fun `an exact result wins over a close one`() {
        val results = listOf("Absolute Regressions", "Absolute Regression", "Absolute Regression (Novel) Side Story")
        val best = TitleMatcher.best(listOf("Absolute Regression"), results) { it }
        assertEquals("Absolute Regression", best?.first)
        assertEquals(TitleMatcher.Quality.EXACT, best?.second)
    }

    @Test
    fun `two close results and no exact one is a miss`() {
        val results = listOf("Revenge of the Baskerville Bloodhounds", "Revenge of the Baskerville Bloodhounde")
        assertNull(TitleMatcher.best(listOf("Revenge of the Baskerville Bloodhound"), results) { it })
    }

    @Test
    fun `nothing matches an empty title`() {
        assertEquals(TitleMatcher.Quality.NONE, quality(listOf("Something"), "  !!  "))
    }
}
