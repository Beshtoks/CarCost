package com.carcost.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class BankImportPreviewAdapter(
    context: Context,
    private val items: List<BankImportPreviewItem>
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_bank_import_preview, parent, false)

        val tvDate = view.findViewById<TextView>(R.id.tvBankImportDate)
        val tvKind = view.findViewById<TextView>(R.id.tvBankImportKind)
        val tvTitle = view.findViewById<TextView>(R.id.tvBankImportTitle)
        val tvAmount = view.findViewById<TextView>(R.id.tvBankImportAmount)
        val tvNarrative = view.findViewById<TextView>(R.id.tvBankImportNarrative)

        val item = items[position]

        val kindText = when (item.kind) {
            BankImportKind.INCOME -> "ДОХ"
            BankImportKind.DOCUMENTATION_EXPENSE -> "ДК"
            BankImportKind.TECHNIQUE_EXPENSE -> "ТХ"
            BankImportKind.UNKNOWN -> "?"
        }

        val titleText = when (item.kind) {
            BankImportKind.INCOME -> {
                item.incomeDraft?.title ?: "Не определено"
            }
            BankImportKind.DOCUMENTATION_EXPENSE -> {
                item.documentationDraft?.title ?: "Не определено"
            }
            BankImportKind.TECHNIQUE_EXPENSE -> {
                item.techniqueDraft?.titles?.joinToString(", ").orEmpty().ifBlank { "Не определено" }
            }
            BankImportKind.UNKNOWN -> "Не определено"
        }

        val textColor = when (item.kind) {
            BankImportKind.INCOME -> Color.parseColor("#A5D6A7")
            BankImportKind.DOCUMENTATION_EXPENSE,
            BankImportKind.TECHNIQUE_EXPENSE -> Color.parseColor("#F5B7B1")
            BankImportKind.UNKNOWN -> Color.parseColor("#D0D0D0")
        }

        tvDate.text = item.bankDate
        tvKind.text = kindText
        tvTitle.text = titleText
        tvAmount.text = "${item.amount} €"
        tvNarrative.text = item.narrative

        tvDate.setTextColor(textColor)
        tvKind.setTextColor(textColor)
        tvTitle.setTextColor(textColor)
        tvAmount.setTextColor(textColor)
        tvNarrative.setTextColor(textColor)

        return view
    }
}