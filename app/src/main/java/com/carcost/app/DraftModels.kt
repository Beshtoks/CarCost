package com.carcost.app

import java.io.Serializable

data class IncomeDraft(
    val type: String,
    val subtype: String?,
    val title: String,
    val date: String,
    val amount: String,
    val comment: String
) : Serializable

data class DocumentationExpenseDraft(
    val type: String,
    val subtype: String?,
    val title: String,
    val date: String,
    val validUntil: String?,
    val amount: String,
    val comment: String
) : Serializable

data class TechniqueExpenseDraft(
    val recordType: String,          // Покупка / Установка
    val titles: List<String>,        // список узлов
    val date: String,
    val mileage: String,
    val quantity: String,
    val quantityUnit: String,
    val amount: String,
    val comment: String
) : Serializable