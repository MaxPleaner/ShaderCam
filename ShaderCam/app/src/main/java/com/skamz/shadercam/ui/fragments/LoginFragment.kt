package com.skamz.shadercam.ui.fragments

import android.content.Intent
import android.graphics.Camera
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
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
import com.skamz.shadercam.logic.database.FirebaseShaderDao
import com.skamz.shadercam.logic.database.FirebaseUserDao
import com.skamz.shadercam.logic.database.Shader
import com.skamz.shadercam.logic.util.TaskRunner
import com.skamz.shadercam.ui.activities.CameraActivity
import com.skamz.shadercam.ui.activities.ShaderSelectActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.*

class LoginFragment : Fragment() {

    private val TAG = "Login"
    private lateinit var binding : FragmentLoginBinding
    private lateinit var mAuth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient

    companion object {
        private const val RC_SIGN_IN = 9002
        var username: String = ""
    }

    @RequiresApi(Build.VERSION_CODES.N)
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_login, container, false)

        binding.pushToCloud.setOnClickListener { pushToCloud() }
        binding.pullFromCloud.setOnClickListener { pullFromCloud() }

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

    private fun updateLoginState(callback: (() -> Unit)? = null) {
        val currentUser = mAuth.currentUser
        if (currentUser != null) {
            binding.loggedOutSection.visibility = View.GONE
            binding.loggedInSection.visibility = View.VISIBLE
            FirebaseUserDao.getUserInfo(currentUser.uid) {
                binding.usernameInput.setText(it.name)
                if (it.name == currentUser.email) {
                    binding.currentUserInfo.text = "Logged in as\n${it.name}"
                    binding.setUsernameWarning.visibility = View.VISIBLE
                    binding.setUsernameButton.text = "Set Username"
                } else {
                    binding.currentUserInfo.text = "Logged in as\n${it.name}\n(${currentUser.email})"
                    binding.setUsernameWarning.visibility = View.GONE
                    binding.setUsernameButton.text = "Change Username"
                }
                username = binding.usernameInput.text.toString()
            }
        } else {
            binding.loggedOutSection.visibility = View.VISIBLE
            binding.loggedInSection.visibility = View.GONE
        }
    }

    private fun pushToCloud() {
        val msg = """
            This will push all the shaders on your device to the cloud.
            If any with the same name already exist on the cloud, they will be overwritten.
            Proceed?
        """.trimIndent()
        showAlert("Push To Cloud", msg) {
            CoroutineScope(Dispatchers.IO).launch {
                val localShaders = CameraActivity.shaderDao.getUserShaders()
                localShaders.forEach { localShader ->
                    FirebaseShaderDao.insertAll(localShader)
                }
                requireActivity().runOnUiThread {
                    Toast.makeText(requireActivity(), "Finished", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.N)
    private fun pullFromCloud () {
        val msg = """
            This will pull all the shaders on the cloud to your device.
            If any with the same name already exist on your device, they will be overwritten.
            Proceed?
        """.trimIndent()
        showAlert("Pull From Cloud", msg) {
            FirebaseShaderDao.getUserShaders(mAuth.currentUser!!.uid) { cloudShaders ->
                CoroutineScope(Dispatchers.IO).launch {
                    val localShaders = CameraActivity.shaderDao.getUserShaders()
                    cloudShaders.forEach { name, shaderAttributes ->
                        val matchingLocalShader = localShaders.find { localShader ->
                            localShader.name == name
                        }
                        val shader = Shader(
                            uid = 0,
                            name = name,
                            shaderMainText = shaderAttributes.shaderMainText,
                            paramsJson = Json.encodeToString(shaderAttributes.params),
                            userUid = mAuth.currentUser!!.uid,
                            isPublic = shaderAttributes.isPublic
                        )
                        if (matchingLocalShader != null) {
                            shader.uid = matchingLocalShader.uid
                            CameraActivity.shaderDao.update(shader)
                        } else {
                            CameraActivity.shaderDao.insertAll(shader)
                        }
                    }
                }
            }
            requireActivity().runOnUiThread {
                Toast.makeText(requireActivity(), "Finished", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun showAlert(title: String, msg: String, callback: () -> Unit) {
        AlertDialog.Builder(requireActivity())
            .setTitle(title)
            .setMessage(msg)
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                callback()
            }
            .setNegativeButton(android.R.string.cancel, null).show()
    }

    private fun setOnClickListeners() {
        binding.loginButton.setOnClickListener {
            signIn()
        }
        binding.logoutButton.setOnClickListener {
            signOut()
            Toast.makeText(requireActivity(), "Logged out", Toast.LENGTH_SHORT).show()
        }
        binding.setUsernameButton.setOnClickListener {
            val username = binding.usernameInput.text.toString()
            ensureValidUsername(username) {
                FirebaseUserDao.updateUserInfo(username)
                updateLoginState()
                Toast.makeText(requireActivity(), "Saved username", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun ensureValidUsername(username: String, callback: () -> Unit)   {
        if (username.isEmpty()) {
            Log.e("DEBUG", "username empty!")
            return Toast.makeText(requireActivity(), "Username cant be empty", Toast.LENGTH_SHORT).show()
        }
        FirebaseUserDao.usernameAlreadyTaken(username) {
            Log.e("DEBUG", "username taken? $it")
            if (it) {
                Toast.makeText(requireActivity(), "Username is taken", Toast.LENGTH_SHORT).show()
            } else { callback() }
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

    private fun loggedIn() {
        updateLoginState() {
            // This doesn't work. wtf.
            requireActivity().runOnUiThread {
                showAlert("Logged In", "If you created/edited any shaders while offline, use the 'Push To Cloud' button to back them up") { }
            }
            FirebaseUserDao.updateUserInfo(binding.usernameInput.text.toString())
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
                                        loggedIn()
                                        // Get new FCM registration token
//                                        val token = fcmTask.result
//                                        Log.e(TAG, "fcm token: $token")
                                    } else {
                                        Log.e(TAG, "could not get token")
                                    }
                                }
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