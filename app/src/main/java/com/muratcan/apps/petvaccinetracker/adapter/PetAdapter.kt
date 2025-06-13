package com.muratcan.apps.petvaccinetracker.adapter

import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import com.muratcan.apps.petvaccinetracker.R
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.util.ImageUtils

class PetAdapter(
    pets: List<Pet>,
    private val listener: OnPetClickListener
) : RecyclerView.Adapter<PetAdapter.PetViewHolder>() {

    private val pets: MutableList<Pet> = ArrayList(pets)

    fun interface OnPetClickListener {
        fun onPetClick(pet: Pet)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PetViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_pet, parent, false)
        return PetViewHolder(view)
    }

    override fun onBindViewHolder(holder: PetViewHolder, position: Int) {
        holder.bind(pets[position], listener)
    }

    override fun getItemCount(): Int {
        return pets.size
    }

    fun updatePets(newPets: List<Pet>) {
        val diffResult = DiffUtil.calculateDiff(PetDiffCallback(pets, newPets))
        pets.clear()
        pets.addAll(newPets)
        diffResult.dispatchUpdatesTo(this)
    }

    private class PetDiffCallback(
        private val oldPets: List<Pet>,
        private val newPets: List<Pet>
    ) : DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldPets.size
        }

        override fun getNewListSize(): Int {
            return newPets.size
        }

        override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            return oldPets[oldItemPosition].id == newPets[newItemPosition].id
        }

        override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
            val oldPet = oldPets[oldItemPosition]
            val newPet = newPets[newItemPosition]
            return oldPet.name == newPet.name &&
                    oldPet.type == newPet.type &&
                    oldPet.breed == newPet.breed &&
                    oldPet.imageUri == newPet.imageUri
        }
    }

    class PetViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val cardView: View = itemView.findViewById(R.id.cardView)
        private val petImageView: ImageView = itemView.findViewById(R.id.petImageView)
        private val petNameTextView: TextView = itemView.findViewById(R.id.petNameTextView)
        private val petTypeChip: Chip = itemView.findViewById(R.id.petTypeChip)
        private val petBreedTextView: TextView = itemView.findViewById(R.id.petBreedTextView)

        fun bind(pet: Pet, listener: OnPetClickListener) {
            val transitionName = "pet_card_${pet.id}"
            cardView.transitionName = transitionName
            cardView.tag = transitionName

            petNameTextView.text = pet.name
            petTypeChip.text = pet.type
            petBreedTextView.text = pet.breed

            if (pet.imageUri != null) {
                ImageUtils.loadImage(itemView.context, Uri.parse(pet.imageUri), petImageView)
            } else {
                petImageView.setImageResource(R.drawable.ic_pet_placeholder)
            }

            itemView.setOnClickListener { listener.onPetClick(pet) }
        }
    }
} 