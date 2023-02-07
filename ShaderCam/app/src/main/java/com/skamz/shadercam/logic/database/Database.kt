package com.skamz.shadercam.logic.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Shader::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun shaderDao(): DeviceShaderDao
}