  package com.skamz.shadercam.logic.database

import androidx.room.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase

  @Entity
data class Shader(
    @PrimaryKey(autoGenerate = true) val uid: Int,
    val name: String,
    var shaderMainText: String,
    var paramsJson: String,
    var userUid: String? = null,
    var isPublic: Boolean = false
)

class ShaderDaoWrapper(db: AppDatabase) {
    private val deviceShaderDao = db.shaderDao()
    private val firebaseShaderDao = FirebaseShaderDao()

    private fun currentUser (): FirebaseUser? {
        return FirebaseAuth.getInstance().currentUser
    }

    fun insertAll(vararg shader: Shader) {
        if (currentUser() != null) {  firebaseShaderDao.insertAll(*shader) }
        deviceShaderDao.insertAll(*shader)
    }

    fun update(shader: Shader) {
        if (currentUser() != null) {  firebaseShaderDao.update(shader) }
        deviceShaderDao.update(shader)
    }

    fun getAll(): List<Shader> {
        if (currentUser() != null) {  firebaseShaderDao.getAll() }
        return deviceShaderDao.getAll()
    }

    fun findByName(name: String): Shader? {
        if (currentUser() != null) {  firebaseShaderDao.findByName(name) }
        return deviceShaderDao.findByName(name)
    }

    fun delete(shader: Shader) {
        if (currentUser() != null) {  firebaseShaderDao.delete(shader) }
        return deviceShaderDao.delete(shader)
    }
}

class FirebaseShaderDao {
    private val db = Firebase.database
    private fun currentUser (): FirebaseUser {
        return FirebaseAuth.getInstance().currentUser!!
    }

    fun insertAll(vararg shaders: Shader) {
        shaders.forEach { update(it) }
    }

    fun update(shader: Shader) {
        // TODO: ensure you can't update other peoples' shaders!
        val shaderRef = db.getReference("userShaders/${currentUser().uid}/shaders/${shader.name}")
        shaderRef.setValue(mapOf(
            "paramsJson" to shader.paramsJson,
            "shaderMainText" to shader.shaderMainText,
            "public" to shader.isPublic
        ))
    }

    fun getAll(): List<Shader> {
        // TODO: Change this to get all users, and make a different function to get a user's shaders.
        return listOf()
    }

    fun findByName(name: String): Shader? {
        // TODO: change this to be scoped by user.
        return null
    }

    fun delete(shader: Shader) {
        // TODO: ensure you can't delete other peoples' shaders!
        val shaderRef = db.getReference("userShaders/${currentUser().uid}/shaders/${shader.name}")
        shaderRef.removeValue()
    }
}

@Dao
interface DeviceShaderDao {
    @Query("SELECT * FROM shader")
    fun getAll(): List<Shader>

    @Query("SELECT * FROM shader WHERE name LIKE :name LIMIT 1 ")
    fun findByName(name: String): Shader?

    @Insert
    fun insertAll(vararg shaders: Shader)

    @Delete
    fun delete(shader: Shader)

    @Update
    fun update(shader: Shader)
}

