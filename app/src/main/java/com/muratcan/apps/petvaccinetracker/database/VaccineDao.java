package com.muratcan.apps.petvaccinetracker.database;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;
import java.util.List;

@Dao
public interface VaccineDao {
    @Insert
    long insert(Vaccine vaccine);
    
    @Update
    void update(Vaccine vaccine);
    
    @Delete
    void delete(Vaccine vaccine);
    
    @Query("SELECT * FROM vaccines WHERE petId = :petId")
    List<Vaccine> getVaccinesForPet(long petId);
} 