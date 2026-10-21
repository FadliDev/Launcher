package com.sally.ide.data

import android.net.Uri
import java.util.UUID

data class EditorDocument(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val content: String,
    val savedContent: String = content,
    val uri: Uri? = null,
    val localPath: String? = null
) {
    val isDirty: Boolean get() = content != savedContent
}
