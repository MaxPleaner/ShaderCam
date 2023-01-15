package com.skamz.shadercam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.defaultShaderMainText

class EditorActivity : AppCompatActivity(){
    lateinit var textInput: TextInputEditText
    lateinit var nameInput: TextInputEditText

    private fun saveShader(name: String, shaderMainText: String) {
        // TODO: Validate shader and show errors.
        val shaderAttributes = ShaderAttributes(name, shaderMainText, mutableListOf())
        CameraActivity.shaderAttributes = shaderAttributes;
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setValuesFromActiveShader()
    }

    private fun setValuesFromActiveShader() {
        textInput.setText(CameraActivity.shaderAttributes.shaderMainText);
        nameInput.setText(CameraActivity.shaderAttributes.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val cameraLink = findViewById<Button>(R.id.camera_link);
        val saveButton = findViewById<Button>(R.id.save)
        textInput = findViewById<TextInputEditText>(R.id.text_input);
        nameInput = findViewById<TextInputEditText>(R.id.name_input);

        setValuesFromActiveShader()

        cameraLink.setOnClickListener {
            saveShader(nameInput.text.toString(), textInput.text.toString())

            val cameraActivityIntent = Intent(this, CameraActivity::class.java)
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(cameraActivityIntent)
        }

        saveButton.setOnClickListener {
            saveShader(nameInput.text.toString(), textInput.text.toString())
        }
    }
}