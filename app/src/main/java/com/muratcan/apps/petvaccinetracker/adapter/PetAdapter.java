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
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.util.ImageUtils;

import java.util.ArrayList;
import java.util.List;

public class PetAdapter extends RecyclerView.Adapter<PetAdapter.PetViewHolder> {
    private final List<Pet> pets;
    private final OnPetClickListener listener;

    public interface OnPetClickListener {
        void onPetClick(Pet pet);
    }

    public PetAdapter(List<Pet> pets, OnPetClickListener listener) {
        this.pets = new ArrayList<>(pets);
        this.listener = listener;
    }

    @NonNull
    @Override
    public PetViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pet, parent, false);
        return new PetViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PetViewHolder holder, int position) {
        holder.bind(pets.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return pets.size();
    }

    public void updatePets(List<Pet> newPets) {
        DiffUtil.DiffResult diffResult = DiffUtil.calculateDiff(new PetDiffCallback(pets, newPets));
        pets.clear();
        pets.addAll(newPets);
        diffResult.dispatchUpdatesTo(this);
    }

    private static class PetDiffCallback extends DiffUtil.Callback {
        private final List<Pet> oldPets;
        private final List<Pet> newPets;

        PetDiffCallback(List<Pet> oldPets, List<Pet> newPets) {
            this.oldPets = oldPets;
            this.newPets = newPets;
        }

        @Override
        public int getOldListSize() {
            return oldPets.size();
        }

        @Override
        public int getNewListSize() {
            return newPets.size();
        }

        @Override
        public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
            return oldPets.get(oldItemPosition).getId() == newPets.get(newItemPosition).getId();
        }

        @Override
        public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
            Pet oldPet = oldPets.get(oldItemPosition);
            Pet newPet = newPets.get(newItemPosition);
            return oldPet.getName().equals(newPet.getName()) &&
                   oldPet.getType().equals(newPet.getType()) &&
                   oldPet.getBreed().equals(newPet.getBreed()) &&
                   (oldPet.getImageUri() == null ? 
                        newPet.getImageUri() == null : 
                        oldPet.getImageUri().equals(newPet.getImageUri()));
        }
    }

    public static class PetViewHolder extends RecyclerView.ViewHolder {
        private final ImageView petImageView;
        private final TextView petNameTextView;
        private final TextView petTypeTextView;
        private final TextView petBreedTextView;
        private final View cardView;

        public PetViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView.findViewById(R.id.cardView);
            petImageView = itemView.findViewById(R.id.petImageView);
            petNameTextView = itemView.findViewById(R.id.petNameTextView);
            petTypeTextView = itemView.findViewById(R.id.petTypeTextView);
            petBreedTextView = itemView.findViewById(R.id.petBreedTextView);
        }

        void bind(Pet pet, OnPetClickListener listener) {
            String transitionName = "pet_card_" + pet.getId();
            cardView.setTransitionName(transitionName);
            cardView.setTag(transitionName);

            petNameTextView.setText(pet.getName());
            petTypeTextView.setText(pet.getType());
            petBreedTextView.setText(pet.getBreed());

            if (pet.getImageUri() != null) {
                ImageUtils.loadImage(itemView.getContext(), pet.getImageUri(), petImageView);
            } else {
                petImageView.setImageResource(R.drawable.ic_pet_placeholder);
            }

            itemView.setOnClickListener(v -> listener.onPetClick(pet));
        }
    }
} 