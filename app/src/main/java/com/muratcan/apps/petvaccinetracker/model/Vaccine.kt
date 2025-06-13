package com.muratcan.apps.petvaccinetracker.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(
    tableName = "vaccines",
    foreignKeys = [ForeignKey(
        entity = Pet::class,
        parentColumns = arrayOf("id"),
        childColumns = arrayOf("petId"),
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("petId")]
)
@Parcelize
data class Vaccine(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,
    var name: String? = null,
    var dateAdministered: Date? = null,
    var nextDueDate: Date? = null,
    var notes: String? = null,
    var petId: Long = 0,
    var isRecurring: Boolean = false,
    var recurrenceMonths: Int = 0
) : Parcelable 