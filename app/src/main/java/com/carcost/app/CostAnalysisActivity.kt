package com.carcost.app

import android.app.DatePickerDialog
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Calendar
import java.util.Locale

class CostAnalysisActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var etDateFrom: EditText
    private lateinit var etDateTo: EditText
    private lateinit var btnApplyPeriod: Button

    private lateinit var tvPeriodValue: TextView
    private lateinit var tvPointCountValue: TextView
    private lateinit var tvMileageValue: TextView
    private lateinit var tvAverageMileageDayValue: TextView

    private lateinit var tvExpenseTotalValue: TextView
    private lateinit var tvExpensePerDayValue: TextView
    private lateinit var tvTechniqueExpenseValue: TextView
    private lateinit var tvDocumentationExpenseValue: TextView
    private lateinit var tvFuelExpenseValue: TextView

    private lateinit var tvFuelLitersValue: TextView
    private lateinit var tvFuelPriceAvgValue: TextView
    private lateinit var tvFuelConsumptionValue: TextView
    private lateinit var tvFuelCostPerKmValue: TextView

    private lateinit var tvTotalCostPerKmValue: TextView
    private lateinit var tvTotalCostPerDayValue: TextView

    private val documentationExpenseDrafts = mutableListOf<DocumentationExpenseDraft>()
    private val techniqueExpenseDrafts = mutableListOf<TechniqueExpenseDraft>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_cost_analysis)

        storage = AppStorage(this)

        etDateFrom = findViewById(R.id.etDateFrom)
        etDateTo = findViewById(R.id.etDateTo)
        btnApplyPeriod = findViewById(R.id.btnApplyPeriod)

        tvPeriodValue = findViewById(R.id.tvPeriodValue)
        tvPointCountValue = findViewById(R.id.tvPointCountValue)
        tvMileageValue = findViewById(R.id.tvMileageValue)
        tvAverageMileageDayValue = findViewById(R.id.tvAverageMileageDayValue)

        tvExpenseTotalValue = findViewById(R.id.tvExpenseTotalValue)
        tvExpensePerDayValue = findViewById(R.id.tvExpensePerDayValue)
        tvTechniqueExpenseValue = findViewById(R.id.tvTechniqueExpenseValue)
        tvDocumentationExpenseValue = findViewById(R.id.tvDocumentationExpenseValue)
        tvFuelExpenseValue = findViewById(R.id.tvFuelExpenseValue)

        tvFuelLitersValue = findViewById(R.id.tvFuelLitersValue)
        tvFuelPriceAvgValue = findViewById(R.id.tvFuelPriceAvgValue)
        tvFuelConsumptionValue = findViewById(R.id.tvFuelConsumptionValue)
        tvFuelCostPerKmValue = findViewById(R.id.tvFuelCostPerKmValue)

        tvTotalCostPerKmValue = findViewById(R.id.tvTotalCostPerKmValue)
        tvTotalCostPerDayValue = findViewById(R.id.tvTotalCostPerDayValue)

        setupDateField(etDateFrom)
        setupDateField(etDateTo)

        loadStoredData()
        fillDefaultPeriod()
        recalculate()

        btnApplyPeriod.setOnClickListener {
            recalculate()
        }
    }

    override fun onResume() {
        super.onResume()
        loadStoredData()
        recalculate()
    }

    private fun loadStoredData() {
        documentationExpenseDrafts.clear()
        documentationExpenseDrafts.addAll(storage.loadDocumentationDrafts())

        techniqueExpenseDrafts.clear()
        techniqueExpenseDrafts.addAll(storage.loadTechniqueDrafts())
    }

    private fun fillDefaultPeriod() {
        val odometerPoints = buildOdometerPoints()
        val today = LocalDate.now()

        val startDate = odometerPoints.firstOrNull()?.date ?: today.minusDays(365)
        etDateFrom.setText(startDate.format(DATE_FORMATTER))
        etDateTo.setText(today.format(DATE_FORMATTER))
    }

    private fun setupDateField(editText: EditText) {
        editText.keyListener = null
        editText.isFocusable = false
        editText.isClickable = true
        editText.inputType = InputType.TYPE_NULL

        editText.setOnClickListener {
            showDatePicker(editText)
        }
    }

    private fun showDatePicker(target: EditText) {
        val parsedDate = parseDate(target.text.toString().trim()) ?: LocalDate.now()
        val calendar = Calendar.getInstance().apply {
            set(parsedDate.year, parsedDate.monthValue - 1, parsedDate.dayOfMonth)
        }

        val dialog = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                val date = LocalDate.of(year, month + 1, dayOfMonth)
                target.setText(date.format(DATE_FORMATTER))
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    private fun recalculate() {
        val dateFrom = parseDate(etDateFrom.text.toString().trim())
        val dateTo = parseDate(etDateTo.text.toString().trim())

        if (dateFrom == null || dateTo == null || dateFrom.isAfter(dateTo)) {
            showNoData()
            return
        }

        val filteredPoints = buildOdometerPoints()
            .filter { !it.date.isBefore(dateFrom) && !it.date.isAfter(dateTo) }

        if (filteredPoints.size < 2) {
            showNoData()
            tvPeriodValue.text = "${dateFrom.format(DATE_FORMATTER)} — ${dateTo.format(DATE_FORMATTER)}"
            return
        }

        val segments = buildMileageSegments(filteredPoints)
        if (segments.isEmpty()) {
            showNoData()
            tvPeriodValue.text = "${dateFrom.format(DATE_FORMATTER)} — ${dateTo.format(DATE_FORMATTER)}"
            return
        }

        val averageDailyMileage = segments.map { it.dailyMileage }.average()
        val totalMileage = filteredPoints.last().mileage - filteredPoints.first().mileage
        val periodDays = ChronoUnit.DAYS.between(filteredPoints.first().date, filteredPoints.last().date)
            .toInt()
            .coerceAtLeast(1)

        val documentationExpenses = documentationExpenseDrafts
            .filter { draft ->
                val date = parseDate(draft.date) ?: return@filter false
                !date.isBefore(dateFrom) && !date.isAfter(dateTo)
            }
            .sumOf { parseAmount(it.amount) }

        val techniqueExpenses = techniqueExpenseDrafts
            .filter { draft ->
                val date = parseDate(draft.date) ?: return@filter false
                !date.isBefore(dateFrom) && !date.isAfter(dateTo) && !isFuelDraft(draft)
            }
            .sumOf { parseAmount(it.amount) }

        val fuelDrafts = techniqueExpenseDrafts
            .filter { draft ->
                val date = parseDate(draft.date) ?: return@filter false
                !date.isBefore(dateFrom) && !date.isAfter(dateTo) && isFuelDraft(draft)
            }

        val fuelExpense = fuelDrafts.sumOf { parseAmount(it.amount) }
        val fuelLiters = fuelDrafts.sumOf { parseQuantity(it.quantity) }
        val totalExpenses = documentationExpenses + techniqueExpenses + fuelExpense
        val expensePerDay = totalExpenses / periodDays.toDouble()
        val averageFuelPrice = if (fuelLiters > 0.0) fuelExpense / fuelLiters else 0.0

        val fuelConsumptions = segments.mapNotNull { segment ->
            val liters = fuelDrafts
                .filter { draft ->
                    val date = parseDate(draft.date) ?: return@filter false
                    !date.isBefore(segment.startDate) && !date.isAfter(segment.endDate)
                }
                .sumOf { parseQuantity(it.quantity) }

            if (liters <= 0.0 || segment.mileageDelta <= 0L) {
                null
            } else {
                (liters / segment.mileageDelta.toDouble()) * 100.0
            }
        }

        val averageFuelConsumption = if (fuelConsumptions.isEmpty()) 0.0 else fuelConsumptions.average()
        val currentFuelPrice = storage.loadFuelPrice()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        val fuelCostPerKm = if (averageFuelConsumption > 0.0 && currentFuelPrice > 0.0) {
            (averageFuelConsumption / 100.0) * currentFuelPrice
        } else {
            0.0
        }

        val nonFuelPerDay = (documentationExpenses + techniqueExpenses) / periodDays.toDouble()
        val nonFuelCostPerKm = if (averageDailyMileage > 0.0) {
            nonFuelPerDay / averageDailyMileage
        } else {
            0.0
        }

        val totalCostPerKm = nonFuelCostPerKm + fuelCostPerKm
        val totalCostPerDay = nonFuelPerDay + (fuelCostPerKm * averageDailyMileage)

        tvPeriodValue.text = "${dateFrom.format(DATE_FORMATTER)} — ${dateTo.format(DATE_FORMATTER)}"
        tvPointCountValue.text = "${filteredPoints.size} точек / ${segments.size} промежутков"
        tvMileageValue.text = "${formatKm(totalMileage)} км"
        tvAverageMileageDayValue.text = "${formatNumber(averageDailyMileage)} км/день"

        tvExpenseTotalValue.text = formatMoney(totalExpenses)
        tvExpensePerDayValue.text = formatMoney(expensePerDay)
        tvTechniqueExpenseValue.text = formatMoney(techniqueExpenses)
        tvDocumentationExpenseValue.text = formatMoney(documentationExpenses)
        tvFuelExpenseValue.text = formatMoney(fuelExpense)

        tvFuelLitersValue.text = if (fuelLiters > 0.0) "${formatNumber(fuelLiters)} л" else "—"
        tvFuelPriceAvgValue.text = if (averageFuelPrice > 0.0) "${formatMoney(averageFuelPrice)} / л" else "—"
        tvFuelConsumptionValue.text = if (averageFuelConsumption > 0.0) "${formatNumber(averageFuelConsumption)} л/100 км" else "—"
        tvFuelCostPerKmValue.text = if (fuelCostPerKm > 0.0) "${formatMoney(fuelCostPerKm)} / км" else "—"

        tvTotalCostPerKmValue.text = "${formatMoney(totalCostPerKm)} / км"
        tvTotalCostPerDayValue.text = "${formatMoney(totalCostPerDay)} / сутки"
    }

    private fun buildOdometerPoints(): List<OdometerPoint> {
        val points = mutableListOf<OdometerPoint>()

        documentationExpenseDrafts.forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            val mileage = draft.odometer.trim().toLongOrNull() ?: return@forEach
            if (mileage > 0L) {
                points.add(OdometerPoint(date = date, mileage = mileage))
            }
        }

        techniqueExpenseDrafts.forEach { draft ->
            val date = parseDate(draft.date) ?: return@forEach
            val mileage = draft.mileage.trim().toLongOrNull() ?: return@forEach
            if (mileage > 0L) {
                points.add(OdometerPoint(date = date, mileage = mileage))
            }
        }

        val currentMileage = storage.loadMileage()?.toLongOrNull()
        if (currentMileage != null && currentMileage > 0L) {
            points.add(OdometerPoint(date = LocalDate.now(), mileage = currentMileage))
        }

        return points
            .distinctBy { "${it.date}|${it.mileage}" }
            .sortedWith(compareBy<OdometerPoint> { it.date }.thenBy { it.mileage })
    }

    private fun buildMileageSegments(points: List<OdometerPoint>): List<MileageSegment> {
        if (points.size < 2) return emptyList()

        val segments = mutableListOf<MileageSegment>()
        for (i in 0 until points.lastIndex) {
            val start = points[i]
            val end = points[i + 1]
            val days = ChronoUnit.DAYS.between(start.date, end.date).toInt()
            val kmDelta = end.mileage - start.mileage

            if (days <= 0 || kmDelta <= 0L) continue

            segments.add(
                MileageSegment(
                    startDate = start.date,
                    endDate = end.date,
                    mileageDelta = kmDelta,
                    days = days,
                    dailyMileage = kmDelta.toDouble() / days.toDouble()
                )
            )
        }

        return segments
    }

    private fun isFuelDraft(draft: TechniqueExpenseDraft): Boolean {
        if (!draft.quantityUnit.equals("л", ignoreCase = true)) return false

        return draft.titles.any { title ->
            val normalized = title.trim().lowercase(Locale.getDefault())
            normalized == "топливо" ||
                    normalized.contains("топлив") ||
                    normalized.contains("fuel") ||
                    normalized.contains("neste")
        }
    }

    private fun parseDate(value: String): LocalDate? {
        return try {
            LocalDate.parse(value, DATE_FORMATTER)
        } catch (_: Exception) {
            null
        }
    }

    private fun parseAmount(value: String): Double {
        return value.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun parseQuantity(value: String): Double {
        return value.replace(" ", "").replace(",", ".").toDoubleOrNull() ?: 0.0
    }

    private fun formatMoney(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("ru"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return "${formatter.format(value)} €"
    }

    private fun formatNumber(value: Double): String {
        val formatter = NumberFormat.getNumberInstance(Locale("ru"))
        formatter.minimumFractionDigits = 0
        formatter.maximumFractionDigits = 2
        return formatter.format(value)
    }

    private fun formatKm(value: Long): String {
        val formatter = NumberFormat.getIntegerInstance(Locale("ru"))
        return formatter.format(value)
    }

    private fun showNoData() {
        tvPointCountValue.text = "Недостаточно данных"
        tvMileageValue.text = "—"
        tvAverageMileageDayValue.text = "—"
        tvExpenseTotalValue.text = "—"
        tvExpensePerDayValue.text = "—"
        tvTechniqueExpenseValue.text = "—"
        tvDocumentationExpenseValue.text = "—"
        tvFuelExpenseValue.text = "—"
        tvFuelLitersValue.text = "—"
        tvFuelPriceAvgValue.text = "—"
        tvFuelConsumptionValue.text = "—"
        tvFuelCostPerKmValue.text = "—"
        tvTotalCostPerKmValue.text = "—"
        tvTotalCostPerDayValue.text = "—"
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT)
    }

    private data class OdometerPoint(
        val date: LocalDate,
        val mileage: Long
    )

    private data class MileageSegment(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val mileageDelta: Long,
        val days: Int,
        val dailyMileage: Double
    )
}