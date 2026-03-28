package com.carcost.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class ExpenseTypeActivity : AppCompatActivity() {

    private lateinit var btnTechnique: Button
    private lateinit var btnDocumentation: Button
    private lateinit var btnExpenseTypeBack: Button

    private val expenseFormLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                setResult(RESULT_OK)
                finish()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_type)

        btnTechnique = findViewById(R.id.btnTechnique)
        btnDocumentation = findViewById(R.id.btnDocumentation)
        btnExpenseTypeBack = findViewById(R.id.btnExpenseTypeBack)

        btnTechnique.setOnClickListener {
            expenseFormLauncher.launch(Intent(this, TechniqueExpenseActivity::class.java))
        }

        btnDocumentation.setOnClickListener {
            expenseFormLauncher.launch(Intent(this, DocumentationExpenseActivity::class.java))
        }

        btnExpenseTypeBack.setOnClickListener {
            finish()
        }
    }
}