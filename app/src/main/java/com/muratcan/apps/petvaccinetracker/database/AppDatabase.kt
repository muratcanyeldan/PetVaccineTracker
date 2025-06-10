package com.muratcan.apps.petvaccinetracker.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.muratcan.apps.petvaccinetracker.database.dao.KtPetDao
import com.muratcan.apps.petvaccinetracker.database.dao.PetDao
import com.muratcan.apps.petvaccinetracker.database.dao.VaccineDao
import com.muratcan.apps.petvaccinetracker.model.Pet
import com.muratcan.apps.petvaccinetracker.model.Vaccine

@Database(entities = [Pet::class, Vaccine::class], version = 3, exportSchema = false)
@TypeConverters(DateConverter::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun petDao(): PetDao
    abstract fun ktPetDao(): KtPetDao
    abstract fun vaccineDao(): VaccineDao

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
                //.fallbackToDestructiveMigration() //development
                .addMigrations(MIGRATION_2_3) // Enable migration
                .build()
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(database: SupportSQLiteDatabase) {
                database.execSQL("ALTER TABLE vaccines ADD COLUMN isRecurring INTEGER NOT NULL DEFAULT 0")
                database.execSQL("ALTER TABLE vaccines ADD COLUMN recurrenceMonths INTEGER NOT NULL DEFAULT 0")
            }
        }
    }
} 