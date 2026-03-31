package com.carcost.app

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.ColorDrawable
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.text.NumberFormat
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var cardTopMileage: LinearLayout
    private lateinit var cardTopFuel: LinearLayout
    private lateinit var cardIncome: LinearLayout
    private lateinit var cardCost: LinearLayout
    private lateinit var cardExpense: LinearLayout

    private lateinit var tvMileageValue: TextView
    private lateinit var tvFuelValue: TextView
    private lateinit var tvTopIncomeValue: TextView

    private lateinit var tvIncomeMonthValue: TextView
    private lateinit var tvIncomeYearValue: TextView
    private lateinit var tvExpenseMonthValue: TextView
    private lateinit var tvExpenseYearValue: TextView
    private lateinit var tvCostPerDayValue: TextView
    private lateinit var tvCostPerKmValue: TextView

    private lateinit var containerSoonByDate: LinearLayout
    private lateinit var containerSoonByMileage: LinearLayout

    private lateinit var btnAddIncome: Button
    private lateinit var btnAddExpense: Button

    private val incomeDrafts = mutableListOf<IncomeDraft>()
    private val documentationExpenseDrafts = mutableListOf<DocumentationExpenseDraft>()
    private val techniqueExpenseDrafts = mutableListOf<TechniqueExpenseDraft>()

    private val incomeLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val incomeDraft =
                    result.data?.getSerializableExtra(EXTRA_INCOME_DRAFT) as? IncomeDraft

                if (incomeDraft != null) {
                    incomeDrafts.add(incomeDraft)
                    storage.saveIncomeDrafts(incomeDrafts)
                    updateIncomeValues()
                    updateTopSalaryValue()

                    Toast.makeText(
                        this,
                        getString(
                            R.string.message_income_saved_with_amount,
                            incomeDraft.amount,
                            incomeDrafts.size.toString()
                        ),
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

    private val expenseLauncher =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == RESULT_OK) {
                val documentationDraft =
                    result.data?.getSerializableExtra(EXTRA_DOCUMENTATION_DRAFT) as? DocumentationExpenseDraft
                val techniqueDraft =
                    result.data?.getSerializableExtra(EXTRA_TECHNIQUE_DRAFT) as? TechniqueExpenseDraft

                when {
                    documentationDraft != null -> {
                        documentationExpenseDrafts.add(documentationDraft)
                        storage.saveDocumentationDrafts(documentationExpenseDrafts)
                        updateExpenseValues()
                        updateSoonDateValues()
                        updateTopSalaryValue()
                        updateCostValues()

                        Toast.makeText(
                            this,
                            getString(
                                R.string.message_documentation_saved_with_amount,
                                documentationDraft.amount,
                                documentationExpenseDrafts.size.toString()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }

                    techniqueDraft != null -> {
                        techniqueExpenseDrafts.add(techniqueDraft)
                        storage.saveTechniqueDrafts(techniqueExpenseDrafts)
                        updateAutoStartMileage()
                        updateExpenseValues()
                        updateSoonMileageValues()
                        updateTopSalaryValue()
                        updateCostValues()

                        Toast.makeText(
                            this,
                            getString(
                                R.string.message_technique_saved_with_amount,
                                techniqueDraft.amount,
                                techniqueExpenseDrafts.size.toString()
                            ),
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

    private val exportBackupLauncher =
        registerForActivityResult(ActivityResultContracts.CreateDocument(BACKUP_MIME_TYPE)) { uri ->
            if (uri != null) {
                exportBackupToUri(uri)
            }
        }

    private val importBackupLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {
                confirmImportFromUri(uri)
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        storage = AppStorage(this)

        cardTopMileage = findViewById(R.id.cardTopMileage)
        cardTopFuel = findViewById(R.id.cardTopFuel)
        cardIncome = findViewById(R.id.cardIncome)
        cardCost = findViewById(R.id.cardCost)
        cardExpense = findViewById(R.id.cardExpense)

        tvMileageValue = findViewById(R.id.tvMileageValue)
        tvFuelValue = findViewById(R.id.tvFuelValue)
        tvTopIncomeValue = findViewById(R.id.tvTopIncomeValue)

        tvIncomeMonthValue = findViewById(R.id.tvIncomeMonthValue)
        tvIncomeYearValue = findViewById(R.id.tvIncomeYearValue)
        tvExpenseMonthValue = findViewById(R.id.tvExpenseMonthValue)
        tvExpenseYearValue = findViewById(R.id.tvExpenseYearValue)
        tvCostPerDayValue = findViewById(R.id.tvCostPerDayValue)
        tvCostPerKmValue = findViewById(R.id.tvCostPerKmValue)

        containerSoonByDate = findViewById(R.id.containerSoonByDate)
        containerSoonByMileage = findViewById(R.id.containerSoonByMileage)

        btnAddIncome = findViewById(R.id.btnAddIncome)
        btnAddExpense = findViewById(R.id.btnAddExpense)

        loadStoredData()
        updateAutoStartMileage()

        cardTopMileage.setOnClickListener {
            showMileageInputDialog()
        }

        cardTopMileage.setOnLongClickListener {
            startActivity(Intent(this, MileageRegistryActivity::class.java))
            true
        }

        cardTopFuel.setOnClickListener {
            showFuelInputDialog()
        }

        cardIncome.setOnClickListener {
            showIncomeHistoryDialog()
        }

        cardIncome.setOnLongClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
            true
        }

        cardExpense.setOnClickListener {
            showExpenseHistoryDialog()
        }

        cardExpense.setOnLongClickListener {
            startActivity(Intent(this, CostAnalysisActivity::class.java))
            true
        }

        cardCost.setOnLongClickListener {
            showBackupMenu()
            true
        }

        btnAddIncome.setOnClickListener {
            incomeLauncher.launch(Intent(this, IncomeEntryActivity::class.java))
        }

        btnAddExpense.setOnClickListener {
            expenseLauncher.launch(Intent(this, ExpenseTypeActivity::class.java))
        }

        refreshScreen()
    }

    override fun onResume() {
        super.onResume()
        loadStoredData()
        refreshScreen()
    }

    private fun refreshScreen() {
        updateAutoStartMileage()
        updateIncomeValues()
        updateExpenseValues()
        updateSoonDateValues()
        updateSoonMileageValues()
        updateTopSalaryValue()
        updateCostValues()
    }

    private fun loadStoredData() {
        incomeDrafts.clear()
        incomeDrafts.addAll(storage.loadIncomeDrafts())

        documentationExpenseDrafts.clear()
        documentationExpenseDrafts.addAll(storage.loadDocumentationDrafts())

        techniqueExpenseDrafts.clear()
        techniqueExpenseDrafts.addAll(storage.loadTechniqueDrafts())

        val rawMileage = storage.loadMileage()
        if (rawMileage.isNullOrBlank()) {
            tvMileageValue.text = getString(R.string.mileage_value_format, "0")
        } else {
            rawMileage.toLongOrNull()?.let { mileage ->
                tvMileageValue.text = formatMileage(mileage)
            }
        }

        val fuel = storage.loadFuelPrice()
        tvFuelValue.text = if (fuel.isNullOrBlank()) {
            getString(R.string.fuel_value_format, "0")
        } else {
            getString(R.string.fuel_value_format, fuel)
        }
    }

    private fun showBackupMenu() {
        val titleContainer = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(16), dp(20), dp(0))
        }

        val titleView = TextView(this).apply {
            text = "Резервная копия базы"
            textSize = 18f
            setTypeface(null, Typeface.BOLD)
            setTextColor(Color.parseColor("#FFA500"))
            setPadding(0, 0, 0, dp(8))
        }

        val underlineView = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(1)
            )
            setBackgroundColor(Color.WHITE)
        }

        titleContainer.addView(titleView)
        titleContainer.addView(underlineView)

        val items = arrayOf(
            "Экспорт базы",
            "Импорт базы (полная замена)",
            "Импорт из банка"
        )

        AlertDialog.Builder(this)
            .setCustomTitle(titleContainer)
            .setItems(items) { _, which ->
                when (which) {
                    0 -> exportBackupLauncher.launch(buildBackupFileName())
                    1 -> importBackupLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "text/plain", "*/*"))
                    2 -> startActivity(Intent(this, BankImportActivity::class.java))
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun buildBackupFileName(): String {
        return "CC_Backup_${LocalDateTime.now().format(BACKUP_FILE_DATE_TIME_FORMATTER)}.json"
    }

    private fun exportBackupToUri(uri: Uri) {
        try {
            val backupJson = storage.exportBackupJson()
            contentResolver.openOutputStream(uri)?.bufferedWriter(Charsets.UTF_8).use { writer ->
                if (writer == null) {
                    throw IOException("Не удалось открыть файл для записи")
                }
                writer.write(backupJson)
            }
            Toast.makeText(this, "База сохранена", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось сохранить базу", Toast.LENGTH_SHORT).show()
        }
    }

    private fun confirmImportFromUri(uri: Uri) {
        AlertDialog.Builder(this)
            .setTitle("Импорт базы")
            .setMessage("Все текущие данные будут полностью удалены и заменены данными из выбранного файла.")
            .setPositiveButton("Импорт") { _, _ ->
                importBackupFromUri(uri)
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun importBackupFromUri(uri: Uri) {
        try {
            val backupJson = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8).use { reader ->
                if (reader == null) {
                    throw IOException("Не удалось открыть файл для чтения")
                }
                reader.readText()
            }

            storage.importBackupJson(backupJson)
            loadStoredData()
            refreshScreen()
            Toast.makeText(this, "База восстановлена", Toast.LENGTH_SHORT).show()
        } catch (_: IllegalArgumentException) {
            Toast.makeText(this, "Файл базы не подходит для этой версии", Toast.LENGTH_SHORT).show()
        } catch (_: Exception) {
            Toast.makeText(this, "Не удалось восстановить базу", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateAutoStartMileage() {
        val earliestMileage = techniqueExpenseDrafts
            .asSequence()
            .mapNotNull { draft ->
                val mileage = draft.mileage.trim().toLongOrNull() ?: return@mapNotNull null
                val date = parseDraftDate(draft.date) ?: return@mapNotNull null
                TechniqueStartMileageCandidate(date = date, mileage = mileage)
            }
            .sortedWith(compareBy<TechniqueStartMileageCandidate> { it.date }.thenBy { it.mileage })
            .firstOrNull()

        if (earliestMileage != null) {
            storage.saveStartMileage(earliestMileage.mileage.toString())
        } else {
            storage.clearStartMileage()
        }
    }

    private fun updateIncomeValues() {
        val today = LocalDate.now()

        val monthTotal = incomeDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val yearTotal = incomeDrafts
            .filter { parseDraftDate(it.date)?.year == today.year }
            .sumOf { parseAmount(it.amount) }

        tvIncomeMonthValue.text = formatMoney(monthTotal)
        tvIncomeYearValue.text = formatMoney(yearTotal)
    }

    private fun updateExpenseValues() {
        val today = LocalDate.now()

        val documentationMonth = documentationExpenseDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val documentationYear = documentationExpenseDrafts
            .filter { parseDraftDate(it.date)?.year == today.year }
            .sumOf { parseAmount(it.amount) }

        val techniqueMonth = techniqueExpenseDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val techniqueYear = techniqueExpenseDrafts
            .filter { parseDraftDate(it.date)?.year == today.year }
            .sumOf { parseAmount(it.amount) }

        val monthTotal = documentationMonth + techniqueMonth
        val yearTotal = documentationYear + techniqueYear

        tvExpenseMonthValue.text = formatMoney(monthTotal)
        tvExpenseYearValue.text = formatMoney(yearTotal)
    }

    private fun updateTopSalaryValue() {
        val today = LocalDate.now()

        val incomeMonth = incomeDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val documentationMonth = documentationExpenseDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val techniqueMonth = techniqueExpenseDrafts
            .filter { parseDraftDate(it.date)?.let { date -> isSameMonth(date, today) } == true }
            .sumOf { parseAmount(it.amount) }

        val salaryMonth = incomeMonth - documentationMonth - techniqueMonth
        tvTopIncomeValue.text = formatMoney(salaryMonth)
    }

    private fun updateCostValues() {
        val today = LocalDate.now()
        val fromDate = today.minusDays(365)

        val allOdometerPoints = buildOdometerPoints()
        if (allOdometerPoints.size < 2) {
            tvCostPerDayValue.text = getString(R.string.cost_no_data)
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val windowSegments = buildWindowMileageSegments(
            points = allOdometerPoints,
            fromDate = fromDate,
            toDate = today
        )

        if (windowSegments.isEmpty()) {
            tvCostPerDayValue.text = getString(R.string.cost_no_data)
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val averageDailyMileage = windowSegments.map { it.dailyMileage }.average()
        if (averageDailyMileage <= 0.0) {
            tvCostPerDayValue.text = getString(R.string.cost_no_data)
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val periodDays = ChronoUnit.DAYS.between(fromDate, today).toInt().coerceAtLeast(1)

        val nonFuelExpenses = buildNonFuelExpenseItems(fromDate, today)
        val nonFuelDailyCost = nonFuelExpenses.sumOf { it.amount } / periodDays.toDouble()

        val averageFuelConsumptionPer100 = buildAverageFuelConsumptionPer100(windowSegments)
        val currentFuelPrice = storage.loadFuelPrice()?.replace(',', '.')?.toDoubleOrNull() ?: 0.0
        val fuelCostPerKm = if (averageFuelConsumptionPer100 != null && currentFuelPrice > 0.0) {
            (averageFuelConsumptionPer100 / 100.0) * currentFuelPrice
        } else {
            0.0
        }

        val nonFuelCostPerKm = nonFuelDailyCost / averageDailyMileage
        val totalCostPerKm = nonFuelCostPerKm + fuelCostPerKm
        val totalCostPerDay = nonFuelDailyCost + (fuelCostPerKm * averageDailyMileage)

        tvCostPerDayValue.text = "${formatMoney(totalCostPerDay)} / сутки"
        tvCostPerKmValue.text = "${formatMoney(totalCostPerKm)} / км"
    }

    private fun showIncomeHistoryDialog() {
        val lines = buildIncomeHistoryLines()
        showHistoryDialog("История доходов", lines)
    }

    private fun showExpenseHistoryDialog() {
        val lines = buildExpenseHistoryLines()
        showHistoryDialog("История расходов", lines)
    }

    private fun showHistoryDialog(title: String, lines: List<String>) {
        if (lines.isEmpty()) {
            Toast.makeText(this, "Нет данных для истории", Toast.LENGTH_SHORT).show()
            return
        }

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_list_item_1,
            lines
        )

        AlertDialog.Builder(this)
            .setTitle(title)
            .setAdapter(adapter, null)
            .setNegativeButton("Закрыть", null)
            .show()
    }

    private fun buildIncomeHistoryLines(): List<String> {
        val grouped = incomeDrafts
            .mapNotNull { draft ->
                val date = parseDraftDate(draft.date) ?: return@mapNotNull null
                YearMonth.of(date.year, date.monthValue) to parseAmount(draft.amount)
            }
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sum() }

        return buildHistoryLines(grouped)
    }

    private fun buildExpenseHistoryLines(): List<String> {
        val documentation = documentationExpenseDrafts.mapNotNull { draft ->
            val date = parseDraftDate(draft.date) ?: return@mapNotNull null
            YearMonth.of(date.year, date.monthValue) to parseAmount(draft.amount)
        }

        val technique = techniqueExpenseDrafts.mapNotNull { draft ->
            val date = parseDraftDate(draft.date) ?: return@mapNotNull null
            YearMonth.of(date.year, date.monthValue) to parseAmount(draft.amount)
        }

        val grouped = (documentation + technique)
            .groupBy({ it.first }, { it.second })
            .mapValues { entry -> entry.value.sum() }

        return buildHistoryLines(grouped)
    }

    private fun buildHistoryLines(monthMap: Map<YearMonth, Double>): List<String> {
        if (monthMap.isEmpty()) return emptyList()

        val years = monthMap.keys.map { it.year }.distinct().sortedDescending()
        val result = mutableListOf<String>()

        years.forEach { year ->
            val months = monthMap.keys
                .filter { it.year == year }
                .sortedByDescending { it.monthValue }

            months.forEach { yearMonth ->
                val amount = monthMap[yearMonth] ?: 0.0
                result.add("${yearMonth.monthValue} — ${formatMoney(amount)}")
            }

            val yearTotal = months.sumOf { yearMonth -> monthMap[yearMonth] ?: 0.0 }
            result.add("$year — ${formatMoney(yearTotal)}")
        }

        return result
    }

    private fun buildOdometerPoints(): List<OdometerPoint> {
        val points = mutableListOf<OdometerPoint>()

        documentationExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            val mileage = draft.odometer.trim().toLongOrNull() ?: return@forEach
            if (mileage > 0L) {
                points.add(OdometerPoint(date = date, mileage = mileage))
            }
        }

        techniqueExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            val mileage = draft.mileage.trim().toLongOrNull() ?: return@forEach
            if (mileage > 0L) {
                points.add(OdometerPoint(date = date, mileage = mileage))
            }
        }

        val currentMileage = extractDigits(tvMileageValue.text.toString()).toLongOrNull()
        if (currentMileage != null && currentMileage > 0L) {
            points.add(OdometerPoint(date = LocalDate.now(), mileage = currentMileage))
        }

        return points
            .distinctBy { "${it.date}|${it.mileage}" }
            .sortedWith(compareBy<OdometerPoint> { it.date }.thenBy { it.mileage })
    }

    private fun buildWindowMileageSegments(
        points: List<OdometerPoint>,
        fromDate: LocalDate,
        toDate: LocalDate
    ): List<WindowMileageSegment> {
        if (points.size < 2) return emptyList()

        val result = mutableListOf<WindowMileageSegment>()

        for (i in 0 until points.lastIndex) {
            val start = points[i]
            val end = points[i + 1]

            val totalDays = ChronoUnit.DAYS.between(start.date, end.date).toInt()
            val totalMileage = end.mileage - start.mileage

            if (totalDays <= 0 || totalMileage <= 0L) continue

            val overlapStart = if (start.date.isBefore(fromDate)) fromDate else start.date
            val overlapEnd = if (end.date.isAfter(toDate)) toDate else end.date

            val overlapDays = ChronoUnit.DAYS.between(overlapStart, overlapEnd).toInt()
            if (overlapDays <= 0) continue

            val dailyMileage = totalMileage.toDouble() / totalDays.toDouble()
            val overlapMileage = dailyMileage * overlapDays.toDouble()

            result.add(
                WindowMileageSegment(
                    startDate = overlapStart,
                    endDate = overlapEnd,
                    mileageDelta = overlapMileage,
                    days = overlapDays,
                    dailyMileage = dailyMileage
                )
            )
        }

        return result
    }

    private fun buildNonFuelExpenseItems(
        startDate: LocalDate,
        endDate: LocalDate
    ): List<ExpenseItem> {
        val result = mutableListOf<ExpenseItem>()

        documentationExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            if (date.isBefore(startDate) || date.isAfter(endDate)) return@forEach
            val amount = parseAmount(draft.amount)
            result.add(ExpenseItem(date = date, amount = amount))
        }

        techniqueExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            if (date.isBefore(startDate) || date.isAfter(endDate)) return@forEach
            if (isFuelDraft(draft)) return@forEach
            val amount = parseAmount(draft.amount)
            result.add(ExpenseItem(date = date, amount = amount))
        }

        return result
    }

    private fun buildAverageFuelConsumptionPer100(segments: List<WindowMileageSegment>): Double? {
        val consumptions = segments.mapNotNull { segment ->
            val liters = techniqueExpenseDrafts
                .asSequence()
                .filter { isFuelDraft(it) }
                .filter { draft ->
                    val date = parseDraftDate(draft.date) ?: return@filter false
                    !date.isBefore(segment.startDate) && !date.isAfter(segment.endDate)
                }
                .sumOf { parseQuantity(it.quantity) }

            if (liters <= 0.0 || segment.mileageDelta <= 0.0) {
                null
            } else {
                (liters / segment.mileageDelta) * 100.0
            }
        }

        return if (consumptions.isEmpty()) null else consumptions.average()
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

    private fun updateSoonDateValues() {
        containerSoonByDate.removeAllViews()

        val today = LocalDate.now()

        val items = documentationExpenseDrafts
            .mapNotNull { draft ->
                val validUntil = draft.validUntil ?: return@mapNotNull null
                val validDate = parseDraftDate(validUntil) ?: return@mapNotNull null
                if (validDate.isBefore(today)) return@mapNotNull null

                SoonDateItem(
                    title = buildDocumentationTitle(draft),
                    targetDate = validDate,
                    draft = draft
                )
            }
            .sortedBy { it.targetDate }

        if (items.isEmpty()) {
            addSoonPlaceholder(containerSoonByDate, getString(R.string.soon_no_data))
            return
        }

        items.forEach { item ->
            val daysLeft = ChronoUnit.DAYS.between(today, item.targetDate).toInt()
            addSoonDateRow(
                title = item.title,
                value = formatRemainingDate(daysLeft),
                exactDate = item.targetDate,
                draft = item.draft
            )
        }
    }

    private fun updateSoonMileageValues() {
        containerSoonByMileage.removeAllViews()

        val currentMileage = extractDigits(tvMileageValue.text.toString()).toLongOrNull()
        if (currentMileage == null) {
            addSoonPlaceholder(containerSoonByMileage, getString(R.string.soon_no_mileage))
            return
        }

        val prefs = getSharedPreferences(MILEAGE_PREFS_NAME, MODE_PRIVATE)

        val items = FIXED_MILEAGE_NODES.mapNotNull { nodeName ->
            val intervalValue = prefs.getString(makeMileageKey(nodeName), "")?.trim().orEmpty()
            val interval = intervalValue.toLongOrNull() ?: return@mapNotNull null
            if (interval <= 0L) return@mapNotNull null

            val latestReplacement = findLatestReplacementForNode(nodeName) ?: return@mapNotNull null
            val installMileage = latestReplacement.mileage.toLongOrNull() ?: return@mapNotNull null

            val remaining = interval - (currentMileage - installMileage)
            if (remaining < 0L) return@mapNotNull null

            SoonMileageItem(
                title = nodeName,
                remainingKm = remaining,
                draft = latestReplacement
            )
        }.sortedBy { it.remainingKm }

        if (items.isEmpty()) {
            addSoonPlaceholder(containerSoonByMileage, getString(R.string.soon_mileage_no_data))
            return
        }

        items.forEach { item ->
            addSoonMileageRow(
                title = item.title,
                value = formatRemainingMileage(item.remainingKm),
                draft = item.draft
            )
        }
    }

    private fun findLatestReplacementForNode(nodeName: String): TechniqueExpenseDraft? {
        return techniqueExpenseDrafts
            .asSequence()
            .filter { it.recordType == TECHNIQUE_REPLACEMENT_TYPE }
            .filter { draft -> draft.titles.any { it.equals(nodeName, ignoreCase = true) } }
            .mapNotNull { draft ->
                val date = parseDraftDate(draft.date) ?: return@mapNotNull null
                TechniqueDraftWithDate(draft, date)
            }
            .sortedByDescending { it.date }
            .map { it.draft }
            .firstOrNull()
    }

    private fun buildDocumentationTitle(draft: DocumentationExpenseDraft): String {
        val type = draft.type.trim()
        val title = draft.title.trim()

        return when {
            type.isNotEmpty() && title.isNotEmpty() -> "$type $title"
            type.isNotEmpty() -> type
            title.isNotEmpty() -> title
            else -> "Без названия"
        }
    }

    private fun addSoonPlaceholder(container: LinearLayout, text: String) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(Color.BLACK)
        }
        container.addView(textView)
    }

    private fun addSoonDateRow(
        title: String,
        value: String,
        exactDate: LocalDate,
        draft: DocumentationExpenseDraft
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            isClickable = true
            isFocusable = true
            setOnClickListener {
                showSoonDatePopup(this, formatExactSoonDate(exactDate))
            }
            setOnLongClickListener {
                openDocumentationForEdit(draft)
                true
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.BLACK)
        }

        row.addView(titleView)
        row.addView(valueView)
        containerSoonByDate.addView(row)
    }

    private fun addSoonMileageRow(
        title: String,
        value: String,
        draft: TechniqueExpenseDraft
    ) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
            isClickable = true
            isFocusable = true
            setOnLongClickListener {
                openTechniqueForEdit(draft)
                true
            }
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.BLACK)
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(Color.BLACK)
        }

        row.addView(titleView)
        row.addView(valueView)
        containerSoonByMileage.addView(row)
    }

    private fun openDocumentationForEdit(draft: DocumentationExpenseDraft) {
        val intent = Intent(this, DocumentationExpenseActivity::class.java).apply {
            putExtra(DocumentationExpenseActivity.EXTRA_EDIT_MODE, true)
            putExtra(DocumentationExpenseActivity.EXTRA_ORIGINAL_DRAFT, draft)
        }
        startActivity(intent)
    }

    private fun openTechniqueForEdit(draft: TechniqueExpenseDraft) {
        val intent = Intent(this, TechniqueExpenseActivity::class.java).apply {
            putExtra(TechniqueExpenseActivity.EXTRA_EDIT_MODE, true)
            putExtra(TechniqueExpenseActivity.EXTRA_ORIGINAL_DRAFT, draft)
        }
        startActivity(intent)
    }

    private fun showSoonDatePopup(anchor: View, text: String) {
        val popupText = TextView(this).apply {
            this.text = text
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(10), dp(6), dp(10), dp(6))
            background = ColorDrawable(Color.parseColor("#CC000000"))
        }

        popupText.measure(
            View.MeasureSpec.UNSPECIFIED,
            View.MeasureSpec.UNSPECIFIED
        )

        val popup = PopupWindow(
            popupText,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT,
            false
        ).apply {
            isOutsideTouchable = true
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(4).toFloat()
        }

        val popupWidth = popupText.measuredWidth
        val popupHeight = popupText.measuredHeight
        val xOff = (anchor.width - popupWidth) / 2
        val yOff = -(anchor.height + popupHeight + dp(4))

        popup.showAsDropDown(anchor, xOff, yOff, Gravity.START)
        anchor.postDelayed({
            if (popup.isShowing) {
                popup.dismiss()
            }
        }, 1800)
    }

    private fun formatRemainingDate(daysLeft: Int): String {
        if (daysLeft < 0) return getString(R.string.soon_expired)
        if (daysLeft == 0) return getString(R.string.soon_today)

        if (daysLeft <= 99) {
            return "$daysLeft ${daysWord(daysLeft)}"
        }

        if (daysLeft >= 366) {
            val years = daysLeft / 365
            return when (years) {
                1 -> getString(R.string.soon_more_than_year)
                2 -> getString(R.string.soon_more_than_two_years)
                3 -> getString(R.string.soon_more_than_three_years)
                else -> getString(R.string.soon_more_than_years, years.toString())
            }
        }

        val months = (daysLeft / 30).coerceIn(4, 11)
        return getString(R.string.soon_more_than_months, months.toString())
    }

    private fun formatExactSoonDate(date: LocalDate): String {
        val monthName = date.month.getDisplayName(TextStyle.FULL_STANDALONE, RUSSIAN_LOCALE)
        return buildString {
            append(date.dayOfMonth.toString().padStart(2, '0'))
            append(' ')
            append(monthName)
            append(' ')
            append(date.year)
        }
    }

    private fun formatRemainingMileage(remainingKm: Long): String {
        return when {
            remainingKm < 0L -> "Просрочено на ${formatKmValue(-remainingKm)} км"
            remainingKm == 0L -> "Сейчас"
            else -> "Осталось ${formatKmValue(remainingKm)} км"
        }
    }

    private fun formatKmValue(value: Long): String {
        val formatter = NumberFormat.getInstance(Locale("ru"))
        return formatter.format(value)
    }

    private fun daysWord(value: Int): String {
        val rem10 = value % 10
        val rem100 = value % 100
        return when {
            rem100 in 11..14 -> "дней"
            rem10 == 1 -> "день"
            rem10 in 2..4 -> "дня"
            else -> "дней"
        }
    }

    private fun isSameMonth(first: LocalDate, second: LocalDate): Boolean {
        return first.year == second.year && first.monthValue == second.monthValue
    }

    private fun parseDraftDate(value: String): LocalDate? {
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
                    storage.saveMileage(number.toString())
                    updateSoonMileageValues()
                    updateCostValues()
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
                    storage.saveFuelPrice(value)
                    updateCostValues()
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

    private fun dp(value: Int): Int {
        return (value * resources.displayMetrics.density).toInt()
    }

    private fun makeMileageKey(nodeName: String): String {
        return "interval_$nodeName"
    }

    companion object {
        const val EXTRA_INCOME_DRAFT = "extra_income_draft"
        const val EXTRA_DOCUMENTATION_DRAFT = "extra_documentation_draft"
        const val EXTRA_TECHNIQUE_DRAFT = "extra_technique_draft"

        private const val MILEAGE_PREFS_NAME = "mileage_registry_prefs"
        private const val TECHNIQUE_REPLACEMENT_TYPE = "Установка / замена"
        private const val BACKUP_MIME_TYPE = "application/json"

        private val DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd.MM.uuuu")
                .withResolverStyle(ResolverStyle.STRICT)

        private val BACKUP_FILE_DATE_TIME_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("dd-MM_HH_mm")

        private val RUSSIAN_LOCALE = Locale("ru")

        private val FIXED_MILEAGE_NODES = listOf(
            "Масло",
            "Масляный фильтр",
            "Воздушный фильтр",
            "Салонный фильтр",
            "Топливный фильтр",
            "Передние колодки",
            "Задние колодки",
            "Привод ГРМ"
        )
    }

    private data class SoonDateItem(
        val title: String,
        val targetDate: LocalDate,
        val draft: DocumentationExpenseDraft
    )

    private data class SoonMileageItem(
        val title: String,
        val remainingKm: Long,
        val draft: TechniqueExpenseDraft
    )

    private data class TechniqueDraftWithDate(
        val draft: TechniqueExpenseDraft,
        val date: LocalDate
    )

    private data class TechniqueStartMileageCandidate(
        val date: LocalDate,
        val mileage: Long
    )

    private data class ExpenseItem(
        val date: LocalDate,
        val amount: Double
    )

    private data class OdometerPoint(
        val date: LocalDate,
        val mileage: Long
    )

    private data class WindowMileageSegment(
        val startDate: LocalDate,
        val endDate: LocalDate,
        val mileageDelta: Double,
        val days: Int,
        val dailyMileage: Double
    )
}