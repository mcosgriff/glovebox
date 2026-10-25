package io.cosgriff.glovebox.ui.components

import android.content.Context
import java.io.IOException

/**
 * Every file in [dir] of the app's assets that [parse] reads, in name order. What a file may hold is checked in
 * `:domain`, where a test reads these same files; here one that doesn't parse is simply left out.
 */
internal fun <T> Context.readBundled(dir: String, parse: (String) -> T?): List<T> = try {
    assets.list(dir).orEmpty().sorted().mapNotNull { name ->
        parse(assets.open("$dir/$name").bufferedReader().use { it.readText() })
    }
} catch (_: IOException) {
    emptyList()
}
