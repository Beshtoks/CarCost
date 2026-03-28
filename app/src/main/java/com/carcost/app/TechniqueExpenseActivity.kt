package com.carcost.app

import android.app.Activity
import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.ArrayAdapter
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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technique_expense)

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

        etTechniqueDate.hint = null
        etTechniqueMileage.hint = null
        etTechniqueQuantity.hint = null
        etTechniqueAmount.hint = null
        etTechniqueComment.hint = null

        setupSpinners()
        setupDateField(etTechniqueDate)
        addTitleRow()

        btnTechniqueSave.setOnClickListener {
            saveTechnique()
        }

        btnTechniqueCancel.setOnClickListener {
            finish()
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

    private fun addTitleRow() {
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

        val editText = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_FLAG_CAP_SENTENCES
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

        row.addView(editText)
        row.addView(addButton)
        titlesContainer.addView(row)
    }

    private fun saveTechnique() {
        val recordType = spinnerTechniqueRecordType.selectedItem?.toString().orEmpty()
        val titles = collectTitles()
        val date = etTechniqueDate.text.toString().trim()
        val mileage = etTechniqueMileage.text.toString().trim()
        val quantity = etTechniqueQuantity.text.toString().trim()
        val quantityUnit = spinnerQuantityUnit.selectedItem?.toString().orEmpty()
        val rawAmount = etTechniqueAmount.text.toString().trim()
        val comment = etTechniqueComment.text.toString().trim()

        if (date.isEmpty() || rawAmount.isEmpty()) {
            Toast.makeText(this, R.string.message_fill_required_fields, Toast.LENGTH_SHORT).show()
            return
        }

        if (titles.isEmpty()) {
            Toast.makeText(this, R.string.message_add_one_title, Toast.LENGTH_SHORT).show()
            return
        }

        val normalizedAmount = normalizeAmount(rawAmount)
        if (normalizedAmount == null) {
            Toast.makeText(this, R.string.message_invalid_amount, Toast.LENGTH_SHORT).show()
            return
        }

        val draft = TechniqueExpenseDraft(
            recordType = recordType,
            titles = titles,
            date = date,
            mileage = mileage,
            quantity = quantity,
            quantityUnit = quantityUnit,
            amount = normalizedAmount,
            comment = comment
        )

        val resultIntent = Intent().apply {
            putExtra(MainActivity.EXTRA_TECHNIQUE_DRAFT, draft)
        }

        setResult(Activity.RESULT_OK, resultIntent)
        finish()
    }

    private fun collectTitles(): List<String> {
        val result = mutableListOf<String>()

        for (i in 0 until titlesContainer.childCount) {
            val row = titlesContainer.getChildAt(i) as LinearLayout
            val editText = row.getChildAt(0) as EditText
            val value = editText.text.toString().trim()
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}