package com.skamz.shadercam

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText

class EditorActivity : AppCompatActivity(){
    companion object {
        var defaultShaderText:String = """
            vec4 color = texture2D(sTexture, vTextureCoord);
            gl_FragColor = color;
        """.trimIndent()

        var defaultShaderText2:String = """
            vec4 color = texture2D(sTexture, vTextureCoord);
            gl_FragColor = 3.0 * color;
        """.trimIndent()

        var shaderContentToken:String = "__SHADER_CONTENT_TOKEN_6d9319__"

        var shaderBoiler:String = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTexture;
            varying vec2 vTextureCoord;
            void main() {
              $shaderContentToken
            }
       """.trimIndent()

        lateinit var cameraActivityIntent: Intent

        var customShader: String? = null

        fun buildShader(shaderText: String): String {
            return shaderBoiler.replace(shaderContentToken, shaderText)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)
        val cameraLink = findViewById<Button>(R.id.camera_link);

        cameraActivityIntent = Intent(this, CameraActivity::class.java)
        cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);

        cameraLink.setOnClickListener {
            startActivity(cameraActivityIntent)
        }

        val textInput = findViewById<TextInputEditText>(R.id.text_input);
        textInput.setText(defaultShaderText2);

        val saveButton = findViewById<Button>(R.id.save)

        saveButton.setOnClickListener {
            val shaderText = buildShader(textInput.text.toString())
            cameraActivityIntent.putExtra("shader", shaderText);
        }
    }
}