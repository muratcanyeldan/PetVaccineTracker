package com.muratcan.apps.petvaccinetracker.database.dao

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.muratcan.apps.petvaccinetracker.model.Pet
import kotlinx.coroutines.flow.Flow

@Dao
interface PetDao {
    @Insert
    fun insert(pet: Pet): Long

    @Update
    fun update(pet: Pet)

    @Delete
    fun delete(pet: Pet)

    @Query("SELECT * FROM pets")
    fun getAllPets(): LiveData<List<Pet>>

    @Query("SELECT * FROM pets")
    fun getAllPetsFlow(): Flow<List<Pet>>

    @Query("SELECT * FROM pets WHERE user_id = :userId")
    fun getPetsForUser(userId: String): Flow<List<Pet>>

    @Query("SELECT * FROM pets WHERE id = :petId")
    fun getPetById(petId: Long): Pet?

    @Query("SELECT * FROM pets")
    fun getAllPetsSync(): List<Pet>

    @Transaction
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    fun deleteAllVaccinesForPet(petId: Long)

    @Query("SELECT * FROM pets ORDER BY id DESC LIMIT 1")
    fun getLastAddedPet(): Pet?
} 