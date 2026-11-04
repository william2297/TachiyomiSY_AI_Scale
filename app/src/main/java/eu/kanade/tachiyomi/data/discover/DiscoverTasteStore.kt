package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.json.Json
import java.io.File

/**
 * Tags of the titles the user read and of the titles the user rejected. Kept in its own small file, away from the
 * picks, because the refresh worker and the tab both write to it.
 */
class DiscoverTasteStore(private val file: File, private val json: Json) {

    fun load(): TasteData {
        return try {
            if (file.exists()) json.decodeFromString(TasteData.serializer(), file.readText()) else TasteData()
        } catch (e: Exception) {
            TasteData()
        }
    }

    @Synchronized
    fun putTitle(normalizedTitle: String, tags: List<TitleTag>) {
        val data = load()
        // Newest last; the oldest go first when the file grows past what the profile can use
        val titles = LinkedHashMap(data.titles).apply {
            remove(normalizedTitle)
            put(normalizedTitle, tags)
        }
        while (titles.size > MAX_TITLES) titles.remove(titles.keys.first())
        save(data.copy(titles = titles))
    }

    @Synchronized
    fun addNegative(tags: List<String>, nowMillis: Long) {
        if (tags.isEmpty()) return
        val data = load()
        val kept = data.negatives.filter { nowMillis - it.atMillis < MAX_NEGATIVE_AGE_MILLIS }
        save(data.copy(negatives = (kept + tags.map { NegativeTag(it, nowMillis) }).takeLast(MAX_NEGATIVES)))
    }

    private fun save(data: TasteData) {
        file.writeText(json.encodeToString(TasteData.serializer(), data))
    }

    private companion object {
        const val MAX_TITLES = 80
        const val MAX_NEGATIVES = 200
        const val MAX_NEGATIVE_AGE_MILLIS = 60L * 86_400_000L
    }
}
