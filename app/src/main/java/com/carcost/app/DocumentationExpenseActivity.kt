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

    private lateinit var spinnerDocumentType: Spinner
    private lateinit var subtypeContainer: LinearLayout
    private lateinit var spinnerDocumentSubtype: Spinner

    private lateinit var etDocumentTitle: EditText
    private lateinit var etDocumentDate: EditText
    private lateinit var etDocumentValidUntil: EditText
    private lateinit var etDocumentAmount: EditText
    private lateinit var etDocumentComment: EditText

    private lateinit var btnDocumentationSave: Button
    private lateinit var btnDocumentationCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation_expense)

        spinnerDocumentType = findViewById(R.id.spinnerDocumentType)
        subtypeContainer = findViewById(R.id.documentationSubtypeContainer)
        spinnerDocumentSubtype = findViewById(R.id.spinnerDocumentSubtype)

        etDocumentTitle = findViewById(R.id.etDocumentTitle)
        etDocumentDate = findViewById(R.id.etDocumentDate)
        etDocumentValidUntil = findViewById(R.id.etDocumentValidUntil)
        etDocumentAmount = findViewById(R.id.etDocumentAmount)
        etDocumentComment = findViewById(R.id.etDocumentComment)

        btnDocumentationSave = findViewById(R.id.btnDocumentationSave)
        btnDocumentationCancel = findViewById(R.id.btnDocumentationCancel)

        etDocumentTitle.hint = null
        etDocumentDate.hint = null
        etDocumentValidUntil.hint = null
        etDocumentAmount.hint = null
        etDocumentComment.hint = null

        setupDocumentTypeSpinner()
        setupDateField(etDocumentDate)
        setupDateField(etDocumentValidUntil)
        updateSubtypeVisibility()

        btnDocumentationSave.setOnClickListener {
            saveDocumentation()
        }

        btnDocumentationCancel.setOnClickListener {
            finish()
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
            updateSubtypeVisibility()
        }
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

    private fun saveDocumentation() {
        val type = spinnerDocumentType.selectedItem?.toString().orEmpty()
        val subtype = if (subtypeContainer.visibility == View.VISIBLE) {
            spinnerDocumentSubtype.selectedItem?.toString()
        } else {
            null
        }

        val title = etDocumentTitle.text.toString().trim()
        val date = etDocumentDate.text.toString().trim()
        val validUntilRaw = etDocumentValidUntil.text.toString().trim()
        val rawAmount = etDocumentAmount.text.toString().trim()
        val comment = etDocumentComment.text.toString().trim()

        if (date.isEmpty() || rawAmount.isEmpty()) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedAmount = normalizeAmount(rawAmount)
        if (normalizedAmount == null) {
            Toast.makeText(this, R.string.message_invalid_amount, Toast.LENGTH_SHORT).show()
            return
        }

        val draft = DocumentationExpenseDraft(
            type = type,
            subtype = subtype,
            title = title,
            date = date,
            validUntil = validUntilRaw.ifEmpty { null },
            amount = normalizedAmount,
            comment = comment
        )

        val resultIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_DOCUMENTATION_DRAFT, draft)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun normalizeAmount(value: String): String? {
        val cleaned = value
            .replace(" ", "")
            .replace(",", ".")

        val parsed = cleaned.toDoubleOrNull() ?: return null
        if (parsed < 0.0) return null

        return cleaned
    }
}