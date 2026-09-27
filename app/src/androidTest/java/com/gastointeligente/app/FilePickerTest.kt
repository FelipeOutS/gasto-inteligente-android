package com.gastointeligente.app

import android.content.ClipData
import android.content.Intent
import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class FilePickerTest {
    private val selection = FileSelection.from(listOf("image/*", "application/pdf"), false)
    private val privateAuthority = "com.gastointeligente.app.fileprovider"

    @Test fun pickerIncludesPdfAndImagesWithoutAllowingMultipleSelection() {
        val intent = selection.createPickerIntent()
        assertEquals(Intent.ACTION_GET_CONTENT, intent.action)
        assertTrue(intent.hasCategory(Intent.CATEGORY_OPENABLE))
        assertEquals("*/*", intent.type)
        assertArrayEquals(arrayOf("image/*", "application/pdf"), intent.getStringArrayExtra(Intent.EXTRA_MIME_TYPES))
        assertFalse(intent.getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, true))
    }

    @Test fun multipleModeIsPassedToAndroidPicker() {
        assertTrue(FileSelection.from(listOf("text/csv"), true).createPickerIntent()
            .getBooleanExtra(Intent.EXTRA_ALLOW_MULTIPLE, false))
    }

    @Test fun duplicateDataAndClipDataDoNotDuplicateAnUpload() {
        val first = Uri.parse("content://documents/first.pdf")
        val second = Uri.parse("content://documents/second.pdf")
        val intent = Intent().setData(first).apply {
            clipData = ClipData.newRawUri("file", first).apply { addItem(ClipData.Item(second)) }
        }
        assertArrayEquals(arrayOf(first), selection.readPickerResult(intent, privateAuthority))
        assertArrayEquals(arrayOf(first, second), selection.copy(allowMultiple = true)
            .readPickerResult(intent, privateAuthority))
    }

    @Test fun canceledOrEmptyPickerDoesNotReturnAStaleCameraFile() {
        assertNull(selection.readPickerResult(null, privateAuthority))
        assertNull(selection.readPickerResult(Intent(), privateAuthority))
    }

    @Test fun pickerCannotReturnPrivateAppFilesOrArbitraryUrls() {
        for (url in listOf("content://$privateAuthority/internal_files/private", "file:///data/private", "https://example.com/file")) {
            assertNull(selection.readPickerResult(Intent().setData(Uri.parse(url)), privateAuthority))
        }
    }
}
