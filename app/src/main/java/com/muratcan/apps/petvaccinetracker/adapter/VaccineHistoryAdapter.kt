package com.muratcan.apps.petvaccinetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem
import java.text.DateFormat
import java.util.Calendar
import java.util.Date

class VaccineHistoryAdapter(
    private val listener: OnHistoryItemClickListener?
) : RecyclerView.Adapter<VaccineHistoryAdapter.HistoryViewHolder>() {

    private var historyItems: MutableList<VaccineHistoryItem> = ArrayList()
    private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    interface OnHistoryItemClickListener {
        fun onHistoryItemClick(item: VaccineHistoryItem)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vaccine_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val item = historyItems[position]
        holder.bind(item, position)
    }

    override fun getItemCount(): Int {
        return historyItems.size
    }

    fun updateHistory(newItems: List<VaccineHistoryItem>) {
        val diffResult = DiffUtil.calculateDiff(HistoryDiffCallback(historyItems, newItems))
        historyItems.clear()
        historyItems.addAll(newItems)
        diffResult.dispatchUpdatesTo(this)
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val petNameText: TextView = itemView.findViewById(R.id.pet_name_text)
        private val vaccineNameText: TextView = itemView.findViewById(R.id.vaccine_name_text)
        private val dateText: TextView = itemView.findViewById(R.id.date_text)
        private val notesText: TextView = itemView.findViewById(R.id.notes_text)
        private val nextDueText: TextView = itemView.findViewById(R.id.next_due_text)
        private val timelineIcon: ImageView = itemView.findViewById(R.id.timeline_icon)
        private val timelineLine: View = itemView.findViewById(R.id.timeline_line)
        private val recurringIndicator: View = itemView.findViewById(R.id.recurring_indicator)

        fun bind(item: VaccineHistoryItem, position: Int) {
            petNameText.text = item.petName
            vaccineNameText.text = item.vaccineName
            dateText.text = dateFormat.format(item.dateAdministered)

            // Notes
            if (!item.notes.isNullOrEmpty() && item.notes!!.trim().isNotEmpty()) {
                notesText.text = item.notes
                notesText.visibility = View.VISIBLE
            } else {
                notesText.visibility = View.GONE
            }

            // Next due date for recurring vaccines
            if (item.isRecurring && item.nextDueDate != null) {
                nextDueText.text = "Next due: ${dateFormat.format(item.nextDueDate)}"
                nextDueText.visibility = View.VISIBLE
                recurringIndicator.visibility = View.VISIBLE
            } else {
                nextDueText.visibility = View.GONE
                recurringIndicator.visibility = View.GONE
            }

            // Timeline styling
            timelineLine.visibility =
                if (position == historyItems.size - 1) View.INVISIBLE else View.VISIBLE

            // Different colors for recent vs old vaccines
            if (isRecent(item.dateAdministered)) {
                timelineIcon.setImageResource(R.drawable.ic_check_circle)
                timelineIcon.setColorFilter(itemView.context.getColor(android.R.color.holo_green_dark))
            } else {
                timelineIcon.setImageResource(R.drawable.ic_check_circle)
                timelineIcon.setColorFilter(itemView.context.getColor(android.R.color.darker_gray))
            }

            itemView.setOnClickListener {
                listener?.onHistoryItemClick(item)
            }
        }

        private fun isRecent(date: Date?): Boolean {
            if (date == null) return false
            val cal = Calendar.getInstance()
            cal.add(Calendar.DAY_OF_MONTH, -30) // Consider recent if within 30 days
            return date.after(cal.time)
        }
    }

    private class HistoryDiffCallback(
        private val oldList: List<VaccineHistoryItem>,
        private val newList: List<VaccineHistoryItem>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldList[oldItemPosition].vaccineId == newList[newItemPosition].vaccineId
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldItem = oldList[oldItemPosition]
            val newItem = newList[newItemPosition]
            return oldItem.vaccineId == newItem.vaccineId &&
                    oldItem.dateAdministered == newItem.dateAdministered
        }
    }
} 