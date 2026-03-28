package com.carcost.app

import android.content.Context
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

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: inflater.inflate(R.layout.item_journal_entry, parent, false)

        val tvDate = view.findViewById<TextView>(R.id.tvJournalItemDate)
        val tvType = view.findViewById<TextView>(R.id.tvJournalItemType)
        val tvTitle = view.findViewById<TextView>(R.id.tvJournalItemTitle)
        val tvAmount = view.findViewById<TextView>(R.id.tvJournalItemAmount)

        val item = items[position]

        tvDate.text = item.displayDate
        tvType.text = item.typeCode
        tvTitle.text = item.shortTitle
        tvAmount.text = "${item.amount} €"

        return view
    }
}