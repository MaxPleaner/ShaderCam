package com.skamz.shadercam.ui.fragments

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.NavHostFragment
import androidx.navigation.fragment.findNavController
import com.google.firebase.auth.FirebaseAuth
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.FragmentSplashBinding
import com.skamz.shadercam.ui.activities.CameraActivity
import com.skamz.shadercam.ui.activities.EditorActivity

class SplashFragment : Fragment() {
    private lateinit var mAuth : FirebaseAuth
    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_splash, container, false)

        mAuth = FirebaseAuth.getInstance()
//        val user = mAuth.currentUser

        // if user is not signed in, they're directed to the login page
        // Otherwise, they go directly to the dashboard.
        // The delay here is not required; it's just so they see the landing page for a second.
//        Handler(Looper.getMainLooper()).postDelayed({
        lifecycleScope.launchWhenResumed {
//                if (user != null){
//                   goToCameraActivity()
//                } else {
                findNavController().navigate(
                    R.id.action_splashFragment_to_loginFragment
                )
//                }
        }
//        }, 1500)

        return binding.root
    }

    private fun goToCameraActivity() {
        val intent = Intent(requireActivity(), CameraActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.putExtra("KEEP_VALUES", true)
        startActivity(intent)
    }
}