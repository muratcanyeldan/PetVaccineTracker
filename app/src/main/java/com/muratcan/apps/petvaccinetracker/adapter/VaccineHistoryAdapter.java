package com.muratcan.apps.petvaccinetracker.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.RecyclerView;

import com.muratcan.apps.petvaccinetracker.R;
import com.muratcan.apps.petvaccinetracker.model.VaccineHistoryItem;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

public class VaccineHistoryAdapter extends RecyclerView.Adapter<VaccineHistoryAdapter.HistoryViewHolder> {
    private List<VaccineHistoryItem> historyItems;
    private final DateFormat dateFormat;
    private final OnHistoryItemClickListener listener;

    public interface OnHistoryItemClickListener {
        void onHistoryItemClick(VaccineHistoryItem item);
    }

    public VaccineHistoryAdapter(OnHistoryItemClickListener listener) {
        this.historyItems = new ArrayList<>();
        this.dateFormat = DateFormat.getDateInstance(DateFormat.MEDIUM);
        this.listener = listener;
    }

    @NonNull
    @Override
    public HistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_vaccine_history, parent, false);
        return new HistoryViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HistoryViewHolder holder, int position) {
        VaccineHistoryItem item = historyItems.get(position);
        holder.bind(item, position);
    }

    @Override
    public int getItemCount() {
        return historyItems.size();
    }

    public void updateHistory(List<VaccineHistoryItem> newItems) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new HistoryDiffCallback(this.historyItems, newItems));
        this.historyItems.clear();
        this.historyItems.addAll(newItems);
        diffResult.dispatchUpdatesTo(this);
    }

    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView petNameText;
        private final TextView vaccineNameText;
        private final TextView dateText;
        private final TextView notesText;
        private final TextView nextDueText;
        private final ImageView timelineIcon;
        private final View timelineLine;
        private final View recurringIndicator;

        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            petNameText = itemView.findViewById(R.id.pet_name_text);
            vaccineNameText = itemView.findViewById(R.id.vaccine_name_text);
            dateText = itemView.findViewById(R.id.date_text);
            notesText = itemView.findViewById(R.id.notes_text);
            nextDueText = itemView.findViewById(R.id.next_due_text);
            timelineIcon = itemView.findViewById(R.id.timeline_icon);
            timelineLine = itemView.findViewById(R.id.timeline_line);
            recurringIndicator = itemView.findViewById(R.id.recurring_indicator);
        }

        public void bind(VaccineHistoryItem item, int position) {
            petNameText.setText(item.getPetName());
            vaccineNameText.setText(item.getVaccineName());
            dateText.setText(dateFormat.format(item.getDateAdministered()));

            // Notes
            if (item.getNotes() != null && !item.getNotes().trim().isEmpty()) {
                notesText.setText(item.getNotes());
                notesText.setVisibility(View.VISIBLE);
            } else {
                notesText.setVisibility(View.GONE);
            }

            // Next due date for recurring vaccines
            if (item.isRecurring() && item.getNextDueDate() != null) {
                nextDueText.setText("Next due: " + dateFormat.format(item.getNextDueDate()));
                nextDueText.setVisibility(View.VISIBLE);
                recurringIndicator.setVisibility(View.VISIBLE);
            } else {
                nextDueText.setVisibility(View.GONE);
                recurringIndicator.setVisibility(View.GONE);
            }

            // Timeline styling
            timelineLine.setVisibility(position == historyItems.size() - 1 ? View.INVISIBLE : View.VISIBLE);

            // Different colors for recent vs old vaccines
            if (isRecent(item.getDateAdministered())) {
                timelineIcon.setImageResource(R.drawable.ic_check_circle);
                timelineIcon.setColorFilter(itemView.getContext().getColor(android.R.color.holo_green_dark));
            } else {
                timelineIcon.setImageResource(R.drawable.ic_check_circle);
                timelineIcon.setColorFilter(itemView.getContext().getColor(android.R.color.darker_gray));
            }

            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onHistoryItemClick(item);
                }
            });
        }

        private boolean isRecent(Date date) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_MONTH, -30); // Consider recent if within 30 days
            return date.after(cal.getTime());
        }
    }

    private static class HistoryDiffCallback extends DiffUtil.Callback {
        private final List<VaccineHistoryItem> oldList;
        private final List<VaccineHistoryItem> newList;

        public HistoryDiffCallback(List<VaccineHistoryItem> oldList, List<VaccineHistoryItem> newList) {
            this.oldList = oldList;
            this.newList = newList;
        }

        @Override
        public int getOldListSize() {
            return oldList.size();
        }

        @Override
        public int getNewListSize() {
            return newList.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldList.get(oldItemPosition).getVaccineId() == newList.get(newItemPosition).getVaccineId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            VaccineHistoryItem oldItem = oldList.get(oldItemPosition);
            VaccineHistoryItem newItem = newList.get(newItemPosition);
            return oldItem.getVaccineId() == newItem.getVaccineId() &&
                    oldItem.getDateAdministered().equals(newItem.getDateAdministered());
        }
    }
}