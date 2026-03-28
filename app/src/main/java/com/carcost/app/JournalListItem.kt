package com.carcost.app

import java.time.LocalDate
import java.util.Locale

data class JournalListItem(
    val date: LocalDate,
    val typeCode: String,
    val shortTitle: String,
    val amount: String,
    val details: String,
    val editAction: () -> Unit,
    val deleteAction: () -> Boolean
) {
    val displayDate: String
        get() = "%02d.%02d.%04d".format(
            Locale.getDefault(),
            date.dayOfMonth,
            date.monthValue,
            date.year
        )
}