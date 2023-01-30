package com.skamz.shadercam.ui.activities

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Paint
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.logic.database.Shader
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.Shaders
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

    private fun saveShaderWithoutPersistance() {
        val name = nameInput.text.toString()
        val shaderMainText = shaderTextInput.text.toString()
        val shaderAttributes = ShaderAttributes(name, shaderMainText, parameters)

        CameraActivity.shaderAttributes = shaderAttributes
    }

    private fun saveShader(callback: (() -> Unit)? = null) {
        // TODO: Validate shader and show errors.
        val name = nameInput.text.toString()
        val shaderMainText = shaderTextInput.text.toString()
        val shaderAttributes = ShaderAttributes(name, shaderMainText, parameters)

        CameraActivity.shaderAttributes = shaderAttributes
        val context = this
        CoroutineScope(Dispatchers.IO).launch {
            var record = CameraActivity.shaderDao.findByName(name)
            if (record == null) {
                val paramsJson = Json.encodeToString(parameters)

                record = Shader(0, name, shaderMainText, paramsJson)
                CameraActivity.shaderDao.insertAll(record)
            } else {
                record.shaderMainText = shaderMainText
                record.paramsJson = Json.encodeToString(parameters)

                CameraActivity.shaderDao.update(record)
            }
            runOnUiThread { Toast.makeText( context, "Saved", Toast.LENGTH_SHORT).show() }
            if (callback != null) {
                // NOTE: The callback will execute in the background thread.
                callback()
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

    private fun isTemplateShader(): Boolean {
        return Shaders.all.map { it.name }.contains(nameInput.text.toString())
    }

    private fun toggleTextStrikethrough(textView: TextView, strikethrough: Boolean) {
        if (strikethrough) {
            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        } else {
            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
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

        nameInput.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable) {
                val isTemplate = isTemplateShader()
                toggleTextStrikethrough(saveButton, isTemplate)
                toggleTextStrikethrough(deleteButton, isTemplate)
            }
            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
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
            cameraLinkOnClick()
        }

        saveButton.setOnClickListener {
            saveButtonOnClick()
        }

        deleteButton.setOnClickListener {
            deleteButtonOnClick()
        }
    }

    private fun saveButtonOnClick() {
        if (isTemplateShader()) {
            val msg = """
                    Template shaders cannot be altered.
                    Change the name in order to save.
                """.trimIndent()
            return Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        if (nameInput.text.toString().isEmpty()) {
            return Toast.makeText(this, "Shader name cannot be blank", Toast.LENGTH_SHORT).show()
        }
        saveShader()
    }

    private fun cameraLinkOnClick () {
        if (nameInput.text.toString().isEmpty()) {
            return Toast.makeText(this, "Shader name cannot be blank", Toast.LENGTH_SHORT).show()
        }
        if (isTemplateShader()) {
            val msg = """
                    Template shaders cannot be altered.
                    Changes will not be persisted.
                    Change the name in order to save as a new shader.
                """.trimIndent()
            AlertDialog.Builder(this)
                .setTitle("Shader will not be saved")
                .setMessage(msg)
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    saveShaderWithoutPersistance()
                    goToCameraActivity()
                }
                .setNegativeButton(android.R.string.cancel, null).show()
        } else {
            saveShader {
                goToCameraActivity()
            }
        }
    }

    private fun deleteButtonOnClick () {
        val context = this
        if (isTemplateShader()) {
            Toast.makeText(context, "Cannot delete template shader", Toast.LENGTH_SHORT).show()
        } else {
            CoroutineScope(Dispatchers.IO).launch {
                val record = CameraActivity.shaderDao.findByName(nameInput.text.toString())
                if (record == null) {
                    val msg = "No shader with this name is saved."
                    runOnUiThread { Toast.makeText(context, msg, Toast.LENGTH_SHORT).show() }
                } else {
                    val msg = "Really delete shader ${nameInput.text.toString()}?"
                    runOnUiThread {
                        AlertDialog.Builder(context)
                            .setTitle("Confirm Delete")
                            .setMessage(msg)
                            .setIcon(android.R.drawable.ic_dialog_alert)
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                CoroutineScope(Dispatchers.IO).launch {
                                    CameraActivity.shaderDao.delete(record)
                                    goToCameraActivity { intent ->
                                        intent.putExtra(
                                            "DeletedShader",
                                            nameInput.text.toString()
                                        )
                                    }
                                }
                            }
                            .setNegativeButton(android.R.string.cancel, null).show()
                    }

                }
            }
        }
    }

    private fun goToCameraActivity(callback: ((Intent) -> Unit)? = null) {
        val cameraActivityIntent = Intent(this, CameraActivity::class.java)
        cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        if (callback != null) { callback(cameraActivityIntent) }
        startActivity(cameraActivityIntent)
    }
}
