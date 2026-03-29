package com.carcost.app

import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView

class JournalAdapter(
    context: Context,
    private val items: List<JournalListItem>
) : BaseAdapter() {

    private val inflater = LayoutInflater.from(context)

    override fun getCount(): Int = items.size

    override fun getItem(position: Int): Any = items[position]

    override fun getItemId(position: Int): Long = position.toLong()

    override fun getViewTypeCount(): Int = 2

    override fun getItemViewType(position: Int): Int {
        return if (items[position].isHeader) VIEW_TYPE_HEADER else VIEW_TYPE_ENTRY
    }

    override fun isEnabled(position: Int): Boolean {
        return !items[position].isHeader
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val item = items[position]

        return if (item.isHeader) {
            bindHeaderView(convertView, parent, item)
        } else {
            bindEntryView(convertView, parent, item)
        }
    }

    private fun bindHeaderView(convertView: View?, parent: ViewGroup, item: JournalListItem): View {
        val view = convertView ?: inflater.inflate(R.layout.item_journal_month_header, parent, false)
        val tvHeader = view.findViewById<TextView>(R.id.tvJournalMonthHeader)
        tvHeader.text = item.headerTitle
        return view
    }

    private fun bindEntryView(convertView: View?, parent: ViewGroup, item: JournalListItem): View {
        val view = convertView ?: inflater.inflate(R.layout.item_journal_entry, parent, false)

        val tvDate = view.findViewById<TextView>(R.id.tvJournalItemDate)
        val tvType = view.findViewById<TextView>(R.id.tvJournalItemType)
        val tvTitle = view.findViewById<TextView>(R.id.tvJournalItemTitle)
        val tvAmount = view.findViewById<TextView>(R.id.tvJournalItemAmount)

        val shortType = when (item.typeCode) {
            "ДОК" -> "ДК"
            "ТЕХ" -> "ТХ"
            "ДОХ" -> "ДХ"
            else -> item.typeCode
        }

        val isIncome = item.typeCode == "ДОХ"

        val textColor = if (isIncome) {
            Color.parseColor("#A5D6A7")
        } else {
            Color.parseColor("#F5B7B1")
        }

        tvDate.text = item.displayDate
        tvType.text = shortType
        tvTitle.text = item.shortTitle
        tvAmount.text = "${item.amount} €"

        tvDate.setTextColor(textColor)
        tvType.setTextColor(textColor)
        tvTitle.setTextColor(textColor)
        tvAmount.setTextColor(textColor)

        return view
    }

    companion object {
        private const val VIEW_TYPE_ENTRY = 0
        private const val VIEW_TYPE_HEADER = 1
    }
}