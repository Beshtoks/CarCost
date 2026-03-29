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

class IncomeEntryActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var spinnerIncomeType: Spinner
    private lateinit var subtypeContainer: LinearLayout
    private lateinit var spinnerIncomeSubtype: Spinner

    private lateinit var etIncomeTitle: EditText
    private lateinit var etIncomeDate: EditText
    private lateinit var etIncomeAmount: EditText
    private lateinit var etIncomeComment: EditText

    private lateinit var btnIncomeSave: Button
    private lateinit var btnIncomeCancel: Button
    private lateinit var btnIncomeNext: Button
    private lateinit var btnIncomeDelete: Button

    private var isEditMode: Boolean = false
    private var originalDraft: IncomeDraft? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_entry)

        storage = AppStorage(this)

        spinnerIncomeType = findViewById(R.id.spinnerIncomeType)
        subtypeContainer = findViewById(R.id.subtypeContainer)
        spinnerIncomeSubtype = findViewById(R.id.spinnerIncomeSubtype)

        etIncomeTitle = findViewById(R.id.etIncomeTitle)
        etIncomeDate = findViewById(R.id.etIncomeDate)
        etIncomeAmount = findViewById(R.id.etIncomeAmount)
        etIncomeComment = findViewById(R.id.etIncomeComment)

        btnIncomeSave = findViewById(R.id.btnIncomeSave)
        btnIncomeCancel = findViewById(R.id.btnIncomeCancel)
        btnIncomeNext = findViewById(R.id.btnIncomeNext)
        btnIncomeDelete = findViewById(R.id.btnIncomeDelete)

        isEditMode = intent.getBooleanExtra(EXTRA_EDIT_MODE, false)
        originalDraft = intent.getSerializableExtra(EXTRA_ORIGINAL_DRAFT) as? IncomeDraft

        setupIncomeTypeSpinner()
        setupDateField(etIncomeDate)

        if (isEditMode && originalDraft != null) {
            fillFormFromDraft(originalDraft!!)
            btnIncomeDelete.visibility = View.VISIBLE
        } else {
            updateSubtypeVisibility()
            btnIncomeDelete.visibility = View.GONE
        }

        btnIncomeSave.setOnClickListener {
            if (isEditMode) {
                updateIncomeAndClose()
            } else {
                saveIncomeAndClose()
            }
        }

        btnIncomeNext.setOnClickListener {
            if (isEditMode) {
                updateIncomeAndStay()
            } else {
                saveIncomeAndStay()
            }
        }

        btnIncomeCancel.setOnClickListener {
            finish()
        }

        btnIncomeDelete.setOnClickListener {
            deleteIncomeAndClose()
        }
    }

    private fun setupIncomeTypeSpinner() {
        val typeItems = resources.getStringArray(R.array.income_types).toList()

        val typeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            typeItems
        )
        typeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIncomeType.adapter = typeAdapter

        spinnerIncomeType.onItemSelectedListener = SimpleItemSelectedListener {
            updateSubtypeVisibility()
        }
    }

    private fun fillFormFromDraft(draft: IncomeDraft) {
        val incomeTypes = resources.getStringArray(R.array.income_types)
        val typeIndex = incomeTypes.indexOfFirst { it == draft.type }
        if (typeIndex >= 0) {
            spinnerIncomeType.setSelection(typeIndex)
        }

        updateSubtypeVisibility()

        if (!draft.subtype.isNullOrBlank() && subtypeContainer.visibility == View.VISIBLE) {
            val adapter = spinnerIncomeSubtype.adapter as? ArrayAdapter<*>
            if (adapter != null) {
                for (i in 0 until adapter.count) {
                    if (adapter.getItem(i)?.toString() == draft.subtype) {
                        spinnerIncomeSubtype.setSelection(i)
                        break
                    }
                }
            }
        }

        etIncomeTitle.setText(draft.title)
        etIncomeDate.setText(draft.date)
        etIncomeAmount.setText(draft.amount)
        etIncomeComment.setText(draft.comment)
    }

    private fun updateSubtypeVisibility() {
        when (spinnerIncomeType.selectedItemPosition) {
            0 -> {
                subtypeContainer.visibility = View.VISIBLE
                bindSubtypeSpinner(R.array.income_subtypes_aggregators)
            }

            3 -> {
                subtypeContainer.visibility = View.VISIBLE
                bindSubtypeSpinner(R.array.income_subtypes_transfers)
            }

            else -> {
                subtypeContainer.visibility = View.GONE
            }
        }
    }

    private fun bindSubtypeSpinner(arrayResId: Int) {
        val subtypeItems = resources.getStringArray(arrayResId).toList()

        val subtypeAdapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_item,
            subtypeItems
        )
        subtypeAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerIncomeSubtype.adapter = subtypeAdapter
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

    private fun saveIncomeAndClose() {
        val draft = buildIncomeDraft() ?: return

        val resultIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_INCOME_DRAFT, draft)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun saveIncomeAndStay() {
        val draft = buildIncomeDraft() ?: return

        val list = storage.loadIncomeDrafts()
        list.add(draft)
        storage.saveIncomeDrafts(list)

        Toast.makeText(
            this,
            getString(
                R.string.message_income_saved_with_amount,
                draft.amount,
                list.size.toString()
            ),
            Toast.LENGTH_SHORT
        ).show()

        clearFieldsForNextIncome()
    }

    private fun updateIncomeAndClose() {
        val updated = buildIncomeDraft() ?: return
        val original = originalDraft ?: return

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
            return
        }

        list[index] = updated
        storage.saveIncomeDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun updateIncomeAndStay() {
        val updated = buildIncomeDraft() ?: return
        val original = originalDraft ?: return

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
            return
        }

        list[index] = updated
        storage.saveIncomeDrafts(list)
        originalDraft = updated

        Toast.makeText(this, "Запись изменена", Toast.LENGTH_SHORT).show()
    }

    private fun deleteIncomeAndClose() {
        val original = originalDraft ?: return

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
            Toast.makeText(this, "Не удалось найти запись для удаления", Toast.LENGTH_SHORT).show()
            return
        }

        list.removeAt(index)
        storage.saveIncomeDrafts(list)

        Toast.makeText(this, "Запись удалена", Toast.LENGTH_SHORT).show()
        finish()
    }

    private fun buildIncomeDraft(): IncomeDraft? {
        val type = spinnerIncomeType.selectedItem?.toString().orEmpty()
        val subtype = if (subtypeContainer.visibility == View.VISIBLE) {
            spinnerIncomeSubtype.selectedItem?.toString()
        } else {
            null
        }

        val title = etIncomeTitle.text.toString().trim()
        val date = etIncomeDate.text.toString().trim()
        val rawAmount = etIncomeAmount.text.toString().trim()
        val comment = etIncomeComment.text.toString().trim()

        if (date.isEmpty() || rawAmount.isEmpty()) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return null
        }

        val normalizedAmount = normalizeAmount(rawAmount)
        if (normalizedAmount == null) {
            Toast.makeText(this, R.string.message_invalid_amount, Toast.LENGTH_SHORT).show()
            return null
        }

        return IncomeDraft(
            type = type,
            subtype = subtype,
            title = title,
            date = date,
            amount = normalizedAmount,
            comment = comment
        )
    }

    private fun clearFieldsForNextIncome() {
        etIncomeTitle.text?.clear()
        etIncomeAmount.text?.clear()
        etIncomeComment.text?.clear()
        etIncomeTitle.requestFocus()
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
    }
}