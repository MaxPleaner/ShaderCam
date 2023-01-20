package com.skamz.shadercam.activities

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.database.Shader
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.ShaderParam
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.w3c.dom.Text

class EditorActivity : AppCompatActivity(){

    private lateinit var shaderTextInput: CodeEditor
    private lateinit var nameInput: TextInputEditText
    private lateinit var addParameterBtn: TextView
    private lateinit var parametersListView: ListView

    companion object {
        val parameters: MutableList<ShaderParam> = mutableListOf()
    }

    private fun saveShader(callback: (() -> Unit)? = null) {
        // TODO: Validate shader and show errors.
        val name = nameInput.text.toString()
        val shaderMainText = shaderTextInput.text.toString()
        val shaderAttributes = ShaderAttributes(name, shaderMainText, parameters)

        CameraActivity.shaderAttributes = shaderAttributes

        CoroutineScope(Dispatchers.IO).launch {
            var record = CameraActivity.shaderDao.findByName(name)
            if (record == null) {
                Log.i("DEBUG", "record is null. ignore lint error.")
                record = Shader(0, name, shaderMainText, "")
                CameraActivity.shaderDao.insertAll(record)
            } else {
                record.shaderMainText = shaderMainText
                record.paramsJson = ""
                CameraActivity.shaderDao.update(record)
                if (callback != null) {
                    // NOTE: The callback will execute in the background thread.
                    callback()
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setValuesFromActiveShader()
    }

    private fun setValuesFromActiveShader() {
        shaderTextInput.setText(CameraActivity.shaderAttributes.shaderMainText)
        nameInput.setText(CameraActivity.shaderAttributes.name)
        setParametersList()
    }

    private fun setParametersList() {
        val arrayAdapter: ArrayAdapter<*>
        val params = parameters.map { "${it.paramName} (${it.type})" }.toTypedArray()
        arrayAdapter = ArrayAdapter(this,
            R.layout.parameter_list_item, params)
        parametersListView.adapter = arrayAdapter

        val paramsTitle = findViewById<TextView>(R.id.paramsTitle)
        if (parameters.isNotEmpty()) {
            paramsTitle.text = "Parameters:"
        } else {
            paramsTitle.text = "Parameters: None"
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val cameraLink = findViewById<Button>(R.id.camera_link)
        val saveButton = findViewById<Button>(R.id.save)
        addParameterBtn =  findViewById(R.id.addParameters)
        shaderTextInput = findViewById(R.id.text_input)
        parametersListView = findViewById(R.id.parameters_list_view)

        addParameterBtn.setOnClickListener {
            val i = Intent(this, ParametersActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(i)
        }
         nameInput = findViewById(R.id.name_input)

//        val grammarDefinition = DefaultGrammarDefinition.withLanguageConfiguration()
//        shaderTextInput.setEditorLanguage(TextMateLanguage.create("GLSL", true))

        setValuesFromActiveShader()

        cameraLink.setOnClickListener {
            saveShader()

            val cameraActivityIntent = Intent(this, CameraActivity::class.java)
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(cameraActivityIntent)
        }

        saveButton.setOnClickListener {
            saveShader {
                val cameraActivityIntent = Intent(this, CameraActivity::class.java)
                cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                startActivity(cameraActivityIntent)
            }
        }

    }
}