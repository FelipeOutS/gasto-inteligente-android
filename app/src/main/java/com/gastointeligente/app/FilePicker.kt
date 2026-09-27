package com.gastointeligente.app

import android.content.Intent
import android.net.Uri

internal fun FileSelection.createPickerIntent(): Intent =
    Intent(Intent.ACTION_GET_CONTENT).apply {
        addCategory(Intent.CATEGORY_OPENABLE)
        type = pickerType
        if (mimeTypes.size > 1) putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes.toTypedArray())
        putExtra(Intent.EXTRA_ALLOW_MULTIPLE, allowMultiple)
    }

internal fun FileSelection.readPickerResult(data: Intent?, privateAuthority: String): Array<Uri>? {
    val items = buildList {
        data?.data?.let { add(it) }
        data?.clipData?.let { clip ->
            for (index in 0 until clip.itemCount) clip.getItemAt(index).uri?.let { add(it) }
        }
    }.filter { it.scheme == "content" && it.authority != privateAuthority }
    return selected(items).takeIf { it.isNotEmpty() }?.toTypedArray()
}
