package com.muratcan.apps.petvaccinetracker.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import java.text.DateFormat
import java.util.concurrent.TimeUnit

class VaccineAdapter(
    vaccines: List<Vaccine>,
    private val listener: OnVaccineClickListener
) : RecyclerView.Adapter<VaccineAdapter.VaccineViewHolder>() {

    private var vaccines: MutableList<Vaccine> = ArrayList(vaccines)
    private val dateFormat: DateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM)

    interface OnVaccineClickListener {
        fun onVaccineClick(vaccine: Vaccine)
        fun onVaccineDelete(vaccine: Vaccine)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VaccineViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_vaccine, parent, false)
        return VaccineViewHolder(view)
    }

    override fun onBindViewHolder(holder: VaccineViewHolder, position: Int) {
        val vaccine = vaccines[position]
        holder.bind(vaccine)
    }

    override fun getItemCount(): Int {
        return vaccines.size
    }

    fun updateVaccines(newVaccines: List<Vaccine>) {
        val diffResult = DiffUtil.calculateDiff(VaccineDiffCallback(vaccines, newVaccines))
        vaccines.clear()
        vaccines.addAll(newVaccines)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getVaccines(): List<Vaccine> {
        return vaccines
    }

    private class VaccineDiffCallback(
        private val oldVaccines: List<Vaccine>,
        private val newVaccines: List<Vaccine>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldVaccines.size
        }

        override fun getNewListSize(): Int {
            return newVaccines.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldVaccines[oldItemPosition].id == newVaccines[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldVaccine = oldVaccines[oldItemPosition]
            val newVaccine = newVaccines[newItemPosition]

            return oldVaccine.name == newVaccine.name &&
                    oldVaccine.dateAdministered == newVaccine.dateAdministered &&
                    oldVaccine.nextDueDate == newVaccine.nextDueDate &&
                    oldVaccine.notes == newVaccine.notes
        }
    }

    inner class VaccineViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nameTextView: TextView = itemView.findViewById(R.id.vaccineNameTextView)
        private val dateAdministeredTextView: TextView =
            itemView.findViewById(R.id.dateAdministeredTextView)
        private val nextDueDateTextView: TextView = itemView.findViewById(R.id.nextDueDateTextView)
        private val notesTextView: TextView = itemView.findViewById(R.id.notesTextView)
        private val deleteButton: ImageButton = itemView.findViewById(R.id.deleteButton)

        fun bind(vaccine: Vaccine) {
            nameTextView.text = vaccine.name

            if (vaccine.dateAdministered != null && vaccine.dateAdministered!!.time > 0) {
                dateAdministeredTextView.text = itemView.context.getString(
                    R.string.vaccine_administered_text,
                    dateFormat.format(vaccine.dateAdministered)
                )
                dateAdministeredTextView.visibility = View.VISIBLE
            } else {
                dateAdministeredTextView.setText(R.string.vaccine_not_administered_text)
                dateAdministeredTextView.visibility = View.VISIBLE
            }

            if (vaccine.nextDueDate != null && vaccine.nextDueDate!!.time > 0) {
                nextDueDateTextView.text = itemView.context.getString(
                    R.string.vaccine_next_due_text,
                    dateFormat.format(vaccine.nextDueDate)
                )
                nextDueDateTextView.visibility = View.VISIBLE

                val daysUntilDue =
                    TimeUnit.MILLISECONDS.toDays(vaccine.nextDueDate!!.time - System.currentTimeMillis())
                if (daysUntilDue <= 7 && daysUntilDue >= 0) {
                    nextDueDateTextView.setTextColor(itemView.context.getColor(android.R.color.holo_red_dark))
                } else {
                    nextDueDateTextView.setTextColor(
                        itemView.context.resources.getColor(
                            com.google.android.material.R.color.material_on_surface_emphasis_medium,
                            itemView.context.theme
                        )
                    )
                }
            } else {
                nextDueDateTextView.visibility = View.GONE
            }

            if (!vaccine.notes.isNullOrEmpty()) {
                notesTextView.text = vaccine.notes
                notesTextView.visibility = View.VISIBLE
            } else {
                notesTextView.visibility = View.GONE
            }

            itemView.setOnClickListener { listener.onVaccineClick(vaccine) }

            deleteButton.setOnClickListener { v ->
                MaterialAlertDialogBuilder(v.context)
                    .setTitle(R.string.delete_vaccine_confirmation)
                    .setMessage(R.string.delete_vaccine_message)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        listener.onVaccineDelete(vaccine)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            }
        }
    }
} 