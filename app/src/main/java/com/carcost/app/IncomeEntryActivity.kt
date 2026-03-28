package com.carcost.app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class IncomeEntryActivity : AppCompatActivity() {

    private lateinit var spinnerIncomeType: Spinner
    private lateinit var subtypeContainer: LinearLayout
    private lateinit var spinnerIncomeSubtype: Spinner

    private lateinit var etIncomeTitle: EditText
    private lateinit var etIncomeDate: EditText
    private lateinit var etIncomeAmount: EditText
    private lateinit var etIncomeComment: EditText

    private lateinit var btnIncomeSave: Button
    private lateinit var btnIncomeCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_income_entry)

        spinnerIncomeType = findViewById(R.id.spinnerIncomeType)
        subtypeContainer = findViewById(R.id.subtypeContainer)
        spinnerIncomeSubtype = findViewById(R.id.spinnerIncomeSubtype)

        etIncomeTitle = findViewById(R.id.etIncomeTitle)
        etIncomeDate = findViewById(R.id.etIncomeDate)
        etIncomeAmount = findViewById(R.id.etIncomeAmount)
        etIncomeComment = findViewById(R.id.etIncomeComment)

        btnIncomeSave = findViewById(R.id.btnIncomeSave)
        btnIncomeCancel = findViewById(R.id.btnIncomeCancel)

        setupIncomeTypeSpinner()
        updateSubtypeVisibility()

        btnIncomeSave.setOnClickListener {
            Toast.makeText(this, R.string.message_save_not_implemented, Toast.LENGTH_SHORT).show()
        }

        btnIncomeCancel.setOnClickListener {
            finish()
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
}