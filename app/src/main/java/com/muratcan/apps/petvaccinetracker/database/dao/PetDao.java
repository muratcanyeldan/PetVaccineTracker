package com.muratcan.apps.petvaccinetracker.database.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.muratcan.apps.petvaccinetracker.model.Pet;

import java.util.List;

@Dao
public interface PetDao {
    @Insert
    long insert(Pet pet);
    
    @Update
    void update(Pet pet);
    
    @Delete
    void delete(Pet pet);
    
    @Query("SELECT * FROM pets")
    LiveData<List<Pet>> getAllPets();
    
    @Query("SELECT * FROM pets WHERE id = :petId")
    Pet getPetById(long petId);

    @Transaction
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    void deleteAllVaccinesForPet(long petId);

    @Query("SELECT * FROM pets ORDER BY id DESC LIMIT 1")
    Pet getLastAddedPet();
} 