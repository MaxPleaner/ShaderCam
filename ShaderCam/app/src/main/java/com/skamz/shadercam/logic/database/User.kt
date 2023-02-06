package com.skamz.shadercam.logic.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase

//interface UserInfo {
//    val name: String?
//}

typealias UserInfo = java.util.HashMap<String, String?>

interface UserInfoWrapper {
    val uid: UserInfo
}

class FirebaseUserDao {
    companion object {
        private val db = Firebase.database
        private fun currentUser(): FirebaseUser {
            return FirebaseAuth.getInstance().currentUser!!
        }

        fun getUserInfo(uid: String, callback: (UserInfo?) -> Unit) {
            val userRef = db.getReference("usernames/${uid}")
            userRef.get().addOnSuccessListener {
                val userInfo = it.value as UserInfo
                callback(userInfo)
            }.addOnFailureListener {
                Log.e("DEBUG", "FAILURE! $it")
            }
        }

        // TODO: Don't do this filtering client side
        fun usernameAlreadyTaken(username: String, callback: (Boolean) -> Unit) {
            Log.e("DEBUG", "sending query ..")
            val query = db.getReference("usernames")
            query.get().addOnSuccessListener {
                val usersData = it.children.mapNotNull { doc -> doc.value
                    doc.value as UserInfo
                }
                val taken = usersData.any { it["name"] == username }
                callback(taken)
            }.addOnFailureListener {
                Log.e("DEBUG", "FAILURE! $it")
            }
        }

        fun updateUserInfo(name: String? = null) {
            Log.e("DEBUG", "saving user name: $name")
            val userRef = db.getReference("usernames/${currentUser().uid}")
            userRef.setValue(
                mapOf("name" to name)
            )
        }
    }
}