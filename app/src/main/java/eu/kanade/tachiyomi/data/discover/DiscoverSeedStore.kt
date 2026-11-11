package eu.kanade.tachiyomi.data.discover

import java.io.File

/**
 * The library title the user picked for the "Because you like" row. Kept in its own small file, like the genres,
 * because the refresh worker rewrites the picks while the user may be changing it.
 */
class DiscoverSeedStore(private val file: File) {

    fun load(): String? {
        return try {
            if (file.exists()) file.readText().trim().takeIf { it.isNotEmpty() } else null
        } catch (e: Exception) {
            null
        }
    }

    /** Saves [title], or forgets the choice when it is null or blank. Returns what is now stored. */
    @Synchronized
    fun save(title: String?): String? {
        val clean = title?.trim()?.takeIf { it.isNotEmpty() }
        if (clean == null) {
            file.delete()
        } else {
            file.writeText(clean)
        }
        return clean
    }
}
