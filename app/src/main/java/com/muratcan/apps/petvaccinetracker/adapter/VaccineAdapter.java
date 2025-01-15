package com.muratcan.apps.petvaccinetracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.muratcan.apps.petvaccinetracker.R;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.List;

public class VaccineAdapter extends RecyclerView.Adapter<VaccineAdapter.VaccineViewHolder> {
    private List<Vaccine> vaccines;
    private final OnVaccineClickListener listener;
    private final DateFormat dateFormat;

    public interface OnVaccineClickListener {
        void onVaccineClick(Vaccine vaccine);
        void onVaccineDelete(Vaccine vaccine);
    }

    public VaccineAdapter(List<Vaccine> vaccines, OnVaccineClickListener listener) {
        this.vaccines = new ArrayList<>(vaccines);
        this.listener = listener;
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
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
        Vaccine vaccine = vaccines.get(position);
        holder.bind(vaccine);
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

    public List<Vaccine> getVaccines() {
        return vaccines;
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
                   (oldVaccine.getNotes() == null ? 
                        newVaccine.getNotes() == null : 
                        oldVaccine.getNotes().equals(newVaccine.getNotes()));
        }
    }

    public class VaccineViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final TextView dateAdministeredTextView;
        private final TextView nextDueDateTextView;
        private final TextView notesTextView;
        private final ImageButton deleteButton;

        VaccineViewHolder(View itemView) {
            super(itemView);
            nameTextView = itemView.findViewById(R.id.vaccineNameTextView);
            dateAdministeredTextView = itemView.findViewById(R.id.dateAdministeredTextView);
            nextDueDateTextView = itemView.findViewById(R.id.nextDueDateTextView);
            notesTextView = itemView.findViewById(R.id.notesTextView);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }

        void bind(final Vaccine vaccine) {
            nameTextView.setText(vaccine.getName());
            
            if (vaccine.getDateAdministered() != null && vaccine.getDateAdministered().getTime() > 0) {
                dateAdministeredTextView.setText(itemView.getContext().getString(
                    R.string.vaccine_administered_text,
                    dateFormat.format(vaccine.getDateAdministered())
                ));
                dateAdministeredTextView.setVisibility(View.VISIBLE);
            } else {
                dateAdministeredTextView.setText(R.string.vaccine_not_administered_text);
                dateAdministeredTextView.setVisibility(View.VISIBLE);
            }
            
            if (vaccine.getNextDueDate() != null && vaccine.getNextDueDate().getTime() > 0) {
                nextDueDateTextView.setText(itemView.getContext().getString(
                    R.string.vaccine_next_due_text,
                    dateFormat.format(vaccine.getNextDueDate())
                ));
                nextDueDateTextView.setVisibility(View.VISIBLE);
                
                long daysUntilDue = (vaccine.getNextDueDate().getTime() - System.currentTimeMillis()) / (1000 * 60 * 60 * 24);
                if (daysUntilDue <= 7 && daysUntilDue >= 0) {
                    nextDueDateTextView.setTextColor(itemView.getContext().getColor(android.R.color.holo_red_dark));
                } else {
                    nextDueDateTextView.setTextColor(itemView.getContext().getResources().getColor(com.google.android.material.R.color.material_on_surface_emphasis_medium, itemView.getContext().getTheme()));
                }
            } else {
                nextDueDateTextView.setVisibility(View.GONE);
            }
            
            if (vaccine.getNotes() != null && !vaccine.getNotes().isEmpty()) {
                notesTextView.setText(vaccine.getNotes());
                notesTextView.setVisibility(View.VISIBLE);
            } else {
                notesTextView.setVisibility(View.GONE);
            }

            itemView.setOnClickListener(v -> listener.onVaccineClick(vaccine));
            
            deleteButton.setOnClickListener(v -> new MaterialAlertDialogBuilder(v.getContext())
                .setTitle(R.string.delete_vaccine_confirmation)
                .setMessage(R.string.delete_vaccine_message)
                .setPositiveButton(android.R.string.ok, (dialog, which) ->
                    listener.onVaccineDelete(vaccine))
                .setNegativeButton(android.R.string.cancel, null)
                .show());
        }
    }
} 