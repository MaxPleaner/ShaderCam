package com.skamz.shadercam.logic.database

import android.util.Log
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.ktx.database
import com.google.firebase.database.ktx.getValue
import com.google.firebase.ktx.Firebase
import com.skamz.shadercam.ui.fragments.LoginFragment

typealias UsernameHashMap = java.util.HashMap<String, String>

data class UserInfo(
    val name: String,
    val uid: String
)

class FirebaseUserDao {
    companion object {
        private val db = Firebase.database
        private fun currentUser(): FirebaseUser? {
            return FirebaseAuth.getInstance().currentUser
        }

        private fun buildUserInfoStruct(uid: String, usernameHashMap: UsernameHashMap): UserInfo {
            return UserInfo(
                name = usernameHashMap["name"]!!,
                uid = uid
            )
        }

        fun getOtherUsers(callback: (List<UserInfo>) -> Unit) {
            val ref = db.getReference("usernames")
            ref.get().addOnSuccessListener { it ->
                getUserInfo(currentUser()?.uid) { userInfo ->
                    var usersData = it.children.mapNotNull { doc ->
                        doc.value
                        buildUserInfoStruct(doc.key!!, doc.value as UsernameHashMap)
                    }
                    usersData = usersData.filter {
                        it.name != userInfo.name
                    }
                    callback(usersData)
                }
            }.addOnFailureListener {
                Log.e("DEBUG", "FAILURE getOtherUsers $it")
            }
        }

        fun getUserInfo(uid: String?, callback: (UserInfo) -> Unit) {
            if (uid == null) {
                return callback(UserInfo("", ""))
            }
            val userRef = db.getReference("usernames/${uid}")
            userRef.get().addOnSuccessListener {
                val userInfo = it.value as UsernameHashMap
                callback(buildUserInfoStruct(uid, userInfo))
            }.addOnFailureListener {
                Log.e("DEBUG", "FAILURE getUserInfo $it")
            }
        }

        // TODO: Don't do this filtering client side
        fun usernameAlreadyTaken(username: String, callback: (Boolean) -> Unit) {
            getOtherUsers { users ->
                val taken = users.any { it.name == username }
                callback(taken)
            }
        }

        fun updateUserInfo(name: String? = null) {
            val nameVal = name ?: currentUser()!!.email
            val userRef = db.getReference("usernames/${currentUser()!!.uid}")
            userRef.setValue(
                mapOf("name" to nameVal)
            )
        }
    }
}