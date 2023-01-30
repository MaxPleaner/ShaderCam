package com.skamz.shadercam.ui.activities

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.skamz.shadercam.R

class OnboardingBaseActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_onboarding_base)
    }
}