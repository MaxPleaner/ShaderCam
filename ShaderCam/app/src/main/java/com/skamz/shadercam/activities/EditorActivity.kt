package com.skamz.shadercam.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.database.Shader
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.Shaders
import io.github.rosemoe.sora.widget.CodeEditor
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class EditorActivity : AppCompatActivity(){

    private lateinit var shaderTextInput: CodeEditor
    private lateinit var nameInput: TextInputEditText
    private lateinit var addParameterBtn: TextView
    private lateinit var parametersListView: ListView

    companion object {
        var parameters: MutableList<ShaderParam> = mutableListOf()
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
                val paramsJson = Json.encodeToString(parameters)
                record = Shader(0, name, shaderMainText, paramsJson)
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
        setValuesFromActiveShader(intent)
    }

    private fun setValuesFromActiveShader(intent: Intent? = null) {
        if (intent?.getBooleanExtra("KEEP_VALUES", false) != true) {
            shaderTextInput.setText(CameraActivity.shaderAttributes.shaderMainText)
            nameInput.setText(CameraActivity.shaderAttributes.name)
            parameters = CameraActivity.shaderAttributes.params
        }
        setParametersListText()
    }

    @SuppressLint("SetTextI18n")
    private fun setParametersListText() {
        val arrayAdapter: ArrayAdapter<*>

        val params = parameters.mapIndexed { index, shaderParam ->
            "${index + 1}) ${shaderParam.paramName} (${shaderParam.paramType})"
        }.toTypedArray()

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
        val deleteButton = findViewById<Button>(R.id.delete)

        addParameterBtn = findViewById(R.id.addParameters)
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

        parametersListView.onItemClickListener =
            AdapterView.OnItemClickListener { _, _, position, _ ->
//                val name = parametersListView.getItemAtPosition(position) as String
                val name = parameters[position].paramName
                val i = Intent(this, ParametersActivity::class.java)
                i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                i.putExtra("editParamName", name);
                startActivity(i)
            }

        cameraLink.setOnClickListener {
            saveShader()
            goToCameraActivity()
        }

        saveButton.setOnClickListener {
            saveShader {
                goToCameraActivity()
            }
        }

        deleteButton.setOnClickListener {
            val context = this
            if (Shaders.all.map { it.name }.contains(nameInput.text.toString())) {
                Toast.makeText(context, "Cannot delete template shader", Toast.LENGTH_SHORT).show()
            } else {
                CoroutineScope(Dispatchers.IO).launch {
                    val record = CameraActivity.shaderDao.findByName(nameInput.text.toString())
                    if (record == null) {
                        runOnUiThread {
                            Toast.makeText(
                                context,
                                "No shader with this name is saved.",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    } else {
                        CameraActivity.shaderDao.delete(record)
                        goToCameraActivity { intent ->
                            intent.putExtra("DeletedShader", nameInput.text.toString())
                        }
                    }
                }
            }
        }
    }

        fun goToCameraActivity(callback: ((Intent) -> Unit)? = null) {
            val cameraActivityIntent = Intent(this, CameraActivity::class.java)
            cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            if (callback != null) { callback(cameraActivityIntent) }
            startActivity(cameraActivityIntent)
        }
}