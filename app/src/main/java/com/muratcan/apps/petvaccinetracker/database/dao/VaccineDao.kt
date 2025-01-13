package com.muratcan.apps.petvaccinetracker.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import kotlinx.coroutines.flow.Flow

@Dao
interface KtVaccineDao {
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPetFlow(petId: Long): Flow<List<Vaccine>>
    
    @Insert
    suspend fun insert(vaccine: Vaccine): Long
    
    @Insert
    suspend fun insertAll(vaccines: List<Vaccine>)
    
    @Update
    suspend fun update(vaccine: Vaccine)
    
    @Delete
    suspend fun delete(vaccine: Vaccine)
    
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    suspend fun deleteAllForPet(petId: Long)
}

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPet(petId: Long): androidx.lifecycle.LiveData<List<Vaccine>>
    
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPetSync(petId: Long): List<Vaccine>
    
    @Insert
    fun insert(vaccine: Vaccine): Long
    
    @Insert
    fun insertAll(vaccines: List<Vaccine>)
    
    @Update
    fun update(vaccine: Vaccine)
    
    @Delete
    fun delete(vaccine: Vaccine)
    
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    fun deleteAllForPet(petId: Long)
} 