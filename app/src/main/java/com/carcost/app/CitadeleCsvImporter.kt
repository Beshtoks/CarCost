package com.carcost.app

import java.nio.charset.Charset
import java.util.Locale

object CitadeleCsvImporter {

    fun parse(bytes: ByteArray): List<BankImportPreviewItem> {
        val text = decodeCitadeleCsv(bytes)
        val lines = text.lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }

        val headerIndex = lines.indexOfFirst { line ->
            line.contains("\"Date\"|\"Type\"|\"Narrative\"")
        }

        if (headerIndex == -1) return emptyList()

        val result = mutableListOf<BankImportPreviewItem>()

        for (i in headerIndex + 1 until lines.size) {
            val row = parsePipeCsvLine(lines[i])
            if (row.isEmpty()) continue

            val normalizedRow = normalizeRow(row)
            if (normalizedRow == null) continue

            val date = normalizedRow[0].trim()
            val type = normalizedRow[1].trim()
            val narrative = normalizedRow[2].trim()
            val amountDr = normalizeMoney(normalizedRow[5])
            val amountCr = normalizeMoney(normalizedRow[6])

            if (date.isBlank()) continue
            if (isSummaryRow(date, type, narrative, amountDr, amountCr)) continue
            if (narrative.isBlank() && amountDr.isBlank() && amountCr.isBlank()) continue

            result.add(classifyRow(date, type, narrative, amountDr, amountCr))
        }

        return result
    }

    private fun normalizeRow(row: List<String>): List<String>? {
        val cleaned = row.map { it.trim().trim('"') }

        return when {
            cleaned.size >= 7 -> cleaned.take(7)
            cleaned.size == 6 -> cleaned + listOf("")
            else -> null
        }
    }

    private fun isSummaryRow(
        date: String,
        bankType: String,
        narrative: String,
        amountDr: String,
        amountCr: String
    ): Boolean {
        val upperType = bankType.uppercase(Locale.ROOT)
        val upperNarrative = narrative.uppercase(Locale.ROOT)

        if (amountDr.isBlank() && amountCr.isBlank()) return true

        if (upperType.contains("OPENING")) return true
        if (upperType.contains("CLOSING")) return true
        if (upperType.contains("BALANCE")) return true

        if (upperNarrative.contains("OPENING BALANCE")) return true
        if (upperNarrative.contains("CLOSING BALANCE")) return true
        if (upperNarrative.contains("AVAILABLE BALANCE")) return true
        if (upperNarrative.contains("TOTAL")) return true

        return false
    }

    private fun classifyRow(
        date: String,
        bankType: String,
        narrative: String,
        amountDr: String,
        amountCr: String
    ): BankImportPreviewItem {
        val upper = narrative.uppercase(Locale.ROOT)
        val amount = when {
            amountCr.isNotBlank() -> amountCr
            amountDr.isNotBlank() -> amountDr
            else -> ""
        }

        if (amountCr.isNotBlank()) {
            if (upper.contains("BOLT")) {
                val draft = IncomeDraft(
                    type = "Агрегаторы",
                    subtype = "Bolt",
                    title = "Bolt",
                    date = date,
                    amount = amountCr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountCr,
                    kind = BankImportKind.INCOME,
                    incomeDraft = draft
                )
            }

            if (upper.contains("SUMUP")) {
                val draft = IncomeDraft(
                    type = "Терминал",
                    subtype = null,
                    title = "SumUp",
                    date = date,
                    amount = amountCr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountCr,
                    kind = BankImportKind.INCOME,
                    incomeDraft = draft
                )
            }

            val draft = IncomeDraft(
                type = "Прочее",
                subtype = null,
                title = shortTitleFromNarrative(narrative, fallback = "Импорт из банка"),
                date = date,
                amount = amountCr,
                comment = narrative
            )

            return BankImportPreviewItem(
                bankDate = date,
                bankType = bankType,
                narrative = narrative,
                amountDr = amountDr,
                amountCr = amountCr,
                amount = amountCr,
                kind = BankImportKind.INCOME,
                incomeDraft = draft
            )
        }

        if (amountDr.isNotBlank()) {
            if (isWashPayment(upper)) {
                val draft = TechniqueExpenseDraft(
                    recordType = "Покупка",
                    titles = listOf("Мойка"),
                    date = date,
                    mileage = "",
                    quantity = "1",
                    quantityUnit = "шт",
                    amount = amountDr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountDr,
                    kind = BankImportKind.TECHNIQUE_EXPENSE,
                    techniqueDraft = draft
                )
            }

            if (upper.contains("NESTE")) {
                val draft = TechniqueExpenseDraft(
                    recordType = "Покупка",
                    titles = listOf("Топливо"),
                    date = date,
                    mileage = "",
                    quantity = "",
                    quantityUnit = "л",
                    amount = amountDr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountDr,
                    kind = BankImportKind.TECHNIQUE_EXPENSE,
                    techniqueDraft = draft
                )
            }

            if (upper.contains("MOBILLY")) {
                val draft = DocumentationExpenseDraft(
                    type = "Парковки",
                    subtype = "Mobilly",
                    title = "Mobilly",
                    date = date,
                    odometer = "",
                    validUntil = null,
                    amount = amountDr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountDr,
                    kind = BankImportKind.DOCUMENTATION_EXPENSE,
                    documentationDraft = draft
                )
            }

            if (upper.contains("JURMALA")) {
                val draft = DocumentationExpenseDraft(
                    type = "Пропуски",
                    subtype = "Юрмала",
                    title = "Юрмала",
                    date = date,
                    odometer = "",
                    validUntil = null,
                    amount = amountDr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountDr,
                    kind = BankImportKind.DOCUMENTATION_EXPENSE,
                    documentationDraft = draft
                )
            }

            if (
                upper.contains("SMS") ||
                upper.contains("COMMISSION") ||
                upper.contains("KOMIS") ||
                upper.contains("BUNDLE")
            ) {
                val draft = DocumentationExpenseDraft(
                    type = "Банковские расходы",
                    subtype = "Комиссии",
                    title = "Комиссия банка",
                    date = date,
                    odometer = "",
                    validUntil = null,
                    amount = amountDr,
                    comment = narrative
                )

                return BankImportPreviewItem(
                    bankDate = date,
                    bankType = bankType,
                    narrative = narrative,
                    amountDr = amountDr,
                    amountCr = amountCr,
                    amount = amountDr,
                    kind = BankImportKind.DOCUMENTATION_EXPENSE,
                    documentationDraft = draft
                )
            }

            val draft = DocumentationExpenseDraft(
                type = "Прочее",
                subtype = null,
                title = shortTitleFromNarrative(narrative, fallback = "Импорт из банка"),
                date = date,
                odometer = "",
                validUntil = null,
                amount = amountDr,
                comment = narrative
            )

            return BankImportPreviewItem(
                bankDate = date,
                bankType = bankType,
                narrative = narrative,
                amountDr = amountDr,
                amountCr = amountCr,
                amount = amountDr,
                kind = BankImportKind.DOCUMENTATION_EXPENSE,
                documentationDraft = draft
            )
        }

        return BankImportPreviewItem(
            bankDate = date,
            bankType = bankType,
            narrative = narrative,
            amountDr = amountDr,
            amountCr = amountCr,
            amount = amount,
            kind = BankImportKind.UNKNOWN
        )
    }

    private fun isWashPayment(upperNarrative: String): Boolean {
        return upperNarrative.contains("WASH") ||
                upperNarrative.contains("WASH AND DRIVE") ||
                upperNarrative.contains("CAR WASH") ||
                upperNarrative.contains("MAZGA") ||
                upperNarrative.contains("MAZGATAVA") ||
                upperNarrative.contains("MAZGA PATS") ||
                upperNarrative.contains("AUTO MAZ") ||
                upperNarrative.contains("AUTOMAZ") ||
                upperNarrative.contains("PUTOAS") ||
                upperNarrative.contains("LIELIRBES AUTO MAZ") ||
                upperNarrative.contains(" MAZ ") ||
                upperNarrative.startsWith("MAZ ") ||
                upperNarrative.endsWith(" MAZ") ||
                upperNarrative.contains(" MAZ,") ||
                upperNarrative.contains(" MAZ.") ||
                upperNarrative.contains(" MAZ-")
    }

    private fun shortTitleFromNarrative(narrative: String, fallback: String): String {
        val cleaned = narrative
            .replace("\n", " ")
            .replace("\r", " ")
            .trim()

        if (cleaned.isBlank()) return fallback

        val compact = cleaned.split(" ")
            .filter { it.isNotBlank() }
            .take(4)
            .joinToString(" ")

        return if (compact.isBlank()) fallback else compact
    }

    private fun normalizeMoney(value: String): String {
        return value
            .replace("\"", "")
            .replace(" ", "")
            .replace(",", ".")
            .trim()
    }

    private fun decodeCitadeleCsv(bytes: ByteArray): String {
        val candidates = listOf(
            Charset.forName("UTF-8"),
            Charset.forName("windows-1257"),
            Charset.forName("ISO-8859-1")
        )

        for (charset in candidates) {
            try {
                val text = bytes.toString(charset)
                if (text.contains("\"Date\"|\"Type\"|\"Narrative\"")) {
                    return text
                }
            } catch (_: Exception) {
            }
        }

        return bytes.toString(Charset.forName("windows-1257"))
    }

    private fun parsePipeCsvLine(line: String): List<String> {
        val result = mutableListOf<String>()
        val current = StringBuilder()

        var inQuotes = false
        var i = 0

        while (i < line.length) {
            val ch = line[i]

            when {
                ch == '"' -> {
                    if (inQuotes && i + 1 < line.length && line[i + 1] == '"') {
                        current.append('"')
                        i++
                    } else {
                        inQuotes = !inQuotes
                    }
                }

                ch == '|' && !inQuotes -> {
                    result.add(current.toString())
                    current.clear()
                }

                else -> {
                    current.append(ch)
                }
            }

            i++
        }

        result.add(current.toString())
        return result.map { it.trim().trim('"') }
    }
}