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
    if (meaningfulCharacterCount < 24 || lineCount < 2) return null

    val keywordText = normalized
        .replace(nonAlphaNumericRegex, " ")
        .replace(whitespaceRegex, " ")
        .trim()
    val paddedKeywordText = " $keywordText "
    val wordCount = keywordText.split(' ').count { word -> word.length >= 2 }
    fun containsKeyword(keyword: String): Boolean =
        paddedKeywordText.contains(" $keyword ")

    val scores = categoryKeywords.mapValues { (_, keywords) ->
        keywords.count(::containsKeyword)
    }
    val strongest = scores.maxByOrNull { it.value }
    val strongestScore = strongest?.value ?: 0
    val keywordHitCount = scores.values.sum()
    val hasStrongDocumentPhrase =
        containsKeyword("receipt") ||
            containsKeyword("invoice") ||
            containsKeyword("application form") ||
            containsKeyword("academic transcript") ||
            containsKeyword("meeting minutes") ||
            containsKeyword("lecture notes") ||
            containsKeyword("dear sir") ||
            containsKeyword("dear madam")
    val socialUiSignalCount = setOf(
        "follow", "followers", "reply", "write a comment", "reels", "likes", "send a chat"
    ).count(::containsKeyword)
    if (socialUiSignalCount >= 1 && keywordHitCount < 3 && !hasStrongDocumentPhrase) return null
    val hasDocumentStructure =
        (
            meaningfulCharacterCount >= 140 &&
                wordCount >= 22 &&
                lineCount >= 6 &&
                blockCount >= 2
            ) ||
            (meaningfulCharacterCount >= 220 && wordCount >= 32 && lineCount >= 5)
    val hasDocumentVocabulary =
        (strongestScore >= 2 && meaningfulCharacterCount >= 36 && lineCount >= 3) ||
            (keywordHitCount >= 3 && meaningfulCharacterCount >= 32 && lineCount >= 3) ||
            (hasStrongDocumentPhrase && meaningfulCharacterCount >= 32 && lineCount >= 3)

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
