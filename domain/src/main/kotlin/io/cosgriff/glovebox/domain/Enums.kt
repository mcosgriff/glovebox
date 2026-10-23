package io.cosgriff.glovebox.domain

/**
 * The entry of [T] named [name] exactly, or null: for a name read back from storage or saved state, where one this
 * version doesn't know (or none) falls back to whatever the caller says.
 */
inline fun <reified T : Enum<T>> enumOrNull(name: String?): T? = name?.let { n -> enumValues<T>().firstOrNull { it.name == n } }
