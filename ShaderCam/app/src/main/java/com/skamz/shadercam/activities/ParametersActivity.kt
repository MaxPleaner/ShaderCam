package com.skamz.shadercam.activities

import android.content.Intent
import android.content.SharedPreferences.Editor
import android.graphics.Color
import android.os.Bundle
import android.text.InputType
import android.util.Log
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

    private var defaultColorValueInitial = Color.BLUE
    var defaultColorValue: Int = defaultColorValueInitial

    private lateinit var cancelBtn: TextView
    private lateinit var saveBtn: TextView
    private lateinit var deleteBtn: TextView

    // mode can be "new" or "update" and changes the saving behavior
    private var mode = "new"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parameters)
        init()
        setupEditMode(intent)
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setupEditMode(intent)
    }

     fun setupEditMode(intent: Intent?) {
        val editParamName = intent?.getStringExtra("editParamName");
        if (editParamName != null) {
            mode = "edit"
            saveBtn.text = "Update"
            nameInput.inputType = InputType.TYPE_NULL
            deleteBtn.visibility = View.VISIBLE

            val param = EditorActivity.parameters.find {
                it.paramName == editParamName
            } ?: return resetValues()

            setType(param.paramType)

            nameInput.setText(param.paramName)

            when (param.paramType) {
                "float" -> setFloatValues(param as FloatShaderParam)
                "color" -> setColorValues(param as ColorShaderParam)
            }
        } else {
            mode = "new"
            saveBtn.text = "Save"
            nameInput.inputType = InputType.TYPE_CLASS_TEXT
            deleteBtn.visibility = View.GONE
            resetValues()
        }
    }

    private fun setFloatValues(param: FloatShaderParam) {
        defaultFloatInput.setText(param.default.toString())
        minFloatInput.setText(param.min.toString())
        maxFloatInput.setText(param.max.toString())
    }

    private fun setColorValues(param: ColorShaderParam) {
        defaultColorValue = param.default
        // TODO: set color of picker.
    }

    private fun setType(newType: String) {
        Log.i("DEBUG", "setting type ${newType}")
        type = newType
        when (type) {
            "float" -> {
                colorRB.isChecked = false
                colorLayout.visibility = View.GONE

                floatRB.isChecked = true
                floatLayout.visibility = View.VISIBLE
            }
            "color" -> {
                colorRB.isChecked = true
                colorLayout.visibility = View.VISIBLE

                floatRB.isChecked = false
                floatLayout.visibility = View.GONE
            }
        }
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
            deleteBtn = findViewById(R.id.deleteParameter)

            setType("float")

            setOnClickListeners()
    }

    private fun setOnClickListeners() {
        colorRB.setOnClickListener { toggleParameterType(colorRB) }
        floatRB.setOnClickListener { toggleParameterType(floatRB) }

        saveBtn.setOnClickListener {
            save()
            goBackToEditor()
        }

        cancelBtn.setOnClickListener {
            goBackToEditor()
        }

        deleteBtn.setOnClickListener {
            deleteParam()
            goBackToEditor()
        }
    }

    private fun goBackToEditor() {
        val intent = Intent(this, EditorActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        intent.putExtra("KEEP_VALUES", true)
        startActivity(intent)
    }

    private fun save() {
        when (mode) {
            "new" -> saveNewParam()
            "edit" -> updateParam()
        }
    }

    private fun saveNewParam() {
        when (type) {
            "float" -> {
                val shaderParam = FloatShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultFloatInput.text.toString().toFloat(),
                    min = minFloatInput.text.toString().toFloat(),
                    max = maxFloatInput.text.toString().toFloat()
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

    private fun updateParam() {
        EditorActivity.parameters.forEachIndexed { index, param ->
            param.takeIf { it.paramName == nameInput.text.toString()}?.let {
                var updatedParam = param
                when (type) {
                    "float" -> {
                        updatedParam = updatedParam as FloatShaderParam
                        updatedParam.default = defaultFloatInput.text.toString().toFloat()
                        updatedParam.min = minFloatInput.text.toString().toFloat()

                        updatedParam.max = maxFloatInput.text.toString().toFloat()
                    }
                    "color" -> {
                        updatedParam = updatedParam as ColorShaderParam
                        updatedParam.default = defaultColorValue
                    }
                }
                EditorActivity.parameters[index] = updatedParam
            }
        }
    }

    private fun deleteParam() {
        val param = EditorActivity.parameters.find {
            it.paramName == nameInput.text.toString()
        }
        EditorActivity.parameters.remove(param)
    }

    private fun resetValues() {
        nameInput.setText("")
        defaultFloatInput.setText(defaultFloatValueInitial.toString())
        maxFloatInput.setText(maxFloatValueInitial.toString())
        minFloatInput.setText(minFloatValueInitial.toString())
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