package com.example.nativegallery.data

import com.example.nativegallery.model.MediaItem
import com.example.nativegallery.model.MediaType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DocumentPhotoClassifierTest {
    @Test
    fun receiptTextIsClassifiedAsBillOrReceipt() {
        val result = classifyDocumentPhoto(
            text = "Store receipt Invoice 103 Subtotal 18.00 Tax 2.00 Total 20.00 Paid",
            lineCount = 7,
            blockCount = 2
        )

        assertEquals(DocumentPhotoCategory.BillsReceipts, result?.category)
    }

    @Test
    fun menuTextIsClassifiedAsMenu() {
        val result = classifyDocumentPhoto(
            text = "Dinner Menu Starters Soup 8 Main course Chicken 20 Dessert Cake 7 Beverages",
            lineCount = 8,
            blockCount = 2
        )

        assertEquals(DocumentPhotoCategory.Menus, result?.category)
    }

    @Test
    fun applicationAndLetterTextIsClassifiedTogether() {
        val result = classifyDocumentPhoto(
            text = "Application form Applicant name Address Telephone Date of birth Signature",
            lineCount = 7,
            blockCount = 1
        )

        assertEquals(DocumentPhotoCategory.FormsLetters, result?.category)
    }

    @Test
    fun transcriptAndNotesTextIsClassifiedTogether() {
        val result = classifyDocumentPhoto(
            text = "Academic transcript Course Semester Grade Class Assignment Notes",
            lineCount = 6,
            blockCount = 2
        )

        assertEquals(DocumentPhotoCategory.NotesTranscripts, result?.category)
    }

    @Test
    fun textHeavyPhotoWithoutKeywordsStillAppearsAsOther() {
        val result = classifyDocumentPhoto(
            text = "This page contains several readable sentences arranged across many lines of text for later reference. Each paragraph explains a separate topic with enough detail to resemble a photographed page. The information continues with dates, names, numbered points, and a closing summary for the reader.",
            lineCount = 9,
            blockCount = 3
        )

        assertEquals(DocumentPhotoCategory.Other, result?.category)
        assertTrue(result?.recognizedText?.isNotBlank() == true)
    }

    @Test
    fun shortSignOrCaptionIsNotTreatedAsDocument() {
        val result = classifyDocumentPhoto(
            text = "Parking entrance only",
            lineCount = 2,
            blockCount = 1
        )

        assertNull(result)
    }

    @Test
    fun socialMediaChromeIsNotTreatedAsDocument() {
        val result = classifyDocumentPhoto(
            text = "Most relevant Follow this account 5.8k likes Reply Write a comment Join the conversation",
            lineCount = 10,
            blockCount = 3
        )

        assertNull(result)
    }

    @Test
    fun aSingleSocialUiSignalRejectsOtherwiseTextHeavyScreenshot() {
        val result = classifyDocumentPhoto(
            text = "Follow A long caption explains a trip, lists several places, describes the weather, thanks friends, and continues with enough prose to look structurally like a page when OCR reads the screenshot.",
            lineCount = 9,
            blockCount = 3
        )

        assertNull(result)
    }

    @Test
    fun oneIncidentalCategoryWordDoesNotMakeAPhotoADocument() {
        val result = classifyDocumentPhoto(
            text = "Device manager settings display adapters network adapters address and system information",
            lineCount = 8,
            blockCount = 2
        )

        assertNull(result)
    }

    @Test
    fun severalLinesOfIncidentalSceneTextAreNotTreatedAsDocument() {
        val result = classifyDocumentPhoto(
            text = "Central station Platform four Next train 18:20 Please stand behind the line",
            lineCount = 6,
            blockCount = 4
        )

        assertNull(result)
    }

    @Test
    fun smallAndExtremeAspectImagesSkipOcrPrefilter() {
        assertFalse(isDocumentPhotoAnalysisCandidate(mediaItem(width = 240, height = 240)))
        assertFalse(isDocumentPhotoAnalysisCandidate(mediaItem(width = 4000, height = 600)))
        assertTrue(isDocumentPhotoAnalysisCandidate(mediaItem(width = 1080, height = 1440)))
    }

    @Test
    fun classifierUpgradeCanReuseUnchangedMediaFingerprint() {
        assertTrue(
            hasSameDocumentPhotoSource(
                storedFingerprint = "2|42|content://media/42|100|2048|1080|1440|image/jpeg",
                expectedFingerprint = "3|42|content://media/42|100|2048|1080|1440|image/jpeg"
            )
        )
        assertFalse(
            hasSameDocumentPhotoSource(
                storedFingerprint = "2|42|content://media/42|100|2048|1080|1440|image/jpeg",
                expectedFingerprint = "3|42|content://media/42|101|2048|1080|1440|image/jpeg"
            )
        )
    }

    private fun mediaItem(width: Int, height: Int) = MediaItem(
        id = "photo-$width-$height",
        albumId = "camera",
        type = MediaType.Photo,
        title = "Photo",
        dateLabel = "Today",
        width = width,
        height = height,
        mimeType = "image/jpeg",
        fileSizeBytes = 1_000_000L
    )
}
