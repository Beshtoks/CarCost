package com.carcost.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val mileagePrefs = context.getSharedPreferences(MILEAGE_PREFS_NAME, Context.MODE_PRIVATE)

    fun saveMileage(value: String) {
        prefs.edit().putString(KEY_MILEAGE, value).apply()
    }

    fun loadMileage(): String? {
        return prefs.getString(KEY_MILEAGE, null)
    }

    fun saveFuelPrice(value: String) {
        prefs.edit().putString(KEY_FUEL_PRICE, value).apply()
    }

    fun loadFuelPrice(): String? {
        return prefs.getString(KEY_FUEL_PRICE, null)
    }

    fun saveStartMileage(value: String) {
        prefs.edit().putString(KEY_START_MILEAGE, value).apply()
    }

    fun loadStartMileage(): String? {
        return prefs.getString(KEY_START_MILEAGE, null)
    }

    fun clearStartMileage() {
        prefs.edit().remove(KEY_START_MILEAGE).apply()
    }

    fun saveIncomeDrafts(items: List<IncomeDraft>) {
        val array = JSONArray()
        items.forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("type", draft.type)
                    put("subtype", draft.subtype)
                    put("title", draft.title)
                    put("date", draft.date)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        prefs.edit().putString(KEY_INCOME_DRAFTS, array.toString()).apply()
    }

    fun loadIncomeDrafts(): MutableList<IncomeDraft> {
        val raw = prefs.getString(KEY_INCOME_DRAFTS, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<IncomeDraft>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    IncomeDraft(
                        type = obj.optString("type"),
                        subtype = obj.optString("subtype").takeIf { it.isNotBlank() },
                        title = obj.optString("title"),
                        date = obj.optString("date"),
                        amount = obj.optString("amount"),
                        comment = obj.optString("comment")
                    )
                )
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveDocumentationDrafts(items: List<DocumentationExpenseDraft>) {
        val array = JSONArray()
        items.forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("type", draft.type)
                    put("subtype", draft.subtype)
                    put("title", draft.title)
                    put("date", draft.date)
                    put("odometer", draft.odometer)
                    put("validUntil", draft.validUntil)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        prefs.edit().putString(KEY_DOCUMENTATION_DRAFTS, array.toString()).apply()
    }

    fun loadDocumentationDrafts(): MutableList<DocumentationExpenseDraft> {
        val raw = prefs.getString(KEY_DOCUMENTATION_DRAFTS, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<DocumentationExpenseDraft>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                result.add(
                    DocumentationExpenseDraft(
                        type = obj.optString("type"),
                        subtype = obj.optString("subtype").takeIf { it.isNotBlank() },
                        title = obj.optString("title"),
                        date = obj.optString("date"),
                        odometer = obj.optString("odometer"),
                        validUntil = obj.optString("validUntil").takeIf { it.isNotBlank() },
                        amount = obj.optString("amount"),
                        comment = obj.optString("comment")
                    )
                )
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun saveTechniqueDrafts(items: List<TechniqueExpenseDraft>) {
        val array = JSONArray()
        items.forEach { draft ->
            val titlesArray = JSONArray()
            draft.titles.forEach { titlesArray.put(it) }

            array.put(
                JSONObject().apply {
                    put("recordType", draft.recordType)
                    put("titles", titlesArray)
                    put("date", draft.date)
                    put("mileage", draft.mileage)
                    put("quantity", draft.quantity)
                    put("quantityUnit", draft.quantityUnit)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        prefs.edit().putString(KEY_TECHNIQUE_DRAFTS, array.toString()).apply()
    }

    fun loadTechniqueDrafts(): MutableList<TechniqueExpenseDraft> {
        val raw = prefs.getString(KEY_TECHNIQUE_DRAFTS, null) ?: return mutableListOf()
        return try {
            val array = JSONArray(raw)
            val result = mutableListOf<TechniqueExpenseDraft>()
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val titlesJson = obj.optJSONArray("titles") ?: JSONArray()
                val titles = mutableListOf<String>()
                for (j in 0 until titlesJson.length()) {
                    titles.add(titlesJson.optString(j))
                }

                result.add(
                    TechniqueExpenseDraft(
                        recordType = obj.optString("recordType"),
                        titles = titles,
                        date = obj.optString("date"),
                        mileage = obj.optString("mileage"),
                        quantity = obj.optString("quantity"),
                        quantityUnit = obj.optString("quantityUnit"),
                        amount = obj.optString("amount"),
                        comment = obj.optString("comment")
                    )
                )
            }
            result
        } catch (_: Exception) {
            mutableListOf()
        }
    }

    fun exportBackupJson(): String {
        val root = JSONObject().apply {
            put("version", BACKUP_VERSION)
            put("mileage", loadMileage())
            put("fuelPrice", loadFuelPrice())
            put("startMileage", loadStartMileage())
            put("incomeDrafts", buildIncomeDraftsJsonArray(loadIncomeDrafts()))
            put("documentationDrafts", buildDocumentationDraftsJsonArray(loadDocumentationDrafts()))
            put("techniqueDrafts", buildTechniqueDraftsJsonArray(loadTechniqueDrafts()))
            put("mileageRegistry", buildMileageRegistryJson())
        }
        return root.toString()
    }

    fun importBackupJson(rawJson: String) {
        val root = JSONObject(rawJson)
        val version = root.optInt("version", -1)
        if (version != BACKUP_VERSION) {
            throw IllegalArgumentException("Unsupported backup version")
        }

        val mileage = root.optString("mileage").takeIf { it.isNotBlank() }
        val fuelPrice = root.optString("fuelPrice").takeIf { it.isNotBlank() }
        val startMileage = root.optString("startMileage").takeIf { it.isNotBlank() }

        val incomeDrafts = parseIncomeDrafts(root.optJSONArray("incomeDrafts") ?: JSONArray())
        val documentationDrafts =
            parseDocumentationDrafts(root.optJSONArray("documentationDrafts") ?: JSONArray())
        val techniqueDrafts =
            parseTechniqueDrafts(root.optJSONArray("techniqueDrafts") ?: JSONArray())
        val mileageRegistry =
            parseMileageRegistry(root.optJSONObject("mileageRegistry") ?: JSONObject())

        prefs.edit().clear().apply()
        mileagePrefs.edit().clear().apply()

        saveNullableString(KEY_MILEAGE, mileage)
        saveNullableString(KEY_FUEL_PRICE, fuelPrice)
        saveNullableString(KEY_START_MILEAGE, startMileage)
        saveIncomeDrafts(incomeDrafts)
        saveDocumentationDrafts(documentationDrafts)
        saveTechniqueDrafts(techniqueDrafts)
        saveMileageRegistry(mileageRegistry)
    }

    private fun buildIncomeDraftsJsonArray(items: List<IncomeDraft>): JSONArray {
        val array = JSONArray()
        items.forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("type", draft.type)
                    put("subtype", draft.subtype)
                    put("title", draft.title)
                    put("date", draft.date)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        return array
    }

    private fun buildDocumentationDraftsJsonArray(items: List<DocumentationExpenseDraft>): JSONArray {
        val array = JSONArray()
        items.forEach { draft ->
            array.put(
                JSONObject().apply {
                    put("type", draft.type)
                    put("subtype", draft.subtype)
                    put("title", draft.title)
                    put("date", draft.date)
                    put("odometer", draft.odometer)
                    put("validUntil", draft.validUntil)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        return array
    }

    private fun buildTechniqueDraftsJsonArray(items: List<TechniqueExpenseDraft>): JSONArray {
        val array = JSONArray()
        items.forEach { draft ->
            val titlesArray = JSONArray()
            draft.titles.forEach { titlesArray.put(it) }

            array.put(
                JSONObject().apply {
                    put("recordType", draft.recordType)
                    put("titles", titlesArray)
                    put("date", draft.date)
                    put("mileage", draft.mileage)
                    put("quantity", draft.quantity)
                    put("quantityUnit", draft.quantityUnit)
                    put("amount", draft.amount)
                    put("comment", draft.comment)
                }
            )
        }
        return array
    }

    private fun buildMileageRegistryJson(): JSONObject {
        val result = JSONObject()
        FIXED_MILEAGE_NODES.forEach { nodeName ->
            result.put(nodeName, mileagePrefs.getString(makeMileageKey(nodeName), "") ?: "")
        }
        return result
    }

    private fun parseIncomeDrafts(array: JSONArray): MutableList<IncomeDraft> {
        val result = mutableListOf<IncomeDraft>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                IncomeDraft(
                    type = obj.optString("type"),
                    subtype = obj.optString("subtype").takeIf { it.isNotBlank() },
                    title = obj.optString("title"),
                    date = obj.optString("date"),
                    amount = obj.optString("amount"),
                    comment = obj.optString("comment")
                )
            )
        }
        return result
    }

    private fun parseDocumentationDrafts(array: JSONArray): MutableList<DocumentationExpenseDraft> {
        val result = mutableListOf<DocumentationExpenseDraft>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            result.add(
                DocumentationExpenseDraft(
                    type = obj.optString("type"),
                    subtype = obj.optString("subtype").takeIf { it.isNotBlank() },
                    title = obj.optString("title"),
                    date = obj.optString("date"),
                    odometer = obj.optString("odometer"),
                    validUntil = obj.optString("validUntil").takeIf { it.isNotBlank() },
                    amount = obj.optString("amount"),
                    comment = obj.optString("comment")
                )
            )
        }
        return result
    }

    private fun parseTechniqueDrafts(array: JSONArray): MutableList<TechniqueExpenseDraft> {
        val result = mutableListOf<TechniqueExpenseDraft>()
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            val titlesJson = obj.optJSONArray("titles") ?: JSONArray()
            val titles = mutableListOf<String>()
            for (j in 0 until titlesJson.length()) {
                titles.add(titlesJson.optString(j))
            }

            result.add(
                TechniqueExpenseDraft(
                    recordType = obj.optString("recordType"),
                    titles = titles,
                    date = obj.optString("date"),
                    mileage = obj.optString("mileage"),
                    quantity = obj.optString("quantity"),
                    quantityUnit = obj.optString("quantityUnit"),
                    amount = obj.optString("amount"),
                    comment = obj.optString("comment")
                )
            )
        }
        return result
    }

    private fun parseMileageRegistry(obj: JSONObject): Map<String, String> {
        val result = linkedMapOf<String, String>()
        FIXED_MILEAGE_NODES.forEach { nodeName ->
            result[nodeName] = obj.optString(nodeName, "")
        }
        return result
    }

    private fun saveMileageRegistry(values: Map<String, String>) {
        val editor = mileagePrefs.edit()
        FIXED_MILEAGE_NODES.forEach { nodeName ->
            editor.putString(makeMileageKey(nodeName), values[nodeName].orEmpty())
        }
        editor.apply()
    }

    private fun saveNullableString(key: String, value: String?) {
        prefs.edit().putString(key, value).apply()
    }

    private fun makeMileageKey(nodeName: String): String {
        return "interval_$nodeName"
    }

    companion object {
        private const val PREFS_NAME = "carcost_storage"
        private const val MILEAGE_PREFS_NAME = "mileage_registry_prefs"
        private const val BACKUP_VERSION = 1

        private const val KEY_MILEAGE = "key_mileage"
        private const val KEY_FUEL_PRICE = "key_fuel_price"
        private const val KEY_START_MILEAGE = "key_start_mileage"
        private const val KEY_INCOME_DRAFTS = "key_income_drafts"
        private const val KEY_DOCUMENTATION_DRAFTS = "key_documentation_drafts"
        private const val KEY_TECHNIQUE_DRAFTS = "key_technique_drafts"

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
}