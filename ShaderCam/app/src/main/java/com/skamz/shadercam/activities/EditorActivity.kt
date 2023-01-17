package com.skamz.shadercam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.shaders.util.ShaderAttributes

import io.github.rosemoe.sora.widget.CodeEditor

class EditorActivity : AppCompatActivity() {
    lateinit var textInput: CodeEditor
   // lateinit var nameInput: TextInputEditText
    lateinit var addParameter: TextView

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
//        nameInput.setText(CameraActivity.shaderAttributes.name)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val cameraLink = findViewById<Button>(R.id.camera_link);
        val saveButton = findViewById<Button>(R.id.save)
        addParameter =  findViewById(R.id.addParameters)
        textInput = findViewById(R.id.text_input);

        addParameter.setOnClickListener {
            val i = Intent(this, ParametersActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(i)
        }
        // nameInput = findViewById(R.id.name_input);

//        val grammarDefinition = DefaultGrammarDefinition.withLanguageConfiguration()
//        textInput.setEditorLanguage(TextMateLanguage.create("GLSL", true))

        setValuesFromActiveShader()

        cameraLink.setOnClickListener {
           // saveShader(nameInput.text.toString(), textInput.text.toString())

            val cameraActivityIntent = Intent(this, CameraActivity::class.java)
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(cameraActivityIntent)
        }

        saveButton.setOnClickListener {
        //    saveShader(nameInput.text.toString(), textInput.text.toString())
        }
    }
}