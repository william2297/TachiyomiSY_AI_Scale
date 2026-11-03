package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * The genres the user chose for the "My genres" row. Kept in its own small file, away from the picks, because the
 * refresh worker rewrites the picks while the user may be changing the genres.
 */
class DiscoverGenreStore(private val file: File, private val json: Json) {

    fun load(): Set<String> {
        return try {
            if (file.exists()) {
                json.decodeFromString(ListSerializer(String.serializer()), file.readText())
                    .filter { it in AniListDiscoverApi.PICKABLE_GENRES }
                    .toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    @Synchronized
    fun save(genres: Set<String>): Set<String> {
        val clean = genres.filter { it in AniListDiscoverApi.PICKABLE_GENRES }.toSet()
        file.writeText(json.encodeToString(ListSerializer(String.serializer()), clean.sorted()))
        return clean
    }
}
