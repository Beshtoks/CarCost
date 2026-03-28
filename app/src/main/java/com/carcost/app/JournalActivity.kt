package com.carcost.app

import android.os.Bundle
import android.text.InputType
import android.view.View
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ListView
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle

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
            showEntryDialog(item)
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
        items.clear()

        storage.loadIncomeDrafts().forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            items.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_income_short),
                    shortTitle = draft.title.ifBlank { draft.type },
                    amount = draft.amount,
                    details = buildIncomeDetails(draft),
                    editAction = {
                        showEditIncomeDialog(draft)
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
            items.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_documentation_short),
                    shortTitle = draft.title.ifBlank { draft.type },
                    amount = draft.amount,
                    details = buildDocumentationDetails(draft),
                    editAction = {
                        showEditDocumentationDialog(draft)
                    },
                    deleteAction = {
                        val list = storage.loadDocumentationDrafts()
                        val index = list.indexOfFirst {
                            it.type == draft.type &&
                                    it.subtype == draft.subtype &&
                                    it.title == draft.title &&
                                    it.date == draft.date &&
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
            items.add(
                JournalListItem(
                    date = date,
                    typeCode = getString(R.string.journal_type_technique_short),
                    shortTitle = draft.titles.firstOrNull().orEmpty().ifBlank { draft.recordType },
                    amount = draft.amount,
                    details = buildTechniqueDetails(draft),
                    editAction = {
                        showEditTechniqueDialog(draft)
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

        items.sortByDescending { it.date }
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
                item.editAction.invoke()
            }
            .setNeutralButton(R.string.journal_delete) { _, _ ->
                val deleted = item.deleteAction.invoke()
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

    private fun showEditIncomeDialog(original: IncomeDraft) {
        val container = createFormContainer()

        val etType = addTextField(container, "Тип", original.type)
        val etSubtype = addTextField(container, "Подтип", original.subtype.orEmpty())
        val etTitle = addTextField(container, "Название", original.title)
        val etDate = addTextField(container, "Дата", original.date)
        val etAmount = addTextField(container, "Сумма", original.amount, true)
        val etComment = addTextField(container, "Комментарий", original.comment)

        showEditorDialog(
            title = "Изменить доход",
            container = container
        ) {
            val type = etType.text.toString().trim()
            val subtype = etSubtype.text.toString().trim().ifBlank { null }
            val title = etTitle.text.toString().trim()
            val date = etDate.text.toString().trim()
            val amount = normalizeAmount(etAmount.text.toString().trim())
            val comment = etComment.text.toString().trim()

            if (date.isEmpty() || amount == null) {
                Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            if (parseDate(date) == null) {
                Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            val updated = IncomeDraft(
                type = type,
                subtype = subtype,
                title = title,
                date = date,
                amount = amount,
                comment = comment
            )

            val list = storage.loadIncomeDrafts()
            val index = list.indexOfFirst {
                it.type == original.type &&
                        it.subtype == original.subtype &&
                        it.title == original.title &&
                        it.date == original.date &&
                        it.amount == original.amount &&
                        it.comment == original.comment
            }

            if (index < 0) {
                Toast.makeText(this, "Не удалось найти запись для изменения", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            list[index] = updated
            storage.saveIncomeDrafts(list)
            loadItems()
            renderState()
            Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showEditDocumentationDialog(original: DocumentationExpenseDraft) {
        val container = createFormContainer()

        val etType = addTextField(container, "Тип", original.type)
        val etSubtype = addTextField(container, "Подтип", original.subtype.orEmpty())
        val etTitle = addTextField(container, "Название", original.title)
        val etDate = addTextField(container, "Дата", original.date)
        val etValidUntil = addTextField(container, "Годен до", original.validUntil.orEmpty())
        val etAmount = addTextField(container, "Сумма", original.amount, true)
        val etComment = addTextField(container, "Комментарий", original.comment)

        showEditorDialog(
            title = "Изменить документацию",
            container = container
        ) {
            val type = etType.text.toString().trim()
            val subtype = etSubtype.text.toString().trim().ifBlank { null }
            val title = etTitle.text.toString().trim()
            val date = etDate.text.toString().trim()
            val validUntil = etValidUntil.text.toString().trim().ifBlank { null }
            val amount = normalizeAmount(etAmount.text.toString().trim())
            val comment = etComment.text.toString().trim()

            if (date.isEmpty() || amount == null) {
                Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            if (parseDate(date) == null) {
                Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            if (validUntil != null && parseDate(validUntil) == null) {
                Toast.makeText(this, "Некорректная дата в поле \"Годен до\"", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            val updated = DocumentationExpenseDraft(
                type = type,
                subtype = subtype,
                title = title,
                date = date,
                validUntil = validUntil,
                amount = amount,
                comment = comment
            )

            val list = storage.loadDocumentationDrafts()
            val index = list.indexOfFirst {
                it.type == original.type &&
                        it.subtype == original.subtype &&
                        it.title == original.title &&
                        it.date == original.date &&
                        it.validUntil == original.validUntil &&
                        it.amount == original.amount &&
                        it.comment == original.comment
            }

            if (index < 0) {
                Toast.makeText(this, "Не удалось найти запись для изменения", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            list[index] = updated
            storage.saveDocumentationDrafts(list)
            loadItems()
            renderState()
            Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showEditTechniqueDialog(original: TechniqueExpenseDraft) {
        val container = createFormContainer()

        val etRecordType = addTextField(container, "Тип записи", original.recordType)
        val etTitles = addTextField(container, "Название", original.titles.joinToString(", "))
        val etDate = addTextField(container, "Дата", original.date)
        val etMileage = addTextField(container, "Пробег", original.mileage, false, InputType.TYPE_CLASS_NUMBER)
        val etQuantity = addTextField(container, "Количество", original.quantity)
        val etQuantityUnit = addTextField(container, "Ед. изм.", original.quantityUnit)
        val etAmount = addTextField(container, "Сумма", original.amount, true)
        val etComment = addTextField(container, "Комментарий", original.comment)

        showEditorDialog(
            title = "Изменить технику",
            container = container
        ) {
            val recordType = etRecordType.text.toString().trim()
            val titlesRaw = etTitles.text.toString().trim()
            val titles = titlesRaw.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val date = etDate.text.toString().trim()
            val mileage = etMileage.text.toString().trim()
            val quantity = etQuantity.text.toString().trim()
            val quantityUnit = etQuantityUnit.text.toString().trim()
            val amount = normalizeAmount(etAmount.text.toString().trim())
            val comment = etComment.text.toString().trim()

            if (date.isEmpty() || amount == null) {
                Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            if (parseDate(date) == null) {
                Toast.makeText(this, "Некорректная дата", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            if (titles.isEmpty()) {
                Toast.makeText(this, R.string.message_add_one_title, Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            val updated = TechniqueExpenseDraft(
                recordType = recordType,
                titles = titles,
                date = date,
                mileage = mileage,
                quantity = quantity,
                quantityUnit = quantityUnit,
                amount = amount,
                comment = comment
            )

            val list = storage.loadTechniqueDrafts()
            val index = list.indexOfFirst {
                it.recordType == original.recordType &&
                        it.titles == original.titles &&
                        it.date == original.date &&
                        it.mileage == original.mileage &&
                        it.quantity == original.quantity &&
                        it.quantityUnit == original.quantityUnit &&
                        it.amount == original.amount &&
                        it.comment == original.comment
            }

            if (index < 0) {
                Toast.makeText(this, "Не удалось найти запись для изменения", Toast.LENGTH_SHORT).show()
                return@showEditorDialog false
            }

            list[index] = updated
            storage.saveTechniqueDrafts(list)
            loadItems()
            renderState()
            Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
            true
        }
    }

    private fun showEditorDialog(
        title: String,
        container: View,
        onSave: () -> Boolean
    ) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setView(container)
            .setPositiveButton("Сохранить") { dialog, _ ->
                val success = onSave.invoke()
                if (!success) {
                    dialog.dismiss()
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun createFormContainer(): View {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(8), dp(8), dp(8), dp(8))
        }

        return ScrollView(this).apply {
            addView(layout)
            tag = layout
        }
    }

    private fun addTextField(
        container: View,
        label: String,
        value: String,
        decimal: Boolean = false,
        explicitInputType: Int? = null
    ): EditText {
        val layout = (container as ScrollView).tag as LinearLayout

        val tvLabel = TextView(this).apply {
            text = label
            textSize = 14f
        }

        val etValue = EditText(this).apply {
            setText(value)
            inputType = explicitInputType ?: if (decimal) {
                InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            } else {
                InputType.TYPE_CLASS_TEXT
            }
        }

        layout.addView(tvLabel)
        layout.addView(etValue)

        return etValue
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
        val comment = draft.comment.ifBlank { "—" }

        return buildString {
            appendLine("Тип: ${draft.type}")
            appendLine("Подтип: $subtype")
            appendLine("Название: ${draft.title.ifBlank { "—" }}")
            appendLine("Дата: ${draft.date}")
            appendLine("Годен до: $validUntil")
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

    private fun normalizeAmount(value: String): String? {
        val cleaned = value
            .replace(" ", "")
            .replace(",", ".")

        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (parsed < 0.0) return null

        return cleaned
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    }
}