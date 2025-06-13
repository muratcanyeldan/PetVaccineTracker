package com.muratcan.apps.petvaccinetracker.model

import android.net.Uri
import android.os.Parcelable
import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.muratcan.apps.petvaccinetracker.database.DateConverter
import kotlinx.parcelize.Parcelize
import java.util.Date

@Entity(tableName = "pets")
@Parcelize
data class Pet(
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0,

    @ColumnInfo(name = "name")
    var name: String = "",

    @ColumnInfo(name = "type")
    var type: String = "",

    @ColumnInfo(name = "breed")
    var breed: String = "Unknown",

    @TypeConverters(DateConverter::class)
    @ColumnInfo(name = "birth_date")
    var birthDate: Date = Date(),

    @ColumnInfo(name = "image_uri")
    var imageUri: String? = null,

    @ColumnInfo(name = "user_id")
    var userId: String = "default_user"
) : Parcelable {

    // Constructor for creating new pets
    @Ignore
    constructor(
        name: String?,
        type: String?,
        breed: String?,
        birthDate: Date?,
        imageUri: Uri?,
        userId: String?
    ) : this(
        id = 0,
        name = name ?: "",
        type = type ?: "",
        breed = breed ?: "Unknown",
        birthDate = birthDate ?: Date(),
        imageUri = imageUri?.toString(),
        userId = userId ?: "default_user"
    )
} 