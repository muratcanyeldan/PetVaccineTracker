package com.muratcan.apps.petvaccinetracker.util

import com.muratcan.apps.petvaccinetracker.model.Vaccine

object RecommendedVaccines {

    fun getRecommendedVaccines(petType: String, petId: Long): List<Vaccine> {
        val vaccines = mutableListOf<Vaccine>()

        when {
            petType.equals("Dog", ignoreCase = true) || petType.equals(
                "Köpek",
                ignoreCase = true
            ) -> {
                // Core vaccines for dogs
                addVaccine(vaccines, "Rabies", "Core vaccine for dogs", petId)
                addVaccine(
                    vaccines,
                    "Distemper (DHPP)",
                    "Core vaccine protecting against Distemper, Hepatitis, Parvovirus, and Parainfluenza",
                    petId
                )
                addVaccine(
                    vaccines,
                    "Bordetella",
                    "Recommended for dogs that interact with other dogs",
                    petId
                )
                addVaccine(vaccines, "Leptospirosis", "Recommended for dogs at risk", petId)
            }

            petType.equals("Cat", ignoreCase = true) || petType.equals(
                "Kedi",
                ignoreCase = true
            ) -> {
                // Core vaccines for cats
                addVaccine(vaccines, "Rabies", "Core vaccine for cats", petId)
                addVaccine(
                    vaccines,
                    "FVRCP",
                    "Core vaccine protecting against Feline Viral Rhinotracheitis, Calicivirus, and Panleukopenia",
                    petId
                )
                addVaccine(vaccines, "FeLV", "Recommended for outdoor cats", petId)
                addVaccine(vaccines, "FIV", "Recommended for cats at risk", petId)
            }
        }

        return vaccines
    }

    private fun addVaccine(
        vaccines: MutableList<Vaccine>,
        name: String,
        notes: String,
        petId: Long
    ) {
        val vaccine = Vaccine(
            name = name,
            notes = notes,
            dateAdministered = null,
            nextDueDate = null,  // Don't set next due date until administered
            petId = petId
        )
        vaccines.add(vaccine)
    }
} 