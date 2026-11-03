package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json
import java.io.File

/**
 * AniList ids the user marked "Not interested". Kept in its own small file, away from the picks, because the daily
 * worker rewrites the picks while the user may be hiding a title at that moment.
 */
class DiscoverHiddenStore(private val file: File, private val json: Json) {

    fun load(): Set<Long> {
        return try {
            if (file.exists()) {
                json.decodeFromString(
                    ListSerializer(Long.serializer()),
                    file.readText(),
                ).toSet()
            } else {
                emptySet()
            }
        } catch (e: Exception) {
            emptySet()
        }
    }

    @Synchronized
    fun add(id: Long): Set<Long> {
        val updated = load() + id
        file.writeText(json.encodeToString(ListSerializer(Long.serializer()), updated.toList()))
        return updated
    }
}
