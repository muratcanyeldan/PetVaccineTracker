package com.muratcan.apps.petvaccinetracker.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.muratcan.apps.petvaccinetracker.model.Pet;
import com.muratcan.apps.petvaccinetracker.model.Vaccine;

@Database(entities = {Pet.class, Vaccine.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    private static AppDatabase instance;
    
    public abstract PetDao petDao();
    public abstract VaccineDao vaccineDao();
    
    public static synchronized AppDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                context.getApplicationContext(),
                AppDatabase.class,
                "pet_vaccine_db")
                .fallbackToDestructiveMigration()
                .build();
        }
        return instance;
    }
} 