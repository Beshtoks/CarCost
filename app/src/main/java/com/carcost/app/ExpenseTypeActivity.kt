package com.carcost.app

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class ExpenseTypeActivity : AppCompatActivity() {

    private lateinit var btnTechnique: Button
    private lateinit var btnDocumentation: Button
    private lateinit var btnExpenseTypeBack: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_expense_type)

        btnTechnique = findViewById(R.id.btnTechnique)
        btnDocumentation = findViewById(R.id.btnDocumentation)
        btnExpenseTypeBack = findViewById(R.id.btnExpenseTypeBack)

        btnTechnique.setOnClickListener {
            startActivity(Intent(this, TechniqueExpenseActivity::class.java))
        }

        btnDocumentation.setOnClickListener {
            startActivity(Intent(this, DocumentationExpenseActivity::class.java))
        }

        btnExpenseTypeBack.setOnClickListener {
            finish()
        }
    }
}