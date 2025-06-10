package com.muratcan.apps.petvaccinetracker.model

import java.util.Date

data class VaccineHistoryItem(
    val vaccineId: Long,
    val petName: String,
    val vaccineName: String,
    val dateAdministered: Date?,
    val notes: String?,
    val petId: Long,
    val nextDueDate: Date?,
    val isRecurring: Boolean
)