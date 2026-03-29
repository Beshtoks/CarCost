package com.carcost.app

import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity

class BankImportActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var tvBankImportTitle: TextView
    private lateinit var tvBankImportStatus: TextView
    private lateinit var btnChooseBankFile: Button
    private lateinit var btnImportBankItems: Button
    private lateinit var btnCloseBankImport: Button
    private lateinit var lvBankImportPreview: ListView

    private val previewItems = mutableListOf<BankImportPreviewItem>()
    private lateinit var adapter: BankImportPreviewAdapter

    private val filePickerLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                readAndPreviewFile(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_bank_import)

        storage = AppStorage(this)

        tvBankImportTitle = findViewById(R.id.tvBankImportTitle)
        tvBankImportStatus = findViewById(R.id.tvBankImportStatus)
        btnChooseBankFile = findViewById(R.id.btnChooseBankFile)
        btnImportBankItems = findViewById(R.id.btnImportBankItems)
        btnCloseBankImport = findViewById(R.id.btnCloseBankImport)
        lvBankImportPreview = findViewById(R.id.lvBankImportPreview)

        adapter = BankImportPreviewAdapter(this, previewItems)
        lvBankImportPreview.adapter = adapter

        tvBankImportTitle.text = "Импорт из банка"
        tvBankImportStatus.text = "Файл не выбран"
        btnImportBankItems.isEnabled = false

        btnChooseBankFile.setOnClickListener {
            filePickerLauncher.launch(arrayOf("text/*", "application/octet-stream", "*/*"))
        }

        btnImportBankItems.setOnClickListener {
            confirmImportOnlyNewItems()
        }

        btnCloseBankImport.setOnClickListener {
            finish()
        }
    }

    private fun readAndPreviewFile(uri: Uri) {
        try {
            val bytes = contentResolver.openInputStream(uri)?.use { it.readBytes() }
                ?: throw IllegalStateException("Не удалось открыть файл")

            val parsed = CitadeleCsvImporter.parse(bytes)

            previewItems.clear()
            previewItems.addAll(parsed)
            adapter.notifyDataSetChanged()

            val duplicateSummary = calculateDuplicateSummary(parsed)

            tvBankImportStatus.text =
                "Найдено: ${parsed.size} · Новых: ${duplicateSummary.newCount} · Уже есть: ${duplicateSummary.duplicateCount}"

            btnImportBankItems.isEnabled = duplicateSummary.newCount > 0

            if (parsed.isEmpty()) {
                Toast.makeText(this, "Подходящих строк не найдено", Toast.LENGTH_SHORT).show()
            }
        } catch (_: Exception) {
            previewItems.clear()
            adapter.notifyDataSetChanged()
            tvBankImportStatus.text = "Не удалось прочитать файл"
            btnImportBankItems.isEnabled = false
            Toast.makeText(this, "Ошибка чтения файла", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmImportOnlyNewItems() {
        val summary = calculateDuplicateSummary(previewItems)

        if (summary.newCount <= 0) {
            Toast.makeText(this, "Новых операций для импорта нет", Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle("Импорт из банка")
            .setMessage(
                "Найдено ${previewItems.size} операций.\n" +
                        "Уже есть: ${summary.duplicateCount}.\n" +
                        "Будут импортированы только новые: ${summary.newCount}."
            )
            .setPositiveButton("Импортировать") { _, _ ->
                importOnlyNewItems()
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importOnlyNewItems() {
        val incomes = storage.loadIncomeDrafts()
        val documentation = storage.loadDocumentationDrafts()
        val technique = storage.loadTechniqueDrafts()

        var addedIncome = 0
        var addedDocumentation = 0
        var addedTechnique = 0
        var skippedDuplicates = 0

        previewItems.forEach { item ->
            when {
                item.incomeDraft != null -> {
                    if (containsIncome(incomes, item.incomeDraft)) {
                        skippedDuplicates++
                    } else {
                        incomes.add(item.incomeDraft)
                        addedIncome++
                    }
                }

                item.documentationDraft != null -> {
                    if (containsDocumentation(documentation, item.documentationDraft)) {
                        skippedDuplicates++
                    } else {
                        documentation.add(item.documentationDraft)
                        addedDocumentation++
                    }
                }

                item.techniqueDraft != null -> {
                    if (containsTechnique(technique, item.techniqueDraft)) {
                        skippedDuplicates++
                    } else {
                        technique.add(item.techniqueDraft)
                        addedTechnique++
                    }
                }
            }
        }

        storage.saveIncomeDrafts(incomes)
        storage.saveDocumentationDrafts(documentation)
        storage.saveTechniqueDrafts(technique)

        Toast.makeText(
            this,
            "Импортировано новых: ${addedIncome + addedDocumentation + addedTechnique} · пропущено дублей: $skippedDuplicates",
            Toast.LENGTH_LONG
        ).show()

        finish()
    }

    private fun calculateDuplicateSummary(items: List<BankImportPreviewItem>): DuplicateSummary {
        val incomes = storage.loadIncomeDrafts()
        val documentation = storage.loadDocumentationDrafts()
        val technique = storage.loadTechniqueDrafts()

        var duplicateCount = 0
        var newCount = 0

        items.forEach { item ->
            val isDuplicate = when {
                item.incomeDraft != null -> containsIncome(incomes, item.incomeDraft)
                item.documentationDraft != null -> containsDocumentation(documentation, item.documentationDraft)
                item.techniqueDraft != null -> containsTechnique(technique, item.techniqueDraft)
                else -> true
            }

            if (isDuplicate) {
                duplicateCount++
            } else {
                newCount++
            }
        }

        return DuplicateSummary(
            duplicateCount = duplicateCount,
            newCount = newCount
        )
    }

    private fun containsIncome(existing: List<IncomeDraft>, candidate: IncomeDraft): Boolean {
        return existing.any {
            it.date == candidate.date &&
                    normalizeAmount(it.amount) == normalizeAmount(candidate.amount) &&
                    normalizeText(it.comment) == normalizeText(candidate.comment)
        }
    }

    private fun containsDocumentation(
        existing: List<DocumentationExpenseDraft>,
        candidate: DocumentationExpenseDraft
    ): Boolean {
        return existing.any {
            it.date == candidate.date &&
                    normalizeAmount(it.amount) == normalizeAmount(candidate.amount) &&
                    normalizeText(it.comment) == normalizeText(candidate.comment)
        }
    }

    private fun containsTechnique(
        existing: List<TechniqueExpenseDraft>,
        candidate: TechniqueExpenseDraft
    ): Boolean {
        return existing.any {
            it.date == candidate.date &&
                    normalizeAmount(it.amount) == normalizeAmount(candidate.amount) &&
                    normalizeText(it.comment) == normalizeText(candidate.comment)
        }
    }

    private fun normalizeAmount(value: String): String {
        return value
            .replace(" ", "")
            .replace(",", ".")
            .trim()
    }

    private fun normalizeText(value: String): String {
        return value
            .trim()
            .lowercase()
            .replace("\r", " ")
            .replace("\n", " ")
            .replace(Regex("\\s+"), " ")
    }

    private data class DuplicateSummary(
        val duplicateCount: Int,
        val newCount: Int
    )
}