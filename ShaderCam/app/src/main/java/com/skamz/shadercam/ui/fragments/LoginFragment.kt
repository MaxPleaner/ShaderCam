package com.skamz.shadercam.ui.fragments

import android.content.Context
import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import com.google.firebase.auth.FirebaseAuth
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.FragmentLoginBinding
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.messaging.FirebaseMessaging
import com.skamz.shadercam.ui.activities.CameraActivity
import java.util.*

class LoginFragment : Fragment() {

    private val TAG = "Login"
    private lateinit var binding : FragmentLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9002
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_login, container, false)

        // configure google sign in
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(requireContext().getString(R.string.default_web_client_id))
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(requireActivity(), gso)

        //Firebase auth instance
        mAuth = FirebaseAuth.getInstance()
        updateLoginState()

        setOnClickListeners()

        return binding.root
    }

    private fun updateLoginState() {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            binding.loginSection.visibility = View.GONE
            binding.logoutSection.visibility = View.VISIBLE
            binding.currentUserInfo.text = "Logged in as\n${currentUser.email}"
        } else {
            binding.loginSection.visibility = View.VISIBLE
            binding.logoutSection.visibility = View.GONE
        }
    }

    private fun setOnClickListeners() {
        binding.loginButton.setOnClickListener {
            signIn()
            Toast.makeText(requireActivity(), "Logged in", Toast.LENGTH_SHORT).show()
        }
        binding.logoutButton.setOnClickListener {
            signOut()
            Toast.makeText(requireActivity(), "Logged out", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        googleSignInClient.signOut()
            .addOnCompleteListener(requireActivity()) {
                FirebaseAuth.getInstance().signOut()
                updateLoginState()
            }
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        requireActivity().startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (requestCode == RC_SIGN_IN) {
                val task: Task<GoogleSignInAccount> =
                    GoogleSignIn.getSignedInAccountFromIntent(data)
                // Google Sign In was successful, authenticate with Firebase
                val account: GoogleSignInAccount =
                    task.getResult(ApiException::class.java)
                firebaseAuthWithGoogle(Objects.requireNonNull(account))
            } else {
                Toast.makeText(activity,getString(R.string.something_went_wrong),Toast.LENGTH_SHORT).show()
            }
    }


    private fun firebaseAuthWithGoogle(acct: GoogleSignInAccount) {
        try {
            val credential = GoogleAuthProvider.getCredential(acct.idToken, null)
            mAuth.signInWithCredential(credential)
                .addOnCompleteListener(requireActivity()) { task ->
                    try {
                        if (task.isSuccessful) {
                            //Sign in success
                            FirebaseMessaging.getInstance().token
                                .addOnCompleteListener { fcmTask: Task<String> ->
                                    if (task.isSuccessful) {
                                        // Get new FCM registration token
                                        updateLoginState()
                                        val token = fcmTask.result
                                        Log.e(TAG, "fcm token: $token")
                                    } else {
                                        Log.e(TAG, "could not get token")
                                    }
                                }
                            Log.e(TAG, "signInWithCredential:success")
                        } else {
                            //if sign in fails
                            Log.e(TAG, "signInWithCredential:failure", task.exception)
                        }
                    } catch (e: Exception) {
                        Toast.makeText(activity,getString(R.string.something_went_wrong),Toast.LENGTH_SHORT).show()
                    }
                }
        } catch (e: Exception) {
            Toast.makeText(activity,getString(R.string.something_went_wrong),Toast.LENGTH_SHORT).show()
        }
    }
}