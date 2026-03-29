package com.carcost.app

enum class BankImportKind {
    INCOME,
    DOCUMENTATION_EXPENSE,
    TECHNIQUE_EXPENSE,
    UNKNOWN
}

data class BankImportPreviewItem(
    val bankDate: String,
    val bankType: String,
    val narrative: String,
    val amountDr: String,
    val amountCr: String,
    val amount: String,
    val kind: BankImportKind,
    val incomeDraft: IncomeDraft? = null,
    val documentationDraft: DocumentationExpenseDraft? = null,
    val techniqueDraft: TechniqueExpenseDraft? = null
)