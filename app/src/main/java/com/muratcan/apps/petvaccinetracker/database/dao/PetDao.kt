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
interface KtPetDao {
    @Insert
    suspend fun insert(pet: Pet): Long
    
    @Update
    suspend fun update(pet: Pet)
    
    @Delete
    suspend fun delete(pet: Pet)
    
    @Query("SELECT * FROM pets")
    fun getAllPetsFlow(): Flow<List<Pet>>
    
    @Query("SELECT * FROM pets")
    fun getAllPets(): LiveData<List<Pet>>
    
    @Query("SELECT * FROM pets WHERE id = :petId")
    suspend fun getPetById(petId: Long): Pet
    
    @Query("SELECT * FROM pets WHERE user_id = :userId")
    fun getPetsForUser(userId: String): Flow<List<Pet>>

    @Transaction
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    suspend fun deleteAllVaccinesForPet(petId: Long)

    @Transaction
    suspend fun deletePetAndVaccines(pet: Pet) {
        deleteAllVaccinesForPet(pet.id)
        delete(pet)
    }

    @Query("SELECT * FROM pets ORDER BY id DESC LIMIT 1")
    suspend fun getLastAddedPet(): Pet
    
    @Query("SELECT * FROM pets ORDER BY name ASC")
    fun getAllPetsSortedByName(): Flow<List<Pet>>
    
    @Query("SELECT * FROM pets ORDER BY type ASC")
    fun getAllPetsSortedByType(): Flow<List<Pet>>
    
    @Query("SELECT * FROM pets ORDER BY birth_date DESC")
    fun getAllPetsSortedByBirthDate(): Flow<List<Pet>>

    @Query("SELECT * FROM pets")
    fun getAllPetsSync(): List<Pet>
} 