package com.example.nativegallery.data

import org.junit.Assert.assertEquals
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
            text = "This page contains several readable sentences arranged across many lines of text for later reference",
            lineCount = 6,
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
    fun oneIncidentalCategoryWordDoesNotMislabelGeneralText() {
        val result = classifyDocumentPhoto(
            text = "Device manager settings display adapters network adapters address and system information",
            lineCount = 8,
            blockCount = 2
        )

        assertEquals(DocumentPhotoCategory.Other, result?.category)
    }
}