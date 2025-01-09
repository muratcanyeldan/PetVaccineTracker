package com.muratcan.apps.petvaccinetracker.database;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import com.muratcan.apps.petvaccinetracker.model.Vaccine;

import java.util.List;

@Dao
public interface VaccineDao {
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    LiveData<List<Vaccine>> getVaccinesForPet(long petId);
    
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    List<Vaccine> getVaccinesForPetSync(long petId);
    
    @Insert
    long insert(Vaccine vaccine);
    
    @Insert
    void insertAll(List<Vaccine> vaccines);
    
    @Update
    void update(Vaccine vaccine);
    
    @Delete
    void delete(Vaccine vaccine);
    
    @Query("DELETE FROM vaccines WHERE petId = :petId")
    void deleteAllForPet(long petId);
} 