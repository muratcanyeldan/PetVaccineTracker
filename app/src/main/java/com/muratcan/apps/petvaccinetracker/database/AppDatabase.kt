package com.muratcan.apps.petvaccinetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.muratcan.apps.petvaccinetracker.database.dao.KtPetDao
import com.muratcan.apps.petvaccinetracker.database.dao.KtVaccineDao
import com.muratcan.apps.petvaccinetracker.database.dao.PetDao
import com.muratcan.apps.petvaccinetracker.database.dao.VaccineDao
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine

@Database(entities = [Pet::class, Vaccine::class], version = 2, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun ktPetDao(): KtPetDao
    abstract fun vaccineDao(): VaccineDao
    abstract fun ktVaccineDao(): KtVaccineDao

    companion object {
        private const val DATABASE_NAME = "pet_vaccine_db"
        @Volatile
        private var instance: AppDatabase? = null

        @JvmStatic
        fun getInstance(context: Context): AppDatabase {
            return instance ?: synchronized(this) {
                instance ?: buildDatabase(context).also { instance = it }
            }
        }

        private fun buildDatabase(context: Context): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DATABASE_NAME
            )
            .fallbackToDestructiveMigration()
            .build()
        }
    }
} 