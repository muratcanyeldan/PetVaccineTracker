package com.muratcan.apps.petvaccinetracker.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.muratcan.apps.petvaccinetracker.model.Vaccine
import com.muratcan.apps.petvaccinetracker.model.VaccineWithPetName
import kotlinx.coroutines.flow.Flow

@Dao
interface VaccineDao {
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPet(petId: Long): androidx.lifecycle.LiveData<List<Vaccine>>

    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPetFlow(petId: Long): Flow<List<Vaccine>>

    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    fun getVaccinesForPetSync(petId: Long): List<Vaccine>

    @Query("SELECT * FROM vaccines WHERE id = :vaccineId")
    fun getVaccineById(vaccineId: Long): Vaccine?

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

    @Query(
        """
        SELECT v.*, p.name as petName 
        FROM vaccines v 
        INNER JOIN pets p ON v.petId = p.id 
        WHERE v.dateAdministered IS NOT NULL 
        ORDER BY v.dateAdministered DESC
    """
    )
    fun getVaccineHistoryRaw(): Flow<List<VaccineWithPetName>>

    @Query(
        """
        SELECT v.*, p.name as petName 
        FROM vaccines v 
        INNER JOIN pets p ON v.petId = p.id 
        WHERE v.dateAdministered IS NOT NULL AND v.petId = :petId
        ORDER BY v.dateAdministered DESC
    """
    )
    fun getVaccineHistoryForPetRaw(petId: Long): Flow<List<VaccineWithPetName>>

    @Query(
        """
    SELECT v.*, p.name as petName 
    FROM vaccines v 
    INNER JOIN pets p ON v.petId = p.id 
    WHERE v.nextDueDate IS NOT NULL AND v.nextDueDate > :currentDate
    ORDER BY v.nextDueDate ASC
"""
    )
    fun getFutureVaccinesWithPetNames(currentDate: Long = System.currentTimeMillis()): List<VaccineWithPetName>
} 