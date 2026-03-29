package com.carcost.app

import android.content.Intent
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.text.InputType
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import java.io.IOException
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.ResolverStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var storage: AppStorage

    private lateinit var cardTopMileage: LinearLayout
    private lateinit var cardTopFuel: LinearLayout
    private lateinit var cardIncome: LinearLayout
    private lateinit var cardCost: LinearLayout

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

        cardIncome.setOnLongClickListener {
            startActivity(Intent(this, JournalActivity::class.java))
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
        val items = arrayOf(
            "Экспорт базы",
            "Импорт базы (полная замена)"
        )

        AlertDialog.Builder(this)
            .setTitle("Резервная копия базы")
            .setItems(items) { _, which ->
                when (which) {
                    0 -> exportBackupLauncher.launch(buildBackupFileName())
                    1 -> importBackupLauncher.launch(arrayOf(BACKUP_MIME_TYPE, "text/plain", "*/*"))
                }
            }
            .setNegativeButton("Отмена", null)
            .show()
    }

    private fun buildBackupFileName(): String {
        return "carcost_backup_${LocalDate.now().format(BACKUP_FILE_DATE_FORMATTER)}.json"
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
        val allExpenses = buildAllExpenseItems()

        if (allExpenses.isEmpty()) {
            tvCostPerDayValue.text = getString(R.string.cost_no_data)
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val today = LocalDate.now()
        val earliestExpenseDate = allExpenses.minOf { it.date }
        val totalDaysFromStart = ChronoUnit.DAYS.between(earliestExpenseDate, today).toInt().coerceAtLeast(1)

        val useRollingYear = totalDaysFromStart >= 365
        val windowStartDate = if (useRollingYear) today.minusDays(365) else earliestExpenseDate

        val filteredExpenses = allExpenses.filter { !it.date.isBefore(windowStartDate) }
        val totalExpenseAmount = filteredExpenses.sumOf { it.amount }
        val divisorDays = if (useRollingYear) 365 else totalDaysFromStart.coerceAtLeast(1)

        val costPerDay = totalExpenseAmount / divisorDays.toDouble()
        tvCostPerDayValue.text = "${formatMoney(costPerDay)} / сутки"

        val currentMileage = extractDigits(tvMileageValue.text.toString()).toLongOrNull()
        val startMileage = storage.loadStartMileage()?.toLongOrNull()

        if (currentMileage == null || startMileage == null || currentMileage <= startMileage) {
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val mileageDelta = currentMileage - startMileage
        if (mileageDelta <= 0L) {
            tvCostPerKmValue.text = getString(R.string.cost_no_data)
            return
        }

        val costPerKm = totalExpenseAmount / mileageDelta.toDouble()
        tvCostPerKmValue.text = "${formatMoney(costPerKm)} / км"
    }

    private fun buildAllExpenseItems(): List<ExpenseItem> {
        val result = mutableListOf<ExpenseItem>()

        documentationExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            val amount = parseAmount(draft.amount)
            result.add(ExpenseItem(date = date, amount = amount))
        }

        techniqueExpenseDrafts.forEach { draft ->
            val date = parseDraftDate(draft.date) ?: return@forEach
            val amount = parseAmount(draft.amount)
            result.add(ExpenseItem(date = date, amount = amount))
        }

        return result
    }

    private fun updateSoonDateValues() {
        containerSoonByDate.removeAllViews()

        val today = LocalDate.now()

        val items = documentationExpenseDrafts
            .mapNotNull { draft ->
                val validUntil = draft.validUntil ?: return@mapNotNull null
                val validDate = parseDraftDate(validUntil) ?: return@mapNotNull null

                SoonDateItem(
                    title = buildDocumentationTitle(draft),
                    targetDate = validDate
                )
            }
            .sortedBy { it.targetDate }

        if (items.isEmpty()) {
            addSoonPlaceholder(containerSoonByDate, getString(R.string.soon_no_data))
            return
        }

        items.forEach { item ->
            val daysLeft = ChronoUnit.DAYS.between(today, item.targetDate).toInt()
            addSoonRow(
                container = containerSoonByDate,
                title = item.title,
                value = formatRemainingDate(daysLeft)
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

            SoonMileageItem(
                title = nodeName,
                remainingKm = remaining
            )
        }.sortedBy { it.remainingKm }

        if (items.isEmpty()) {
            addSoonPlaceholder(containerSoonByMileage, getString(R.string.soon_mileage_no_data))
            return
        }

        items.forEach { item ->
            addSoonRow(
                container = containerSoonByMileage,
                title = item.title,
                value = formatRemainingMileage(item.remainingKm)
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
        return when {
            draft.title.isNotBlank() -> draft.title
            !draft.subtype.isNullOrBlank() -> "${draft.type}: ${draft.subtype}"
            else -> draft.type
        }
    }

    private fun addSoonPlaceholder(container: LinearLayout, text: String) {
        val textView = TextView(this).apply {
            this.text = text
            textSize = 14f
            setTextColor(android.graphics.Color.BLACK)
        }
        container.addView(textView)
    }

    private fun addSoonRow(container: LinearLayout, title: String, value: String) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(0, dp(4), 0, dp(8))
        }

        val titleView = TextView(this).apply {
            text = title
            textSize = 14f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(android.graphics.Color.BLACK)
        }

        val valueView = TextView(this).apply {
            text = value
            textSize = 13f
            setTextColor(android.graphics.Color.BLACK)
        }

        row.addView(titleView)
        row.addView(valueView)
        container.addView(row)
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
        return value.replace(",", ".").toDoubleOrNull() ?: 0.0
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

        private val BACKUP_FILE_DATE_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy_MM_dd")

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
        val targetDate: LocalDate
    )

    private data class SoonMileageItem(
        val title: String,
        val remainingKm: Long
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
}