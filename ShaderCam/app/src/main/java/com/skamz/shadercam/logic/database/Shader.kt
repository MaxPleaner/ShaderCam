  package com.skamz.shadercam.logic.database

import android.util.Log
import androidx.room.*
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.ktx.Firebase
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json

  @Entity
data class Shader(
      @PrimaryKey(autoGenerate = true) var uid: Int,
      val name: String,
      var shaderMainText: String,
      var paramsJson: String,
      var userUid: String? = null,
      var isPublic: Boolean = false
)

class ShaderDaoWrapper(db: AppDatabase) {
    private val deviceShaderDao = db.shaderDao()
    private val firebaseShaderDao = FirebaseShaderDao

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

    fun getUserShaders(): List<Shader> {
        return deviceShaderDao.getUserShaders()
    }

    fun findByName(name: String): Shader? {
        return deviceShaderDao.findByName(name)
    }

    fun delete(shader: Shader) {
        if (currentUser() != null) {  firebaseShaderDao.delete(shader) }
        return deviceShaderDao.delete(shader)
    }
}

typealias ShaderAttrsHashMap = java.util.HashMap<String, *>

class FirebaseShaderDao {
    companion object {
        private val db = Firebase.database
        private fun currentUser(): FirebaseUser {
            return FirebaseAuth.getInstance().currentUser!!
        }

        private fun shaderFromHashMap(name: String, shaderAttrsHashMap: ShaderAttrsHashMap) : ShaderAttributes {
            return ShaderAttributes(
                name = name,
                shaderMainText = shaderAttrsHashMap["shaderMainText"] as String,
                params = Json.decodeFromString(shaderAttrsHashMap["paramsJson"] as String),
//                isPublic = shaderAttrsHashMap["public"] as Boolean,
                isPublic = true,
                isTemplate = false,
            )
        }

        fun insertAll(vararg shaders: Shader) {
            shaders.forEach { update(it) }
        }

        fun update(shader: Shader) {
            // TODO: ensure you can't update other peoples' shaders!
            val addToFolder = if (shader.isPublic) "public" else "private"
            val removeFromFolder = if (shader.isPublic) "private" else "public"

            val shaderRef =
                db.getReference("userShaders/${currentUser().uid}/shaders/$addToFolder/${shader.name}")
            shaderRef.setValue(
                mapOf(
                    "paramsJson" to shader.paramsJson,
                    "shaderMainText" to shader.shaderMainText,
                )
            )

            val removeShaderRef = db.getReference("userShaders/${currentUser().uid}/shaders/$removeFromFolder/${shader.name}")
            removeShaderRef.removeValue()
        }

//        fun getAll(): List<Shader> {
//            // TODO: Change this to get all users, and make a different function to get a user's shaders.
//            return listOf()
//        }
//
//        fun findByName(name: String): Shader? {
//            // TODO: change this to be scoped by user.
//            return null
//        }

        fun getUserShaders(uid: String, callback: (MutableMap<String, ShaderAttributes>) -> Unit) {
            val shaderRef =
                db.getReference("userShaders/${uid}/shaders/public")
            shaderRef.get().addOnSuccessListener {
                val shadersMap = mutableMapOf<String, ShaderAttributes>()
                it.children.forEach {
                    shadersMap[it.key!!] = shaderFromHashMap(it.key!!, it.value as ShaderAttrsHashMap)
                }
                callback(shadersMap)
            }.addOnFailureListener {
                Log.e("DEBUG", "FAILURE getUserShaders $it")
            }
        }

        fun delete(shader: Shader) {
            listOf("public", "private").forEach { visibility ->
                val shaderRef =
                    db.getReference("userShaders/${currentUser().uid}/shaders/$visibility/${shader.name}")
                shaderRef.removeValue()
            }
            // TODO: ensure you can't delete other peoples' shaders!
        }
    }
}

@Dao
interface DeviceShaderDao {
    @Query("SELECT * FROM shader")
    fun getUserShaders(): List<Shader>

    @Query("SELECT * FROM shader WHERE name LIKE :name LIMIT 1 ")
    fun findByName(name: String): Shader?

    @Insert
    fun insertAll(vararg shaders: Shader)

    @Delete
    fun delete(shader: Shader)

    @Update
    fun update(shader: Shader)
}

