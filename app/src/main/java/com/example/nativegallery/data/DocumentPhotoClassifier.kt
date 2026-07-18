package com.example.nativegallery.data

import androidx.compose.runtime.Immutable

enum class DocumentPhotoCategory(val label: String) {
    BillsReceipts("Bills & receipts"),
    Menus("Menus"),
    FormsLetters("Forms & letters"),
    NotesTranscripts("Notes & transcripts"),
    Other("Other")
}

@Immutable
data class DocumentPhotoClassification(
    val category: DocumentPhotoCategory,
    val recognizedText: String,
    val lineCount: Int
)

private val whitespaceRegex = Regex("""\s+""")
private val nonAlphaNumericRegex = Regex("""[^\p{L}\p{N}]+""")

private val categoryKeywords = mapOf(
    DocumentPhotoCategory.BillsReceipts to setOf(
        "receipt", "invoice", "subtotal", "total", "tax", "amount", "balance",
        "payment", "paid", "quantity", "price", "account", "bill", "due"
    ),
    DocumentPhotoCategory.Menus to setOf(
        "menu", "appetizer", "starter", "beverage", "dessert", "breakfast",
        "lunch", "dinner", "special", "served", "restaurant"
    ),
    DocumentPhotoCategory.FormsLetters to setOf(
        "application", "applicant", "form", "signature", "address", "date of birth",
        "telephone", "dear sir", "dear madam", "sincerely", "subject", "letter"
    ),
    DocumentPhotoCategory.NotesTranscripts to setOf(
        "transcript", "notes", "lecture", "meeting", "minutes", "course",
        "semester", "grade", "chapter", "assignment", "class"
    )
)

fun classifyDocumentPhoto(
    text: String,
    lineCount: Int,
    blockCount: Int
): DocumentPhotoClassification? {
    val normalized = text
        .lowercase()
        .replace(whitespaceRegex, " ")
        .trim()
    val meaningfulCharacterCount = normalized.count { it.isLetterOrDigit() }
    if (meaningfulCharacterCount < 20 || lineCount < 2) return null

    val keywordText = normalized
        .replace(nonAlphaNumericRegex, " ")
        .replace(whitespaceRegex, " ")
        .trim()
    val paddedKeywordText = " $keywordText "
    fun containsKeyword(keyword: String): Boolean =
        paddedKeywordText.contains(" $keyword ")

    val scores = categoryKeywords.mapValues { (_, keywords) ->
        keywords.count(::containsKeyword)
    }
    val strongest = scores.maxByOrNull { it.value }
    val strongestScore = strongest?.value ?: 0
    val keywordHitCount = scores.values.sum()
    val socialUiSignalCount = setOf(
        "follow", "followers", "reply", "write a comment", "reels", "likes", "send a chat"
    ).count(::containsKeyword)
    if (socialUiSignalCount >= 2 && keywordHitCount < 2) return null

    val hasDocumentStructure =
        (meaningfulCharacterCount >= 45 && lineCount >= 4) ||
            (meaningfulCharacterCount >= 80 && lineCount >= 3 && blockCount >= 1)
    val hasDocumentVocabulary =
        (keywordHitCount >= 2 && meaningfulCharacterCount >= 28) ||
            (strongestScore >= 1 && meaningfulCharacterCount >= 55 && lineCount >= 3)

    if (!hasDocumentStructure && !hasDocumentVocabulary) return null

    val strongPhraseCategory = when {
        containsKeyword("receipt") ||
            containsKeyword("invoice") ||
            containsKeyword("subtotal") -> DocumentPhotoCategory.BillsReceipts
        containsKeyword("application form") ||
            containsKeyword("dear sir") ||
            containsKeyword("dear madam") -> DocumentPhotoCategory.FormsLetters
        containsKeyword("academic transcript") ||
            containsKeyword("meeting minutes") ||
            containsKeyword("lecture notes") -> DocumentPhotoCategory.NotesTranscripts
        else -> null
    }
    val category = when {
        strongestScore >= 2 -> strongest!!.key
        strongPhraseCategory != null -> strongPhraseCategory
        else -> DocumentPhotoCategory.Other
    }
    return DocumentPhotoClassification(
        category = category,
        recognizedText = normalized.take(4_000),
        lineCount = lineCount
    )
}
