package com.carcost.app

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

class AppStorage(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

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

    companion object {
        private const val PREFS_NAME = "carcost_storage"

        private const val KEY_MILEAGE = "key_mileage"
        private const val KEY_FUEL_PRICE = "key_fuel_price"
        private const val KEY_INCOME_DRAFTS = "key_income_drafts"
        private const val KEY_DOCUMENTATION_DRAFTS = "key_documentation_drafts"
        private const val KEY_TECHNIQUE_DRAFTS = "key_technique_drafts"
    }
}