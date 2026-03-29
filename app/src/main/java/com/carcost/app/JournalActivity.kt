package com.carcost.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.util.Locale

class JournalActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage
    private lateinit var tvJournalTitle: TextView
    private lateinit var lvJournal: ListView
    private lateinit var tvJournalEmpty: TextView

    private val items = mutableListOf<JournalListItem>()
    private lateinit var adapter: JournalAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_journal)

        storage = AppStorage(this)

        tvJournalTitle = findViewById(R.id.tvJournalTitle)
        lvJournal = findViewById(R.id.lvJournal)
        tvJournalEmpty = findViewById(R.id.tvJournalEmpty)

        tvJournalTitle.text = getString(R.string.journal_title)

        adapter = JournalAdapter(this, items)
        lvJournal.adapter = adapter

        lvJournal.setOnItemClickListener { _, _, position, _ ->
            val item = items[position]
            if (!item.isHeader) {
                showEntryDialog(item)
            }
        }

        loadItems()
        renderState()
    }

    override fun onResume() {
        super.onResume()
        loadItems()
        renderState()
    }

    private fun loadItems() {
        val rawItems = mutableListOf<JournalListItem>()

        storage.loadIncomeDrafts().forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            rawItems.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_income_short),
                    shortTitle = draft.title.ifBlank { draft.type },
                    amount = draft.amount,
                    details = buildIncomeDetails(draft),
                    editAction = {
                        val intent = Intent(this, IncomeEntryActivity::class.java).apply {
                            putExtra(IncomeEntryActivity.EXTRA_EDIT_MODE, true)
                            putExtra(IncomeEntryActivity.EXTRA_ORIGINAL_DRAFT, draft)
                        }
                        startActivity(intent)
                    },
                    deleteAction = {
                        val list = storage.loadIncomeDrafts()
                        val index = list.indexOfFirst {
                            it.type == draft.type &&
                                    it.subtype == draft.subtype &&
                                    it.title == draft.title &&
                                    it.date == draft.date &&
                                    it.amount == draft.amount &&
                                    it.comment == draft.comment
                        }
                        if (index >= 0) {
                            list.removeAt(index)
                            storage.saveIncomeDrafts(list)
                            true
                        } else {
                            false
                        }
                    }
                )
            )
        }

        storage.loadDocumentationDrafts().forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            rawItems.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_documentation_short),
                    shortTitle = draft.title.ifBlank { draft.type },
                    amount = draft.amount,
                    details = buildDocumentationDetails(draft),
                    editAction = {
                        val intent = Intent(this, DocumentationExpenseActivity::class.java).apply {
                            putExtra(DocumentationExpenseActivity.EXTRA_EDIT_MODE, true)
                            putExtra(DocumentationExpenseActivity.EXTRA_ORIGINAL_DRAFT, draft)
                        }
                        startActivity(intent)
                    },
                    deleteAction = {
                        val list = storage.loadDocumentationDrafts()
                        val index = list.indexOfFirst {
                            it.type == draft.type &&
                                    it.subtype == draft.subtype &&
                                    it.title == draft.title &&
                                    it.date == draft.date &&
                                    it.odometer == draft.odometer &&
                                    it.validUntil == draft.validUntil &&
                                    it.amount == draft.amount &&
                                    it.comment == draft.comment
                        }
                        if (index >= 0) {
                            list.removeAt(index)
                            storage.saveDocumentationDrafts(list)
                            true
                        } else {
                            false
                        }
                    }
                )
            )
        }

        storage.loadTechniqueDrafts().forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            rawItems.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_technique_short),
                    shortTitle = draft.titles.firstOrNull().orEmpty().ifBlank { draft.recordType },
                    amount = draft.amount,
                    details = buildTechniqueDetails(draft),
                    editAction = {
                        val intent = Intent(this, TechniqueExpenseActivity::class.java).apply {
                            putExtra(TechniqueExpenseActivity.EXTRA_EDIT_MODE, true)
                            putExtra(TechniqueExpenseActivity.EXTRA_ORIGINAL_DRAFT, draft)
                        }
                        startActivity(intent)
                    },
                    deleteAction = {
                        val list = storage.loadTechniqueDrafts()
                        val index = list.indexOfFirst {
                            it.recordType == draft.recordType &&
                                    it.titles == draft.titles &&
                                    it.date == draft.date &&
                                    it.mileage == draft.mileage &&
                                    it.quantity == draft.quantity &&
                                    it.quantityUnit == draft.quantityUnit &&
                                    it.amount == draft.amount &&
                                    it.comment == draft.comment
                        }
                        if (index >= 0) {
                            list.removeAt(index)
                            storage.saveTechniqueDrafts(list)
                            true
                        } else {
                            false
                        }
                    }
                )
            )
        }

        rawItems.sortByDescending { it.date }

        items.clear()

        var currentMonth: YearMonth? = null
        rawItems.forEach { item ->
            val itemDate = item.date ?: return@forEach
            val itemMonth = YearMonth.of(itemDate.year, itemDate.month)

            if (currentMonth != itemMonth) {
                items.add(JournalListItem.header(formatMonthHeader(itemDate)))
                currentMonth = itemMonth
            }

            items.add(item)
        }
    }

    private fun renderState() {
        adapter.notifyDataSetChanged()
        val isEmpty = items.isEmpty()
        lvJournal.visibility = if (isEmpty) View.GONE else View.VISIBLE
        tvJournalEmpty.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    private fun showEntryDialog(item: JournalListItem) {
        AlertDialog.Builder(this)
            .setTitle(formatDialogTitle(item))
            .setMessage(item.details)
            .setPositiveButton("Изменить") { _, _ ->
                item.editAction?.invoke()
            }
            .setNeutralButton(R.string.journal_delete) { _, _ ->
                val deleted = item.deleteAction?.invoke() == true
                if (deleted) {
                    loadItems()
                    renderState()
                    Toast.makeText(this, R.string.journal_deleted, Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, R.string.journal_delete_failed, Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton(R.string.journal_close, null)
            .show()
    }

    private fun formatDialogTitle(item: JournalListItem): String {
        return "${item.displayDate}  ${item.typeCode}"
    }

    private fun buildIncomeDetails(draft: IncomeDraft): String {
        val subtype = draft.subtype?.takeIf { it.isNotBlank() } ?: "—"
        val comment = draft.comment.ifBlank { "—" }

        return buildString {
            appendLine("Тип: ${draft.type}")
            appendLine("Подтип: $subtype")
            appendLine("Название: ${draft.title.ifBlank { "—" }}")
            appendLine("Дата: ${draft.date}")
            appendLine("Сумма: ${draft.amount} €")
            append("Комментарий: $comment")
        }
    }

    private fun buildDocumentationDetails(draft: DocumentationExpenseDraft): String {
        val subtype = draft.subtype?.takeIf { it.isNotBlank() } ?: "—"
        val validUntil = draft.validUntil?.takeIf { it.isNotBlank() } ?: "—"
        val odometer = draft.odometer.ifBlank { "—" }
        val comment = draft.comment.ifBlank { "—" }

        return buildString {
            appendLine("Тип: ${draft.type}")
            appendLine("Подтип: $subtype")
            appendLine("Название: ${draft.title.ifBlank { "—" }}")
            appendLine("Дата: ${draft.date}")
            appendLine("Годен до: $validUntil")
            appendLine("Спидометр: $odometer")
            appendLine("Сумма: ${draft.amount} €")
            append("Комментарий: $comment")
        }
    }

    private fun buildTechniqueDetails(draft: TechniqueExpenseDraft): String {
        val titles = if (draft.titles.isEmpty()) "—" else draft.titles.joinToString(", ")
        val mileage = draft.mileage.ifBlank { "—" }
        val quantity = buildQuantityText(draft)
        val comment = draft.comment.ifBlank { "—" }

        return buildString {
            appendLine("Тип записи: ${draft.recordType}")
            appendLine("Название: $titles")
            appendLine("Дата: ${draft.date}")
            appendLine("Пробег: $mileage")
            appendLine("Количество: $quantity")
            appendLine("Сумма: ${draft.amount} €")
            append("Комментарий: $comment")
        }
    }

    private fun buildQuantityText(draft: TechniqueExpenseDraft): String {
        val quantity = draft.quantity.ifBlank { return "—" }
        val unit = draft.quantityUnit.ifBlank { "" }
        return if (unit.isBlank()) quantity else "$quantity $unit"
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    private fun formatMonthHeader(date: LocalDate): String {
        val monthName = date.month.getDisplayName(java.time.format.TextStyle.FULL_STANDALONE, Locale("ru"))
        return monthName.replaceFirstChar { char ->
            if (char.isLowerCase()) char.titlecase(Locale("ru")) else char.toString()
        } + " ${date.year}"
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    }
}