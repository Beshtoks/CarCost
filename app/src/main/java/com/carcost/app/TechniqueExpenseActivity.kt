package com.carcost.app

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

class TechniqueExpenseActivity : AppCompatActivity() {

    private lateinit var spinnerTechniqueRecordType: Spinner
    private lateinit var spinnerQuantityUnit: Spinner
    private lateinit var titlesContainer: LinearLayout

    private lateinit var btnTechniqueSave: Button
    private lateinit var btnTechniqueCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_technique_expense)

        spinnerTechniqueRecordType = findViewById(R.id.spinnerTechniqueRecordType)
        spinnerQuantityUnit = findViewById(R.id.spinnerQuantityUnit)
        titlesContainer = findViewById(R.id.titlesContainer)

        btnTechniqueSave = findViewById(R.id.btnTechniqueSave)
        btnTechniqueCancel = findViewById(R.id.btnTechniqueCancel)

        setupSpinners()
        addTitleRow()

        btnTechniqueSave.setOnClickListener {
            Toast.makeText(this, R.string.message_save_not_implemented, Toast.LENGTH_SHORT).show()
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
            hint = getString(R.string.hint_technique_title)
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }
}