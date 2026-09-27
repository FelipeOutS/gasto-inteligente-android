package com.gastointeligente.app

import org.junit.Assert.*
import org.junit.Test

class FileSelectionTest {
    @Test fun pdfAndImageRemainAvailableTogether() {
        val selection = FileSelection.from(listOf("image/*", "application/pdf"), false)
        assertEquals("*/*", selection.pickerType)
        assertEquals(listOf("image/*", "application/pdf"), selection.mimeTypes)
        assertTrue(selection.allowsCamera)
    }

    @Test fun pdfOnlyDoesNotOfferCamera() {
        val selection = FileSelection.from(listOf("application/pdf"), false)
        assertEquals("application/pdf", selection.pickerType)
        assertFalse(selection.allowsCamera)
    }

    @Test fun unrestrictedInputDoesNotBecomeImageOnly() {
        val selection = FileSelection.from(emptyList(), false)
        assertEquals(listOf("*/*"), selection.mimeTypes)
        assertTrue(selection.allowsCamera)
    }

    @Test fun commaSeparatedTypesAndCaseAreNormalized() {
        assertEquals(listOf("image/*", "application/pdf"),
            FileSelection.from(listOf(" IMAGE/*, application/pdf ", "image/*"), false).mimeTypes)
    }

    @Test fun extensionIsResolvedBeforeFiltering() {
        val selection = FileSelection.from(listOf(".PDF", ".csv"), false) {
            mapOf("pdf" to "application/pdf", "csv" to "text/csv")[it]
        }
        assertEquals(listOf("application/pdf", "text/csv"), selection.mimeTypes)
    }

    @Test fun unknownExtensionDoesNotHidePotentiallyValidFiles() {
        assertEquals(listOf("*/*"), FileSelection.from(listOf("image/*", ".unknown"), false).mimeTypes)
    }

    @Test fun pngOnlyDoesNotOfferJpegCapture() {
        assertFalse(FileSelection.from(listOf("image/png"), false).allowsCamera)
    }

    @Test fun singleSelectionRejectsExtraItems() {
        assertEquals(listOf("first.pdf"),
            FileSelection.from(emptyList(), false).selected(listOf("first.pdf", "second.pdf")))
    }

    @Test fun multipleSelectionRemovesDuplicateUrisInOriginalOrder() {
        assertEquals(listOf("first.pdf", "second.pdf"),
            FileSelection.from(emptyList(), true).selected(listOf("first.pdf", "first.pdf", "second.pdf")))
    }
}
