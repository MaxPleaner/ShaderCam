package com.skamz.shadercam.ui.activities

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import com.skamz.shadercam.R
import com.skamz.shadercam.databinding.ActivityOnboardingBaseBinding

class OnboardingBaseActivity : AppCompatActivity() {
    private var backPressedTime:Long = 0
    lateinit var backToast: Toast
    private lateinit var binding : ActivityOnboardingBaseBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = DataBindingUtil.setContentView(this,R.layout.activity_onboarding_base)

        findViewById<Button>(R.id.camera_link).setOnClickListener {
            val cameraActivityIntent = Intent(this, CameraActivity::class.java)
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(cameraActivityIntent)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
            super.onActivityResult(requestCode, resultCode, data)
            if (getForegroundFragment() != null) {
                getForegroundFragment()!!.onActivityResult(requestCode, resultCode, data)
            }
    }

    private fun getForegroundFragment(): Fragment? {
        val navHostFragment = supportFragmentManager.findFragmentById(R.id.base)
        return navHostFragment?.childFragmentManager?.fragments?.get(0)
    }

    override fun onBackPressed() {
            backToast = Toast.makeText(this, "Press back again to leave the app.", Toast.LENGTH_LONG)
            if (backPressedTime + 2000 > System.currentTimeMillis()) {
                backToast.cancel()
                super.onBackPressed()
                finishAffinity()
                return
            } else {
                backToast.show()
            }
            backPressedTime = System.currentTimeMillis()
    }

}