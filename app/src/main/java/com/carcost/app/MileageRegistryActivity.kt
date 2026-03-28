package com.carcost.app

import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MileageRegistryActivity : AppCompatActivity() {

    private lateinit var containerList: LinearLayout
    private lateinit var btnSave: Button
    private lateinit var btnClose: Button

    private val intervalFields = linkedMapOf<String, EditText>()

    private val fixedNodes = listOf(
        "Масло",
        "Масляный фильтр",
        "Воздушный фильтр",
        "Салонный фильтр",
        "Топливный фильтр",
        "Передние колодки",
        "Задние колодки",
        "Привод ГРМ"
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_mileage_registry)

        containerList = findViewById(R.id.containerList)
        btnSave = findViewById(R.id.btnSaveMileageRegistry)
        btnClose = findViewById(R.id.btnCloseMileageRegistry)

        buildFixedRows()
        loadValues()

        btnSave.setOnClickListener {
            saveValues()
        }

        btnClose.setOnClickListener {
            finish()
        }
    }

    private fun buildFixedRows() {
        containerList.removeAllViews()
        intervalFields.clear()

        fixedNodes.forEachIndexed { index, nodeName ->
            val row = LinearLayout(this).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                layoutParams = LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT
                ).apply {
                    if (index > 0) {
                        topMargin = dp(10)
                    }
                }
            }

            val tvNode = TextView(this).apply {
                text = nodeName
                textSize = 16f
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    1.45f
                )
            }

            val etInterval = EditText(this).apply {
                inputType = InputType.TYPE_CLASS_NUMBER
                maxLines = 1
                setSingleLine(true)
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    0.85f
                ).apply {
                    marginStart = dp(10)
                }
            }

            row.addView(tvNode)
            row.addView(etInterval)

            containerList.addView(row)
            intervalFields[nodeName] = etInterval
        }
    }

    private fun loadValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)

        fixedNodes.forEach { nodeName ->
            val savedValue = prefs.getString(makeKey(nodeName), "") ?: ""
            intervalFields[nodeName]?.setText(savedValue)
        }
    }

    private fun saveValues() {
        val prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
        val editor = prefs.edit()

        fixedNodes.forEach { nodeName ->
            val value = intervalFields[nodeName]?.text?.toString()?.trim().orEmpty()
            editor.putString(makeKey(nodeName), value)
        }

        editor.apply()
        Toast.makeText(this, "Протокол сохранён", Toast.LENGTH_SHORT).show()
    }

    private fun makeKey(nodeName: String): String {
        return "interval_$nodeName"
    }

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    companion object {
        private const val PREFS_NAME = "mileage_registry_prefs"
    }
}