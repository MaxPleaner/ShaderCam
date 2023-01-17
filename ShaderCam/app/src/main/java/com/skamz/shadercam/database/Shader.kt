package com.skamz.shadercam.database


import androidx.room.*

@Entity
data class Shader(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    val name: String,
    var shaderMainText: String,
    var paramsJson: String?
)

@Dao
interface ShaderDao {
    @Query("SELECT * FROM shader")
    fun getAll(): List<Shader>

    @Query("SELECT * FROM shader WHERE uid IN (:ids)")
    fun loadAllByIds(ids: IntArray): List<Shader>

    @Query("SELECT * FROM shader WHERE name LIKE :name LIMIT 1 ")
    fun findByName(name: String): Shader

    @Insert
    fun insertAll(vararg shader: Shader)

    @Delete
    fun delete(shader: Shader)

    @Update
    fun update(shader: Shader)

}

