package com.skamz.shadercam.activities

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.shaders.util.ShaderAttributes

class EditorActivity : AppCompatActivity(){
    companion object {
        var defaultShaderText:String = """
            vec4 color = texture2D(sTexture, vTextureCoord);
            gl_FragColor = color;
        """.trimIndent()

        fun buildShaderAttributes(shaderMainText: String): ShaderAttributes {
            return ShaderAttributes(
                "CHANGE ME",
                shaderMainText,
                mutableListOf()
            )
        }
    }

    fun saveShader(shaderMainText: String) {
        // TODO: Validate shader and show errors.
        val shaderAttributes = buildShaderAttributes(shaderMainText)
        CameraActivity.shaderAttributes = shaderAttributes;
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        val cameraLink = findViewById<Button>(R.id.camera_link);

        val textInput = findViewById<TextInputEditText>(R.id.text_input);
        textInput.setText(defaultShaderText);

        val cameraActivityIntent = Intent(this, CameraActivity::class.java)
        cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        cameraLink.setOnClickListener {
            saveShader(textInput.text.toString())
            startActivity(cameraActivityIntent)
        }

        val saveButton = findViewById<Button>(R.id.save)

        saveButton.setOnClickListener {
            saveShader(textInput.text.toString())
        }
    }
}