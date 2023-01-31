package com.skamz.shadercam.ui.fragments

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.FragmentSplashBinding

class SplashFragment : Fragment() {

    private val TAG = "SplashFragment"

    private lateinit var mAuth : FirebaseAuth

    private lateinit var binding: FragmentSplashBinding

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        binding = DataBindingUtil.inflate(inflater,R.layout.fragment_splash, container, false)

        mAuth = FirebaseAuth.getInstance()

        val user = mAuth.currentUser

        //if user is not signed is directed to the login page but directed to dashboard if signed in
        Handler(Looper.getMainLooper()).postDelayed({
            lifecycleScope.launchWhenResumed {
                if (user != null){

                }else {
                }
            }
        }, 3000)
        // Inflate the layout for this fragment

        return binding.root
    }

}