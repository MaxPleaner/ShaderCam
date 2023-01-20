package com.skamz.shadercam.activities

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.shaders.util.ColorShaderParam
import com.skamz.shadercam.shaders.util.FloatShaderParam

class ParametersActivity : AppCompatActivity() {
    private lateinit var floatLayout: LinearLayout
    private lateinit var colorLayout: LinearLayout

    private lateinit var colorRB: RadioButton
    private lateinit var floatRB: RadioButton

    private lateinit var nameInput: TextInputEditText
    private lateinit var defaultFloatInput: TextInputEditText
    private lateinit var maxFloatInput: TextInputEditText
    private lateinit var minFloatInput: TextInputEditText

    private var type: String = "float"

    private var defaultFloatValueInitial: Float = 1f
    private var maxFloatValueInitial: Float = 1f
    private var minFloatValueInitial: Float = 0f

    private var defaultFloatValue: Float = defaultFloatValueInitial
    private var maxFloatValue: Float = maxFloatValueInitial
    private var minFloatValue: Float = minFloatValueInitial

    private var defaultColorValueInitial = Color.BLUE
    var defaultColorValue: Int = defaultColorValueInitial

    private lateinit var cancelBtn: TextView
    private lateinit var saveBtn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parameters)

        init()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
    }

    private fun init() {
            colorLayout = findViewById(R.id.colorLayout)
            floatLayout = findViewById(R.id.floatLayout)

            colorRB = findViewById(R.id.colorRB)
            floatRB = findViewById(R.id.floatRb)

            nameInput = findViewById(R.id.name_input)

            defaultFloatInput = findViewById(R.id.defaultFloatInput)
            minFloatInput = findViewById(R.id.minFloatInput)
            maxFloatInput = findViewById(R.id.maxFloatInput)

            saveBtn = findViewById(R.id.saveParameters)
            cancelBtn = findViewById(R.id.cancelParameters)

            colorRB.isChecked = false
            colorLayout.visibility = View.GONE

            floatRB.isChecked = true
            floatLayout.visibility = View.VISIBLE

            setOnClickListeners()
    }

    private fun setOnClickListeners() {
        colorRB.setOnClickListener { toggleParameterType(colorRB) }
        floatRB.setOnClickListener { toggleParameterType(floatRB) }

        saveBtn.setOnClickListener {
            save()
            resetValues()
            goBackToEditor()
        }

        cancelBtn.setOnClickListener {
            resetValues()
            goBackToEditor()
        }
    }

    private fun goBackToEditor() {
        val intent = Intent(this, EditorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        startActivity(intent)
    }

    private fun save() {
        when (type) {
            "float" -> {
                val shaderParam = FloatShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultFloatValue,
                    min = minFloatValue,
                    max = maxFloatValue
                )
                EditorActivity.parameters.add(shaderParam)
            }
            "color" -> {
                val shaderParam = ColorShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultColorValue
                )
                EditorActivity.parameters.add(shaderParam)
            }
        }
    }

    private fun resetValues() {
        defaultFloatValue = defaultFloatValueInitial
        maxFloatValue = maxFloatValueInitial
        minFloatValue = minFloatValueInitial
        defaultColorValue = defaultColorValueInitial
    }

    private fun toggleParameterType(view: View) {
        if (view is RadioButton) {
            val checked = view.isChecked
            when (view.getId()) {
                R.id.colorRB ->
                    if (checked) {
                        type = "color"
                        floatLayout.visibility = View.GONE
                        colorLayout.visibility = View.VISIBLE
                    }
                R.id.floatRb ->
                    if (checked) {
                        type = "float"
                        floatLayout.visibility = View.VISIBLE
                        colorLayout.visibility = View.GONE
                    }
            }
        }
    }
}