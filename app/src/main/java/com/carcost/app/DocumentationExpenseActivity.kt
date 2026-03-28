package com.carcost.app

import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class DocumentationExpenseActivity : AppCompatActivity() {

    private lateinit var spinnerDocumentType: Spinner
    private lateinit var subtypeContainer: LinearLayout
    private lateinit var spinnerDocumentSubtype: Spinner

    private lateinit var btnDocumentationSave: Button
    private lateinit var btnDocumentationCancel: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_documentation_expense)

        spinnerDocumentType = findViewById(R.id.spinnerDocumentType)
        subtypeContainer = findViewById(R.id.documentationSubtypeContainer)
        spinnerDocumentSubtype = findViewById(R.id.spinnerDocumentSubtype)

        btnDocumentationSave = findViewById(R.id.btnDocumentationSave)
        btnDocumentationCancel = findViewById(R.id.btnDocumentationCancel)

        setupDocumentTypeSpinner()
        updateSubtypeVisibility()

        btnDocumentationSave.setOnClickListener {
            Toast.makeText(this, R.string.message_save_not_implemented, Toast.LENGTH_SHORT).show()
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
}