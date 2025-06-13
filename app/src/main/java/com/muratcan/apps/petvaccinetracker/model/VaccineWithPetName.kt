package com.muratcan.apps.petvaccinetracker.model

import androidx.room.Embedded

data class VaccineWithPetName(
    @Embedded val vaccine: Vaccine,
    val petName: String
) {
    fun toHistoryItem(): VaccineHistoryItem {
        return VaccineHistoryItem(
            vaccineId = vaccine.id,
            petName = petName,
            vaccineName = vaccine.name ?: "",
            dateAdministered = vaccine.dateAdministered,
            notes = vaccine.notes,
            petId = vaccine.petId,
            nextDueDate = vaccine.nextDueDate,
            isRecurring = vaccine.isRecurring
        )
    }
}