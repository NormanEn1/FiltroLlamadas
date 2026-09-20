package com.norman.filtrollamadas.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [CallEntry::class, ListEntry::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun callDao(): CallDao
    abstract fun listDao(): ListDao
}
