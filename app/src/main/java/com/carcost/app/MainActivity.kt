package com.carcost.app

import android.content.Intent
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var cardTopMileage: LinearLayout
    private lateinit var cardTopFuel: LinearLayout

    private lateinit var tvMileageValue: TextView
    private lateinit var tvFuelValue: TextView

    private lateinit var btnAddIncome: Button
    private lateinit var btnAddExpense: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        cardTopMileage = findViewById(R.id.cardTopMileage)
        cardTopFuel = findViewById(R.id.cardTopFuel)

        tvMileageValue = findViewById(R.id.tvMileageValue)
        tvFuelValue = findViewById(R.id.tvFuelValue)

        btnAddIncome = findViewById(R.id.btnAddIncome)
        btnAddExpense = findViewById(R.id.btnAddExpense)

        cardTopMileage.setOnClickListener {
            showMileageInputDialog()
        }

        cardTopFuel.setOnClickListener {
            showFuelInputDialog()
        }

        btnAddIncome.setOnClickListener {
            startActivity(Intent(this, IncomeEntryActivity::class.java))
        }

        btnAddExpense.setOnClickListener {
            startActivity(Intent(this, ExpenseTypeActivity::class.java))
        }
    }

    private fun showMileageInputDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER
            setText(extractDigits(tvMileageValue.text.toString()))
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_mileage)
            .setView(input)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val value = input.text.toString().trim()
                if (value.isNotEmpty()) {
                    val number = value.toLong()
                    tvMileageValue.text = formatMileage(number)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun showFuelInputDialog() {
        val input = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
            setText(extractFuel(tvFuelValue.text.toString()))
            setSelection(text.length)
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.dialog_title_fuel_price)
            .setView(input)
            .setPositiveButton(R.string.dialog_ok) { _, _ ->
                val value = input.text.toString()
                    .trim()
                    .replace(',', '.')

                if (value.isNotEmpty()) {
                    tvFuelValue.text = getString(R.string.fuel_value_format, value)
                }
            }
            .setNegativeButton(R.string.dialog_cancel, null)
            .show()
    }

    private fun extractDigits(text: String): String {
        return text.filter { it.isDigit() }
    }

    private fun extractFuel(text: String): String {
        return text.replace("€", "").trim()
    }

    private fun formatMileage(value: Long): String {
        val formatter = NumberFormat.getInstance(Locale("ru"))
        return getString(R.string.mileage_value_format, formatter.format(value))
    }
}