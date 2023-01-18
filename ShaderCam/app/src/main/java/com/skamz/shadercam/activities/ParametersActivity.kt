package com.skamz.shadercam.activities

import android.content.Intent
import android.opengl.Visibility
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R

class ParametersActivity : AppCompatActivity() {
    private lateinit var floatLayout: LinearLayout
    private lateinit var colorLayout: LinearLayout
    private lateinit var colorRB: RadioButton
    private lateinit var floatRB: RadioButton
    private lateinit var defaultFloatValue: TextInputEditText
    private lateinit var cancelBtn: TextView
    private lateinit var saveBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parameters)

        init()
    }

    private fun init() {
            colorLayout = findViewById(R.id.colorLayout)
            floatLayout = findViewById(R.id.floatLayout)
            colorRB = findViewById(R.id.colorRB)
            floatRB = findViewById(R.id.floatRb)
            defaultFloatValue = findViewById(R.id.defaultFloatValue)
            saveBtn = findViewById(R.id.saveParameters)
            cancelBtn = findViewById(R.id.cancelParameters)

            colorRB.isChecked = true
            colorLayout.visibility = View.VISIBLE

            floatRB.isChecked = false
            floatLayout.visibility = View.GONE

            setOnClickListeners()
    }

    private fun setOnClickListeners() {
        colorRB.setOnClickListener { toggleParameterType(colorRB) }
        floatRB.setOnClickListener { toggleParameterType(floatRB) }

        saveBtn.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        cancelBtn.setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }
    }

    private fun toggleParameterType(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                R.id.colorRB ->
                    if (checked) {
                        floatLayout.visibility = View.GONE
                        colorLayout.visibility = View.VISIBLE
                    }
                R.id.floatRb ->
                    if (checked) {
                        floatLayout.visibility = View.VISIBLE
                        colorLayout.visibility = View.GONE
                    }
            }
        }
    }
}