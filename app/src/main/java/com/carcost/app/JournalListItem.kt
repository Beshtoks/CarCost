package com.carcost.app

import java.time.LocalDate
import java.util.Locale

data class JournalListItem(
    val isHeader: Boolean = false,
    val headerTitle: String = "",
    val date: LocalDate? = null,
    val typeCode: String = "",
    val shortTitle: String = "",
    val amount: String = "",
    val details: String = "",
    val editAction: (() -> Unit)? = null,
    val deleteAction: (() -> Boolean)? = null
) {
    val displayDate: String
        get() = if (date == null) {
            ""
        } else {
            "%02d.%02d.%02d".format(
                Locale.getDefault(),
                date.dayOfMonth,
                date.monthValue,
                date.year % 100
            )
        }

    companion object {
        fun header(title: String): JournalListItem {
            return JournalListItem(
                isHeader = true,
                headerTitle = title
            )
        }
    }
}