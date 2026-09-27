package com.gastointeligente.app

import java.util.Locale

/** The website's accept list is a picker filter, not a substitute for server validation. */
internal data class FileSelection(
    val mimeTypes: List<String>,
    val allowMultiple: Boolean,
) {
    val pickerType: String
        get() = mimeTypes.singleOrNull() ?: "*/*"

    // ACTION_IMAGE_CAPTURE creates JPEGs; do not offer it for a PNG-only input.
    val allowsCamera: Boolean
        get() = mimeTypes.any { it == "*/*" || it == "image/*" || it == "image/jpeg" }

    fun <T> selected(items: List<T>): List<T> =
        items.distinct().let { if (allowMultiple) it else it.take(1) }

    companion object {
        fun from(
            acceptTypes: List<String>,
            allowMultiple: Boolean,
            mimeForExtension: (String) -> String? = { null },
        ): FileSelection {
            val types = acceptTypes.flatMap { it.split(',') }
                .map { it.trim().lowercase(Locale.ROOT) }
                .filter { it.isNotEmpty() }
                .map { type ->
                    if (type.startsWith('.')) mimeForExtension(type.drop(1)) ?: "*/*"
                    else type
                }
                .filter { it.matches(Regex("[a-z0-9!#$&^_.+*-]+/[a-z0-9!#$&^_.+*-]+")) }
                .distinct()
            val normalized = if (types.isEmpty() || "*/*" in types) listOf("*/*") else types
            return FileSelection(normalized, allowMultiple)
        }
    }
}
