package com.group14.notes.data

/** A note as the UI sees it: already decrypted. */
data class Note(
    val id: Long,
    val title: String,
    val content: String,
    val createdAt: Long,
    val updatedAt: Long
) {
    /** One-line subtitle for the list: whitespace collapsed, truncated. */
    val preview: String
        get() {
            val flat = content.replace(Regex("\\s+"), " ").trim()
            return if (flat.length > PREVIEW_LENGTH) flat.take(PREVIEW_LENGTH).trimEnd() + "…" else flat
        }

    companion object {
        const val PREVIEW_LENGTH = 90
    }
}
