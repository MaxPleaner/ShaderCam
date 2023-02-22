package com.skamz.shadercam.ui.activities

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.constraintlayout.widget.ConstraintLayout
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.auth.FirebaseAuth
import com.skamz.shadercam.R
import com.skamz.shadercam.logic.database.Shader
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.Shaders
import com.skamz.shadercam.logic.shaders.util.TextureShaderParam
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
    private var isPublic: Boolean = false
    private var isFullScreen = false
    private var normalLayoutParams: ConstraintLayout.LayoutParams? = null

    companion object {
        var parameters: MutableList<ShaderParam> = mutableListOf()
    }

    private fun saveShaderWithoutPersistance() {
        val name = nameInput.text.toString()
        val shaderMainText = shaderTextInput.text.toString()
        val shaderAttributes = ShaderAttributes(name, shaderMainText, parameters, isPublic = isPublic)

        CameraActivity.shaderAttributes = shaderAttributes
    }

    private fun performSaveCoroutine(name: String, shaderMainText: String, context: Context, callback: (() -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            var record = CameraActivity.shaderDao.findByName(name)
            if (record == null) {
                val paramsJson = Json.encodeToString(parameters)

                record = Shader(0, name, shaderMainText, paramsJson)
                CameraActivity.shaderDao.insertAll(record)
            } else {
                record.shaderMainText = shaderMainText
                record.paramsJson = Json.encodeToString(parameters)
                record.isPublic = isPublic

                CameraActivity.shaderDao.update(record)
            }

            runOnUiThread { Toast.makeText(context, "Saved", Toast.LENGTH_SHORT).show() }
            if (callback != null) {
                callback()
            }
        }
    }

    private fun saveShader(callback: (() -> Unit)? = null) {
        val name = nameInput.text.toString()
        val shaderMainText = shaderTextInput.text.toString()
        val shaderAttributes = ShaderAttributes(name, shaderMainText, parameters, isPublic = isPublic)

        CameraActivity.shaderAttributes = shaderAttributes
        if (FirebaseAuth.getInstance().currentUser == null) {
            AlertDialog.Builder(this)
                .setTitle("Not logged in")
                .setMessage("Without logging in, shaders will be saved to your device only.")
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    performSaveCoroutine(name, shaderMainText, this, callback)
                }
                .setNegativeButton(android.R.string.cancel, null).show()
        } else {
            performSaveCoroutine(name, shaderMainText, this, callback)
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
            parameters = CameraActivity.shaderAttributes.params.toMutableList()
            isPublic = CameraActivity.shaderAttributes.isPublic
            findViewById<ToggleButton>(R.id.public_private_toggle).isChecked = isPublic
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
        // This really isn't important. Disabling.
//        if (strikethrough) {
//            textView.paintFlags = textView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
//        } else {
//            textView.paintFlags = textView.paintFlags and Paint.STRIKE_THRU_TEXT_FLAG.inv()
//        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_editor)

        val cameraLink = findViewById<Button>(R.id.camera_link)
        val saveButton = findViewById<Button>(R.id.save)
        val deleteButton = findViewById<Button>(R.id.delete)
        val rootView = findViewById<ConstraintLayout>(R.id.rootView)
        val nameInputWrapper = findViewById<TextInputLayout>(R.id.name_input_wrapper)


        addParameterBtn = findViewById(R.id.addParameters)
        shaderTextInput = findViewById(R.id.text_input)
        parametersListView = findViewById(R.id.parameters_list_view)

        findViewById<ToggleButton>(R.id.public_private_toggle).setOnCheckedChangeListener { buttonView, isChecked ->
            isPublic = isChecked
        }

        setLayoutChange()

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

    private fun setLayoutChange() {

        val text_input = findViewById<CodeEditor>(R.id.text_input)
        // Store the normal layout parameters for restoring later
        normalLayoutParams = text_input.layoutParams as ConstraintLayout.LayoutParams

        // Set click listener for the CodeEditor view
        text_input.setOnClickListener {
            if (!isFullScreen) {
                // Enter fullscreen mode
                val params = ConstraintLayout.LayoutParams(
                    ConstraintLayout.LayoutParams.MATCH_PARENT,
                    ConstraintLayout.LayoutParams.MATCH_PARENT
                )
                text_input.layoutParams = params
                isFullScreen = true
            } else {
                // Exit fullscreen mode
                text_input.layoutParams = normalLayoutParams
                isFullScreen = false
            }
        }
    }

    private fun invalidShaderName (): Boolean {
        // shader names cannot contain . # $ [ ] because of Firebase path rules.
        return nameInput.text.toString().matches(Regex("[.#\$\\[\\]]"))
    }

    private fun isPublicShaderWithLocalTexture(): Boolean {
        if (!isPublic) { return false }
        val textureParams = parameters.filter { it.paramType == "texture" }
        val hasLocalTexture = textureParams.any {
            val uri = Uri.parse((it as TextureShaderParam).default)
            !listOf("http", "https").contains(uri.scheme)
        }
        return hasLocalTexture
    }

    private fun saveButtonOnClick() {
        if (invalidShaderName()) {
            val msg = """
                Shader names cannot contain the characters . # $ [ ]
            """.trimIndent()
            return Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        }
        if (isPublicShaderWithLocalTexture()) {
            AlertDialog.Builder(this)
                .setTitle("Canot save")
                .setMessage("""
                    Public shaders cannot have local texture paths.
                    Change the texture path to a URL, or make the shader private.
                """.trimIndent())
                .setIcon(android.R.drawable.ic_dialog_alert)
                .setPositiveButton(android.R.string.ok) { _, _ ->
                }
                .setNegativeButton(android.R.string.cancel, null).show()
        }
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
        saveShaderWithoutPersistance()
        goToCameraActivity()
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
