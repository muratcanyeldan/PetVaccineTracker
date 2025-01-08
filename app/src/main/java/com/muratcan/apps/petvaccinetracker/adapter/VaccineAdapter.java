package com.muratcan.apps.petvaccinetracker.adapter;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.chip.Chip;
import com.google.android.material.divider.MaterialDivider;
import com.muratcan.apps.petvaccinetracker.R;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import java.util.ArrayList;
import java.util.List;

public class VaccineAdapter extends RecyclerView.Adapter<VaccineAdapter.VaccineViewHolder> {
    private final List<Vaccine> vaccines;
    private final OnVaccineClickListener listener;

    public interface OnVaccineClickListener {
        void onVaccineClick(Vaccine vaccine);
    }

    public VaccineAdapter(List<Vaccine> vaccines, OnVaccineClickListener listener) {
        this.vaccines = new ArrayList<>(vaccines);
        this.listener = listener;
    }

    @NonNull
    @Override
    public VaccineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vaccine, parent, false);
        return new VaccineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull VaccineViewHolder holder, int position) {
        holder.bind(vaccines.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return vaccines.size();
    }

    public void updateVaccines(List<Vaccine> newVaccines) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new VaccineDiffCallback(vaccines, newVaccines));
        vaccines.clear();
        vaccines.addAll(newVaccines);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class VaccineDiffCallback extends DiffUtil.Callback {
        private final List<Vaccine> oldVaccines;
        private final List<Vaccine> newVaccines;

        VaccineDiffCallback(List<Vaccine> oldVaccines, List<Vaccine> newVaccines) {
            this.oldVaccines = oldVaccines;
            this.newVaccines = newVaccines;
        }

        @Override
        public int getOldListSize() {
            return oldVaccines.size();
        }

        @Override
        public int getNewListSize() {
            return newVaccines.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldVaccines.get(oldItemPosition).getId() == newVaccines.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Vaccine oldVaccine = oldVaccines.get(oldItemPosition);
            Vaccine newVaccine = newVaccines.get(newItemPosition);
            return oldVaccine.getName().equals(newVaccine.getName()) &&
                   (oldVaccine.getDateAdministered() == null ? 
                        newVaccine.getDateAdministered() == null : 
                        oldVaccine.getDateAdministered().equals(newVaccine.getDateAdministered())) &&
                   (oldVaccine.getNextDueDate() == null ? 
                        newVaccine.getNextDueDate() == null : 
                        oldVaccine.getNextDueDate().equals(newVaccine.getNextDueDate())) &&
                   oldVaccine.isRecurring() == newVaccine.isRecurring() &&
                   oldVaccine.getRecurringPeriodMonths() == newVaccine.getRecurringPeriodMonths() &&
                   (oldVaccine.getNotes() == null ? 
                        newVaccine.getNotes() == null : 
                        oldVaccine.getNotes().equals(newVaccine.getNotes()));
        }
    }

    public static class VaccineViewHolder extends RecyclerView.ViewHolder {
        private final TextView vaccineNameText;
        private final TextView vaccineDateText;
        private final TextView nextDueDateText;
        private final TextView vaccineNotesText;
        private final Chip vaccineStatusChip;
        private final MaterialDivider notesDivider;

        VaccineViewHolder(@NonNull View itemView) {
            super(itemView);
            vaccineNameText = itemView.findViewById(R.id.vaccineNameText);
            vaccineDateText = itemView.findViewById(R.id.vaccineDateText);
            nextDueDateText = itemView.findViewById(R.id.nextDueDateText);
            vaccineNotesText = itemView.findViewById(R.id.vaccineNotesText);
            vaccineStatusChip = itemView.findViewById(R.id.vaccineStatusChip);
            notesDivider = itemView.findViewById(R.id.notesDivider);
        }

        void bind(Vaccine vaccine, OnVaccineClickListener listener) {
            Context context = itemView.getContext();
            vaccineNameText.setText(vaccine.getName());
            
            if (vaccine.getDateAdministered() != null) {
                // Vaccine has been administered
                vaccineDateText.setText(context.getString(
                    R.string.vaccine_administered_date, vaccine.getDateAdministered()));
                vaccineDateText.setVisibility(View.VISIBLE);
                vaccineStatusChip.setVisibility(View.GONE);
                
                // Show next due date for recurring vaccines
                if (vaccine.isRecurring() && vaccine.getNextDueDate() != null && !vaccine.getNextDueDate().isEmpty()) {
                    nextDueDateText.setText(context.getString(
                        R.string.vaccine_next_due_format, vaccine.getNextDueDate()));
                    
                    // Check if due date is within 7 days
                    try {
                        java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault());
                        java.util.Date dueDate = dateFormat.parse(vaccine.getNextDueDate());
                        java.util.Calendar calendar = java.util.Calendar.getInstance();
                        java.util.Date today = calendar.getTime();
                        calendar.add(java.util.Calendar.DAY_OF_MONTH, 7);
                        java.util.Date sevenDaysLater = calendar.getTime();
                        
                        if (dueDate != null && !dueDate.before(today) && !dueDate.after(sevenDaysLater)) {
                            // Due within 7 days - show in warning color
                            nextDueDateText.setTextColor(ContextCompat.getColor(context, R.color.error));
                        } else {
                            // Due later - show in normal color
                            nextDueDateText.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
                        }
                    } catch (Exception e) {
                        // In case of date parsing error, use normal color
                        nextDueDateText.setTextColor(ContextCompat.getColor(context, R.color.textSecondary));
                    }
                    
                    nextDueDateText.setVisibility(View.VISIBLE);
                } else {
                    nextDueDateText.setVisibility(View.GONE);
                }
            } else {
                // Vaccine not administered yet
                vaccineDateText.setVisibility(View.GONE);
                vaccineStatusChip.setVisibility(View.VISIBLE);
                vaccineStatusChip.setText(R.string.vaccine_not_administered);
                vaccineStatusChip.setChipBackgroundColor(
                    ColorStateList.valueOf(ContextCompat.getColor(context, R.color.accent)));
                nextDueDateText.setVisibility(View.GONE);
            }

            if (vaccine.getNotes() != null && !vaccine.getNotes().isEmpty()) {
                notesDivider.setVisibility(View.VISIBLE);
                vaccineNotesText.setText(context.getString(
                    R.string.vaccine_notes_format, vaccine.getNotes()));
                vaccineNotesText.setVisibility(View.VISIBLE);
            } else {
                notesDivider.setVisibility(View.GONE);
                vaccineNotesText.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onVaccineClick(vaccine));
        }
    }
} 