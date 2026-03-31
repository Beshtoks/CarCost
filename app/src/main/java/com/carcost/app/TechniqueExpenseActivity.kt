package com.carcost.app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

class TechniqueExpenseActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var spinnerTechniqueRecordType: Spinner
    private lateinit var spinnerQuantityUnit: Spinner
    private lateinit var titlesContainer: LinearLayout

    private lateinit var etTechniqueDate: EditText
    private lateinit var etTechniqueMileage: EditText
    private lateinit var etTechniqueQuantity: EditText
    private lateinit var etTechniqueAmount: EditText
    private lateinit var etTechniqueComment: EditText

    private lateinit var btnTechniqueSave: Button
    private lateinit var btnTechniqueCancel: Button
    private lateinit var btnTechniqueNext: Button
    private lateinit var btnTechniqueDelete: Button

    private var isEditMode: Boolean = false
    private var originalDraft: TechniqueExpenseDraft? = null
    private var switchSourceDocumentationDraft: DocumentationExpenseDraft? = null
    private var prefillTechniqueDraft: TechniqueExpenseDraft? = null

    private val isSwitchMode: Boolean
        get() = switchSourceDocumentationDraft != null && prefillTechniqueDraft != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technique_expense)

        storage = AppStorage(this)

        spinnerTechniqueRecordType = findViewById(R.id.spinnerTechniqueRecordType)
        spinnerQuantityUnit = findViewById(R.id.spinnerQuantityUnit)
        titlesContainer = findViewById(R.id.titlesContainer)

        etTechniqueDate = findViewById(R.id.etTechniqueDate)
        etTechniqueMileage = findViewById(R.id.etTechniqueMileage)
        etTechniqueQuantity = findViewById(R.id.etTechniqueQuantity)
        etTechniqueAmount = findViewById(R.id.etTechniqueAmount)
        etTechniqueComment = findViewById(R.id.etTechniqueComment)

        btnTechniqueSave = findViewById(R.id.btnTechniqueSave)
        btnTechniqueCancel = findViewById(R.id.btnTechniqueCancel)
        btnTechniqueNext = findViewById(R.id.btnTechniqueNext)
        btnTechniqueDelete = findViewById(R.id.btnTechniqueDelete)

        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        originalDraft = intent.getSerializableExtra(EXTRA_ORIGINAL_DRAFT) as? TechniqueExpenseDraft
        switchSourceDocumentationDraft = intent.getSerializableExtra(EXTRA_SWITCH_SOURCE_DOCUMENTATION_DRAFT) as? DocumentationExpenseDraft
        prefillTechniqueDraft = intent.getSerializableExtra(EXTRA_PREFILL_TECHNIQUE_DRAFT) as? TechniqueExpenseDraft

        setupSpinners()
        setupDateField(etTechniqueDate)

        when {
            isSwitchMode -> {
                fillFormFromDraft(prefillTechniqueDraft!!)
                btnTechniqueDelete.visibility = View.GONE
                btnTechniqueNext.text = "Документация"
            }
            isEditMode && originalDraft != null -> {
                fillFormFromDraft(prefillTechniqueDraft ?: originalDraft!!)
                btnTechniqueDelete.visibility = View.VISIBLE
                btnTechniqueNext.text = "Документация"
            }
            else -> {
                addTitleRow()
                btnTechniqueDelete.visibility = View.GONE
                btnTechniqueNext.text = "Следующий"
            }
        }

        btnTechniqueSave.setOnClickListener {
            when {
                isSwitchMode -> saveSwitchedTechniqueAndClose()
                isEditMode -> updateTechniqueAndClose()
                else -> saveTechniqueAndClose()
            }
        }

        btnTechniqueNext.setOnClickListener {
            when {
                isSwitchMode || isEditMode -> switchToDocumentation()
                else -> saveTechniqueAndStay()
            }
        }

        btnTechniqueCancel.setOnClickListener {
            finish()
        }

        btnTechniqueDelete.setOnClickListener {
            deleteTechniqueAndClose()
        }
    }

    private fun setupSpinners() {
        val recordTypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.technique_record_types).toList()
        )
        recordTypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerTechniqueRecordType.adapter = recordTypeAdapter

        val quantityUnitAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            resources.getStringArray(R.array.quantity_units).toList()
        )
        quantityUnitAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerQuantityUnit.adapter = quantityUnitAdapter
    }

    private fun fillFormFromDraft(draft: TechniqueExpenseDraft) {
        val recordTypes = resources.getStringArray(R.array.technique_record_types)
        val recordTypeIndex = recordTypes.indexOfFirst { it == draft.recordType }
        if (recordTypeIndex >= 0) {
            spinnerTechniqueRecordType.setSelection(recordTypeIndex)
        }

        val quantityUnits = resources.getStringArray(R.array.quantity_units)
        val quantityUnitIndex = quantityUnits.indexOfFirst { it == draft.quantityUnit }
        if (quantityUnitIndex >= 0) {
            spinnerQuantityUnit.setSelection(quantityUnitIndex)
        }

        titlesContainer.removeAllViews()
        if (draft.titles.isEmpty()) {
            addTitleRow()
        } else {
            draft.titles.forEach { title ->
                addTitleRow(title)
            }
        }

        etTechniqueDate.setText(draft.date)
        etTechniqueMileage.setText(draft.mileage)
        etTechniqueQuantity.setText(draft.quantity)
        etTechniqueAmount.setText(draft.amount)
        etTechniqueComment.setText(draft.comment)
    }

    private fun setupDateField(editText: EditText) {
        editText.keyListener = null
        editText.isFocusable = false
        editText.isClickable = true

        editText.setOnClickListener {
            showDatePicker(editText)
        }
    }

    private fun showDatePicker(target: EditText) {
        val calendar = parseDateOrToday(target.text.toString().trim())

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                target.setText(
                    String.format(
                        Locale.getDefault(),
                        "%02d.%02d.%04d",
                        dayOfMonth,
                        month + 1,
                        year
                    )
                )
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    private fun parseDateOrToday(value: String): Calendar {
        val calendar = Calendar.getInstance()
        val parts = value.split(".")

        if (parts.size == 3) {
            val day = parts[0].toIntOrNull()
            val month = parts[1].toIntOrNull()
            val year = parts[2].toIntOrNull()

            if (day != null && month != null && year != null) {
                calendar.set(year, month - 1, day)
            }
        }

        return calendar
    }

    private fun addTitleRow(prefill: String = "") {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            layoutParams = LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = dp(8)
            }
        }

        val titleSuggestions = listOf("Мойка")

        val autoCompleteTextView = AutoCompleteTextView(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
            setText(prefill)
            threshold = 1
            setAdapter(
                ArrayAdapter(
                    this@TechniqueExpenseActivity,
                    android.R.layout.simple_dropdown_item_1line,
                    titleSuggestions
                )
            )
            layoutParams = LinearLayout.LayoutParams(
                0,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                1f
            )
        }

        val addButton = ImageButton(this).apply {
            setImageResource(android.R.drawable.ic_input_add)
            contentDescription = getString(R.string.add_title_row)
            setBackground(null)
            layoutParams = LinearLayout.LayoutParams(
                dp(48),
                dp(48)
            ).apply {
                marginStart = dp(8)
            }
            setOnClickListener {
                addTitleRow()
            }
        }

        row.addView(autoCompleteTextView)
        row.addView(addButton)
        titlesContainer.addView(row)
    }

    private fun saveTechniqueAndClose() {
        val draft = buildTechniqueDraft() ?: return

        val resultIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_TECHNIQUE_DRAFT, draft)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun saveTechniqueAndStay() {
        val draft = buildTechniqueDraft() ?: return

        val list = storage.loadTechniqueDrafts()
        list.add(draft)
        storage.saveTechniqueDrafts(list)

        Toast.makeText(
            this,
            getString(
                R.string.message_technique_saved_with_amount,
                draft.amount,
                list.size.toString()
            ),
            Toast.LENGTH_SHORT
        ).show()

        clearFieldsForNextTechnique()
    }

    private fun saveSwitchedTechniqueAndClose() {
        val switchedDraft = buildTechniqueDraft() ?: return
        val sourceDocumentationDraft = switchSourceDocumentationDraft ?: return

        val documentationList = storage.loadDocumentationDrafts()
        val documentationIndex = documentationList.indexOfFirst {
            it.type == sourceDocumentationDraft.type &&
                    it.subtype == sourceDocumentationDraft.subtype &&
                    it.title == sourceDocumentationDraft.title &&
                    it.date == sourceDocumentationDraft.date &&
                    it.odometer == sourceDocumentationDraft.odometer &&
                    it.validUntil == sourceDocumentationDraft.validUntil &&
                    it.amount == sourceDocumentationDraft.amount &&
                    it.comment == sourceDocumentationDraft.comment
        }

        if (documentationIndex < 0) {
            Toast.makeText(this, "Не удалось найти исходную запись Документация", Toast.LENGTH_SHORT).show()
            return
        }

        val techniqueList = storage.loadTechniqueDrafts()
        documentationList.removeAt(documentationIndex)
        techniqueList.add(switchedDraft)

        storage.saveDocumentationDrafts(documentationList)
        storage.saveTechniqueDrafts(techniqueList)

        Toast.makeText(this, "Тип записи изменён на Техника", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateTechniqueAndClose() {
        val updated = buildTechniqueDraft() ?: return
        val original = originalDraft ?: return

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
            return
        }

        list[index] = updated
        storage.saveTechniqueDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateTechniqueAndStay() {
        val updated = buildTechniqueDraft() ?: return
        val original = originalDraft ?: return

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
            return
        }

        list[index] = updated
        storage.saveTechniqueDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
    }

    private fun deleteTechniqueAndClose() {
        val original = originalDraft ?: return

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
            Toast.makeText(this, "Не удалось найти запись для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        list.removeAt(index)
        storage.saveTechniqueDrafts(list)

        Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun switchToDocumentation() {
        val documentationDraft = buildDocumentationDraftForSwitch() ?: return
        val sourceTechnique = if (isSwitchMode) null else originalDraft
        val sourceDocumentation = switchSourceDocumentationDraft

        val intent = Intent(this, DocumentationExpenseActivity::class.java).apply {
            when {
                sourceTechnique != null -> {
                    putExtra(DocumentationExpenseActivity.EXTRA_SWITCH_SOURCE_TECHNIQUE_DRAFT, sourceTechnique)
                    putExtra(DocumentationExpenseActivity.EXTRA_PREFILL_DOCUMENTATION_DRAFT, documentationDraft)
                }
                sourceDocumentation != null -> {
                    putExtra(DocumentationExpenseActivity.EXTRA_EDIT_MODE, true)
                    putExtra(DocumentationExpenseActivity.EXTRA_ORIGINAL_DRAFT, sourceDocumentation)
                    putExtra(DocumentationExpenseActivity.EXTRA_PREFILL_DOCUMENTATION_DRAFT, documentationDraft)
                }
            }
        }

        startActivity(intent)
        finish()
    }

    private fun buildDocumentationDraftForSwitch(): DocumentationExpenseDraft? {
        val techniqueDraft = buildTechniqueDraft() ?: return null
        val defaultType = resources.getStringArray(R.array.documentation_types).lastOrNull().orEmpty()

        return DocumentationExpenseDraft(
            type = defaultType,
            subtype = null,
            title = techniqueDraft.titles.joinToString(", "),
            date = techniqueDraft.date,
            odometer = techniqueDraft.mileage,
            validUntil = null,
            amount = techniqueDraft.amount,
            comment = techniqueDraft.comment
        )
    }

    private fun buildTechniqueDraft(): TechniqueExpenseDraft? {
        val recordType = spinnerTechniqueRecordType.selectedItem?.toString().orEmpty()
        val titles = collectTitles()
        val date = etTechniqueDate.text.toString().trim()
        val mileage = etTechniqueMileage.text.toString().trim()
        val quantityUnit = spinnerQuantityUnit.selectedItem?.toString().orEmpty()
        val quantityInput = etTechniqueQuantity.text.toString().trim()
        val rawAmount = etTechniqueAmount.text.toString().trim()
        val comment = etTechniqueComment.text.toString().trim()

        if (date.isEmpty() || rawAmount.isEmpty()) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return null
        }

        if (titles.isEmpty()) {
            Toast.makeText(this, R.string.message_add_one_title, Toast.LENGTH_SHORT).show()
            return null
        }

        val normalizedAmount = normalizeAmount(rawAmount)
        if (normalizedAmount == null) {
            Toast.makeText(this, R.string.message_invalid_amount, Toast.LENGTH_SHORT).show()
            return null
        }

        val normalizedQuantity = normalizeQuantity(quantityInput, quantityUnit)
        if (normalizedQuantity == null) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return null
        }

        return TechniqueExpenseDraft(
            recordType = recordType,
            titles = titles,
            date = date,
            mileage = mileage,
            quantity = normalizedQuantity,
            quantityUnit = quantityUnit,
            amount = normalizedAmount,
            comment = comment
        )
    }

    private fun clearFieldsForNextTechnique() {
        titlesContainer.removeAllViews()
        addTitleRow()

        etTechniqueQuantity.text?.clear()
        etTechniqueAmount.text?.clear()
        etTechniqueComment.text?.clear()

        val firstRow = titlesContainer.getChildAt(0) as? LinearLayout
        val firstInput = firstRow?.getChildAt(0) as? AutoCompleteTextView
        firstInput?.requestFocus()
    }

    private fun collectTitles(): List<String> {
        val result = mutableListOf<String>()

        for (i in 0 until titlesContainer.childCount) {
            val row = titlesContainer.getChildAt(i) as LinearLayout
            val input = row.getChildAt(0) as AutoCompleteTextView
            val value = input.text.toString().trim()
            if (value.isNotEmpty()) {
                result.add(value)
            }
        }

        return result
    }

    private fun normalizeAmount(value: String): String? {
        val cleaned = value
            .replace(" ", "")
            .replace(",", ".")

        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (parsed < 0.0) return null

        return cleaned
    }

    private fun normalizeQuantity(value: String, unit: String): String? {
        val cleaned = value
            .replace(" ", "")
            .replace(",", ".")

        if (cleaned.isBlank()) {
            return if (unit == "шт") "1" else null
        }

        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (parsed <= 0.0) return null

        return cleaned
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
        const val EXTRA_ORIGINAL_DRAFT = "extra_original_draft"
        const val EXTRA_SWITCH_SOURCE_DOCUMENTATION_DRAFT = "extra_switch_source_documentation_draft"
        const val EXTRA_PREFILL_TECHNIQUE_DRAFT = "extra_prefill_technique_draft"
    }
}