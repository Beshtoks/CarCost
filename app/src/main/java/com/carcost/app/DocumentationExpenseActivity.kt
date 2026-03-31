package com.carcost.app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar
import java.util.Locale

class DocumentationExpenseActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var spinnerDocumentType: Spinner
    private lateinit var subtypeContainer: LinearLayout
    private lateinit var spinnerDocumentSubtype: Spinner

    private lateinit var etDocumentTitle: EditText
    private lateinit var etDocumentDate: EditText
    private lateinit var etDocumentValidUntil: EditText
    private lateinit var etDocumentOdometer: EditText
    private lateinit var etDocumentAmount: EditText
    private lateinit var etDocumentComment: EditText

    private lateinit var btnDocumentationSave: Button
    private lateinit var btnDocumentationCancel: Button
    private lateinit var btnDocumentationNext: Button
    private lateinit var btnDocumentationDelete: Button

    private var isEditMode: Boolean = false
    private var originalDraft: DocumentationExpenseDraft? = null
    private var switchSourceTechniqueDraft: TechniqueExpenseDraft? = null
    private var prefillDocumentationDraft: DocumentationExpenseDraft? = null
    private var isRestoringForm: Boolean = false

    private val isSwitchMode: Boolean
        get() = switchSourceTechniqueDraft != null && prefillDocumentationDraft != null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation_expense)

        storage = AppStorage(this)

        spinnerDocumentType = findViewById(R.id.spinnerDocumentType)
        subtypeContainer = findViewById(R.id.documentationSubtypeContainer)
        spinnerDocumentSubtype = findViewById(R.id.spinnerDocumentSubtype)

        etDocumentTitle = findViewById(R.id.etDocumentTitle)
        etDocumentDate = findViewById(R.id.etDocumentDate)
        etDocumentValidUntil = findViewById(R.id.etDocumentValidUntil)
        etDocumentOdometer = findViewById(R.id.etDocumentOdometer)
        etDocumentAmount = findViewById(R.id.etDocumentAmount)
        etDocumentComment = findViewById(R.id.etDocumentComment)

        btnDocumentationSave = findViewById(R.id.btnDocumentationSave)
        btnDocumentationCancel = findViewById(R.id.btnDocumentationCancel)
        btnDocumentationNext = findViewById(R.id.btnDocumentationNext)
        btnDocumentationDelete = findViewById(R.id.btnDocumentationDelete)

        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        @Suppress("DEPRECATION")
        originalDraft = intent.getSerializableExtra(EXTRA_ORIGINAL_DRAFT) as? DocumentationExpenseDraft
        @Suppress("DEPRECATION")
        switchSourceTechniqueDraft =
            intent.getSerializableExtra(EXTRA_SWITCH_SOURCE_TECHNIQUE_DRAFT) as? TechniqueExpenseDraft
        @Suppress("DEPRECATION")
        prefillDocumentationDraft =
            intent.getSerializableExtra(EXTRA_PREFILL_DOCUMENTATION_DRAFT) as? DocumentationExpenseDraft

        setupDocumentTypeSpinner()
        setupRequiredDateField(etDocumentDate)
        setupOptionalDateField(etDocumentValidUntil)

        when {
            isSwitchMode -> {
                fillFormFromDraft(prefillDocumentationDraft!!)
                btnDocumentationDelete.visibility = View.GONE
                btnDocumentationNext.text = "Техника"
            }

            isEditMode && originalDraft != null -> {
                fillFormFromDraft(prefillDocumentationDraft ?: originalDraft!!)
                btnDocumentationDelete.visibility = View.VISIBLE
                btnDocumentationNext.text = "Техника"
            }

            else -> {
                updateSubtypeVisibility()
                btnDocumentationDelete.visibility = View.GONE
                btnDocumentationNext.text = "Следующий"
            }
        }

        btnDocumentationSave.setOnClickListener {
            when {
                isSwitchMode -> saveSwitchedDocumentationAndClose()
                isEditMode -> updateDocumentationAndClose()
                else -> saveDocumentationAndClose()
            }
        }

        btnDocumentationNext.setOnClickListener {
            when {
                isSwitchMode || isEditMode -> switchToTechnique()
                else -> saveDocumentationAndStay()
            }
        }

        btnDocumentationCancel.setOnClickListener {
            finish()
        }

        btnDocumentationDelete.setOnClickListener {
            deleteDocumentationAndClose()
        }
    }

    private fun setupDocumentTypeSpinner() {
        val items = resources.getStringArray(R.array.documentation_types).toList()

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            items
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDocumentType.adapter = adapter

        spinnerDocumentType.onItemSelectedListener = SimpleItemSelectedListener {
            if (!isRestoringForm) {
                updateSubtypeVisibility()
            }
        }
    }

    private fun fillFormFromDraft(draft: DocumentationExpenseDraft) {
        isRestoringForm = true

        val documentTypes = resources.getStringArray(R.array.documentation_types)
        val typeIndex = documentTypes.indexOfFirst { it == draft.type }
        if (typeIndex >= 0) {
            spinnerDocumentType.setSelection(typeIndex, false)
        }

        updateSubtypeVisibility()

        if (!draft.subtype.isNullOrBlank() && subtypeContainer.visibility == View.VISIBLE) {
            val adapter = spinnerDocumentSubtype.adapter as? ArrayAdapter<*>
            if (adapter != null) {
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i)?.toString()?.trim() == draft.subtype.trim()) {
                        spinnerDocumentSubtype.setSelection(i, false)
                        break
                    }
                }
            }
        } else if (subtypeContainer.visibility == View.VISIBLE) {
            spinnerDocumentSubtype.setSelection(0, false)
        }

        etDocumentTitle.setText(draft.title)
        etDocumentDate.setText(draft.date)
        etDocumentValidUntil.setText(draft.validUntil.orEmpty())
        etDocumentOdometer.setText(draft.odometer)
        etDocumentAmount.setText(draft.amount)
        etDocumentComment.setText(draft.comment)

        isRestoringForm = false
    }

    private fun updateSubtypeVisibility() {
        val arrayResId = when (spinnerDocumentType.selectedItemPosition) {
            0 -> R.array.documentation_subtypes_license
            1 -> R.array.documentation_subtypes_taxes
            2 -> R.array.documentation_subtypes_banking
            3 -> R.array.documentation_subtypes_accounting
            4 -> R.array.documentation_subtypes_permissions
            5 -> R.array.documentation_subtypes_insurance
            6 -> R.array.documentation_subtypes_parking
            7 -> R.array.documentation_subtypes_services
            8 -> R.array.documentation_subtypes_fines
            9 -> R.array.documentation_subtypes_passes
            10 -> R.array.documentation_subtypes_credit
            else -> null
        }

        if (arrayResId == null) {
            subtypeContainer.visibility = View.GONE
            return
        }

        subtypeContainer.visibility = View.VISIBLE
        val subtypeItems = resources.getStringArray(arrayResId).toList()

        val subtypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            subtypeItems
        )
        subtypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerDocumentSubtype.adapter = subtypeAdapter

        if (!isEditMode && !isSwitchMode) {
            spinnerDocumentSubtype.setSelection(0, false)
        }
    }

    private fun setupRequiredDateField(editText: EditText) {
        editText.keyListener = null
        editText.isFocusable = false
        editText.isClickable = true

        editText.setOnClickListener {
            showDatePicker(editText)
        }
    }

    private fun setupOptionalDateField(editText: EditText) {
        editText.keyListener = null
        editText.isFocusable = false
        editText.isClickable = true
        editText.isLongClickable = true

        editText.setOnClickListener {
            showDatePicker(editText)
        }

        editText.setOnLongClickListener {
            if (editText.text.isNullOrEmpty()) {
                false
            } else {
                editText.text?.clear()
                Toast.makeText(this, "Дата очищена", Toast.LENGTH_SHORT).show()
                true
            }
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

    private fun saveDocumentationAndClose() {
        val draft = buildDocumentationDraft() ?: return

        val resultIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_DOCUMENTATION_DRAFT, draft)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun saveDocumentationAndStay() {
        val draft = buildDocumentationDraft() ?: return

        val list = storage.loadDocumentationDrafts()
        list.add(draft)
        storage.saveDocumentationDrafts(list)

        Toast.makeText(
            this,
            getString(
                R.string.message_documentation_saved_with_amount,
                draft.amount,
                list.size.toString()
            ),
            Toast.LENGTH_SHORT
        ).show()

        clearFieldsForNextDocumentation()
    }

    private fun saveSwitchedDocumentationAndClose() {
        val switchedDraft = buildDocumentationDraft() ?: return
        val sourceTechniqueDraft = switchSourceTechniqueDraft ?: return

        val techniqueList = storage.loadTechniqueDrafts()
        val techniqueIndex = techniqueList.indexOfFirst {
            it.recordType == sourceTechniqueDraft.recordType &&
                    it.titles == sourceTechniqueDraft.titles &&
                    it.date == sourceTechniqueDraft.date &&
                    it.mileage == sourceTechniqueDraft.mileage &&
                    it.quantity == sourceTechniqueDraft.quantity &&
                    it.quantityUnit == sourceTechniqueDraft.quantityUnit &&
                    it.amount == sourceTechniqueDraft.amount &&
                    it.comment == sourceTechniqueDraft.comment
        }

        if (techniqueIndex < 0) {
            Toast.makeText(this, "Не удалось найти исходную запись Техника", Toast.LENGTH_SHORT).show()
            return
        }

        val documentationList = storage.loadDocumentationDrafts()
        techniqueList.removeAt(techniqueIndex)
        documentationList.add(switchedDraft)

        storage.saveTechniqueDrafts(techniqueList)
        storage.saveDocumentationDrafts(documentationList)

        Toast.makeText(this, "Тип записи изменён на Документация", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateDocumentationAndClose() {
        val updated = buildDocumentationDraft() ?: return
        val original = originalDraft ?: return

        val list = storage.loadDocumentationDrafts()
        val index = list.indexOfFirst {
            it.type == original.type &&
                    it.subtype == original.subtype &&
                    it.title == original.title &&
                    it.date == original.date &&
                    it.odometer == original.odometer &&
                    it.validUntil == original.validUntil &&
                    it.amount == original.amount &&
                    it.comment == original.comment
        }

        if (index < 0) {
            Toast.makeText(this, "Не удалось найти запись для изменения", Toast.LENGTH_SHORT).show()
            return
        }

        list[index] = updated
        storage.saveDocumentationDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateDocumentationAndStay() {
        val updated = buildDocumentationDraft() ?: return
        val original = originalDraft ?: return

        val list = storage.loadDocumentationDrafts()
        val index = list.indexOfFirst {
            it.type == original.type &&
                    it.subtype == original.subtype &&
                    it.title == original.title &&
                    it.date == original.date &&
                    it.odometer == original.odometer &&
                    it.validUntil == original.validUntil &&
                    it.amount == original.amount &&
                    it.comment == original.comment
        }

        if (index < 0) {
            Toast.makeText(this, "Не удалось найти запись для изменения", Toast.LENGTH_SHORT).show()
            return
        }

        list[index] = updated
        storage.saveDocumentationDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
    }

    private fun deleteDocumentationAndClose() {
        val original = originalDraft ?: return

        val list = storage.loadDocumentationDrafts()
        val index = list.indexOfFirst {
            it.type == original.type &&
                    it.subtype == original.subtype &&
                    it.title == original.title &&
                    it.date == original.date &&
                    it.odometer == original.odometer &&
                    it.validUntil == original.validUntil &&
                    it.amount == original.amount &&
                    it.comment == original.comment
        }

        if (index < 0) {
            Toast.makeText(this, "Не удалось найти запись для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        list.removeAt(index)
        storage.saveDocumentationDrafts(list)

        Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun switchToTechnique() {
        val techniqueDraft = buildTechniqueDraftForSwitch() ?: return
        val sourceDocumentation = if (isSwitchMode) null else originalDraft
        val sourceTechnique = switchSourceTechniqueDraft

        val intent = Intent(this, TechniqueExpenseActivity::class.java).apply {
            when {
                sourceDocumentation != null -> {
                    putExtra(TechniqueExpenseActivity.EXTRA_SWITCH_SOURCE_DOCUMENTATION_DRAFT, sourceDocumentation)
                    putExtra(TechniqueExpenseActivity.EXTRA_PREFILL_TECHNIQUE_DRAFT, techniqueDraft)
                }

                sourceTechnique != null -> {
                    putExtra(TechniqueExpenseActivity.EXTRA_EDIT_MODE, true)
                    putExtra(TechniqueExpenseActivity.EXTRA_ORIGINAL_DRAFT, sourceTechnique)
                    putExtra(TechniqueExpenseActivity.EXTRA_PREFILL_TECHNIQUE_DRAFT, techniqueDraft)
                }
            }
        }

        startActivity(intent)
        finish()
    }

    private fun buildTechniqueDraftForSwitch(): TechniqueExpenseDraft? {
        val documentationDraft = buildDocumentationDraft() ?: return null
        val defaultRecordType =
            resources.getStringArray(R.array.technique_record_types).firstOrNull().orEmpty()
        val title = documentationDraft.title.ifBlank {
            documentationDraft.type.ifBlank { "Без названия" }
        }

        return TechniqueExpenseDraft(
            recordType = defaultRecordType,
            titles = listOf(title),
            date = documentationDraft.date,
            mileage = documentationDraft.odometer,
            quantity = "1",
            quantityUnit = "шт",
            amount = documentationDraft.amount,
            comment = documentationDraft.comment
        )
    }

    private fun buildDocumentationDraft(): DocumentationExpenseDraft? {
        val type = spinnerDocumentType.selectedItem?.toString().orEmpty()
        val subtype = if (subtypeContainer.visibility == View.VISIBLE) {
            spinnerDocumentSubtype.selectedItem?.toString()?.trim()?.ifEmpty { null }
        } else {
            null
        }

        val title = etDocumentTitle.text.toString().trim()
        val date = etDocumentDate.text.toString().trim()
        val validUntilRaw = etDocumentValidUntil.text.toString().trim()
        val odometer = etDocumentOdometer.text.toString().trim()
        val rawAmount = etDocumentAmount.text.toString().trim()
        val comment = etDocumentComment.text.toString().trim()

        if (date.isEmpty() || rawAmount.isEmpty()) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return null
        }

        val normalizedAmount = normalizeAmount(rawAmount)
        if (normalizedAmount == null) {
            Toast.makeText(this, R.string.message_invalid_amount, Toast.LENGTH_SHORT).show()
            return null
        }

        return DocumentationExpenseDraft(
            type = type,
            subtype = subtype,
            title = title,
            date = date,
            odometer = odometer,
            validUntil = validUntilRaw.ifEmpty { null },
            amount = normalizedAmount,
            comment = comment
        )
    }

    private fun clearFieldsForNextDocumentation() {
        etDocumentTitle.text?.clear()
        etDocumentValidUntil.text?.clear()
        etDocumentOdometer.text?.clear()
        etDocumentAmount.text?.clear()
        etDocumentComment.text?.clear()
        etDocumentTitle.requestFocus()
        if (subtypeContainer.visibility == View.VISIBLE) {
            spinnerDocumentSubtype.setSelection(0, false)
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

    companion object {
        const val EXTRA_EDIT_MODE = "extra_edit_mode"
        const val EXTRA_ORIGINAL_DRAFT = "extra_original_draft"
        const val EXTRA_SWITCH_SOURCE_TECHNIQUE_DRAFT = "extra_switch_source_technique_draft"
        const val EXTRA_PREFILL_DOCUMENTATION_DRAFT = "extra_prefill_documentation_draft"
    }
}