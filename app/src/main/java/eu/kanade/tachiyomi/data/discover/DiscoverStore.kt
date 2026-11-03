package eu.kanade.tachiyomi.data.discover

import kotlinx.serialization.json.Json
import java.io.File

/** The Discover picks and the remembered matches, as one small JSON file. A bad or missing file reads as empty. */
class DiscoverStore(private val file: File, private val json: Json) {

    fun load(): DiscoverCache {
        return try {
            if (file.exists()) json.decodeFromString(DiscoverCache.serializer(), file.readText()) else DiscoverCache()
        } catch (e: Exception) {
            DiscoverCache()
        }
    }

    fun save(cache: DiscoverCache) {
        // Written beside the file and renamed, so a stopped app never leaves half a file
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(json.encodeToString(DiscoverCache.serializer(), cache))
        if (!temp.renameTo(file)) {
            file.delete()
            temp.renameTo(file)
        }
    }
}
