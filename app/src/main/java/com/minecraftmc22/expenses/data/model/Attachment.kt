package com.minecraftmc22.expenses.data.model

enum class AttachmentKind {
    IMAGE, VIDEO, OTHER
}

/**
 * A file the user attached to an expense: a picture, a video or anything else.
 *
 * [path] points at a copy inside the app's private storage, never at the document the user
 * picked, so the attachment keeps working without any persistable URI permission.
 */
data class Attachment(
    val id: String,
    val path: String,
    val name: String,
    val mimeType: String
) {

    val kind: AttachmentKind
        get() = when {
            mimeType.startsWith("image/") -> AttachmentKind.IMAGE
            mimeType.startsWith("video/") -> AttachmentKind.VIDEO
            else -> AttachmentKind.OTHER
        }

    val isImage: Boolean get() = kind == AttachmentKind.IMAGE
}
