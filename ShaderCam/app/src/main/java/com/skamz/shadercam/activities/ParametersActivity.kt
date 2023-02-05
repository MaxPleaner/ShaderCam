package com.skamz.shadercam.activities

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContract
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import com.skamz.shadercam.shaders.camera_view_defaults.TextureOverlayShaderData
import com.skamz.shadercam.shaders.util.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import androidx.compose.ui.graphics.Color as ComposeColor

class ParametersActivity : AppCompatActivity() {
    private lateinit var floatLayout: LinearLayout
    private lateinit var colorLayout: LinearLayout
    private lateinit var textureLayout: LinearLayout

    private lateinit var colorRB: RadioButton
    private lateinit var floatRB: RadioButton
    private lateinit var textureRB: RadioButton

    private lateinit var nameInput: TextInputEditText
    private lateinit var defaultFloatInput: TextInputEditText
    private lateinit var maxFloatInput: TextInputEditText
    private lateinit var minFloatInput: TextInputEditText

    private lateinit var defaultTextureImage: ImageView
    private lateinit var defaultTextureUrl: TextInputEditText

    private var type: String = "float"

    private var defaultFloatValueInitial: Float = 1f
    private var maxFloatValueInitial: Float = 1f
    private var minFloatValueInitial: Float = 0f

    private var origParamName: String? = null

    private var defaultColorValueInitial = Color.BLUE
    var defaultColorValue: Int = defaultColorValueInitial

    lateinit var defaultTextureValueInitial: String
    lateinit var defaultTextureValue: String

    private lateinit var cancelBtn: TextView
    private lateinit var saveBtn: TextView
    private lateinit var deleteBtn: TextView

    private lateinit var filePickerLauncher: ActivityResultLauncher<String>

    // mode can be "new" or "update" and changes the saving behavior
    private var mode = "new"

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parameters)
        init()
        setupEditMode(intent)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setupEditMode(intent)
    }

     @RequiresApi(Build.VERSION_CODES.O)
     private fun setupEditMode(intent: Intent?) {
        val editParamName = intent?.getStringExtra("editParamName");
        if (editParamName != null) {
            mode = "edit"
            saveBtn.text = "Update"
            deleteBtn.visibility = View.VISIBLE

            val param = EditorActivity.parameters.find {
                it.paramName == editParamName
            } ?: return resetValues()

            setType(param.paramType)

            nameInput.setText(param.paramName)
            origParamName = param.paramName

            when (param.paramType) {
                "float" -> setFloatValues(param as FloatShaderParam)
                "color" -> setColorValues(param as ColorShaderParam)
                "texture" -> setTextureValues(param as TextureShaderParam)
                else -> throw Exception("Param type not implemented in ParametersActivity.setupEditMode")
            }
        } else {
            mode = "new"
            saveBtn.text = "Save"
            deleteBtn.visibility = View.GONE
            origParamName = null
            resetValues()
        }
    }

    private fun setFloatValues(param: FloatShaderParam) {
        defaultFloatInput.setText(param.default.toString())
        minFloatInput.setText(param.min.toString())
        maxFloatInput.setText(param.max.toString())
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setColorValues(param: ColorShaderParam) {
        defaultColorValue = param.default
        ParametersActivityColorPickerFragmentActivity.startingColor = ComposeColor(defaultColorValue)
    }

    private fun setTextureValues(param: TextureShaderParam) {
        defaultTextureValue = param.default ?: defaultTextureValueInitial
        showDefaultTextureImage(Uri.parse(defaultTextureValue))
    }

    private fun updateTextureUriText (uri: Uri) {
        when (uri.scheme) {
            "http", "https" -> {
                defaultTextureUrl.setText(uri.toString())
            }
            else -> {
                defaultTextureUrl.setText("")
            }
        }
    }

    private fun showDefaultTextureImage(uri: Uri) {
        val context = this
        when (uri.scheme) {
            "http", "https", "hardcodedResource" -> {
                CoroutineScope(Dispatchers.IO).launch {
                    val bitmap = TextureUtils.bitmapFromUri(context, uri)
                    runOnUiThread { defaultTextureImage.setImageBitmap(bitmap); }
                }
            }
            else -> {
                defaultTextureUrl.setText("")
                defaultTextureImage.setImageURI(Uri.parse(defaultTextureValue))
            }
        }
    }

    private fun setType(newType: String) {
        colorRB.isChecked = false
        colorLayout.visibility = View.GONE

        textureRB.isChecked = false
        textureLayout.visibility = View.GONE

        floatRB.isChecked = false
        floatLayout.visibility = View.GONE

        type = newType

        when (type) {
            "float" -> {
                floatRB.isChecked = true
                floatLayout.visibility = View.VISIBLE
            }
            "color" -> {
                colorRB.isChecked = true
                colorLayout.visibility = View.VISIBLE
            }
            "texture" -> {
                Log.i("DEBUG", "making texture RB checked")
                textureRB.isChecked = true
                textureLayout.visibility = View.VISIBLE
            }
            else -> {
                throw Exception("unimplemented")
            }
        }
    }

    private fun init() {
            colorLayout = findViewById(R.id.colorLayout)
            floatLayout = findViewById(R.id.floatLayout)
            textureLayout = findViewById(R.id.textureLayout)

            colorRB = findViewById(R.id.colorRB)
            floatRB = findViewById(R.id.floatRb)
            textureRB = findViewById(R.id.textureRB)

            nameInput = findViewById(R.id.name_input)

            defaultFloatInput = findViewById(R.id.defaultFloatInput)
            minFloatInput = findViewById(R.id.minFloatInput)
            maxFloatInput = findViewById(R.id.maxFloatInput)

            saveBtn = findViewById(R.id.saveParameters)
            cancelBtn = findViewById(R.id.cancelParameters)
            deleteBtn = findViewById(R.id.deleteParameter)

            defaultTextureImage = findViewById(R.id.default_texture_image)
            defaultTextureUrl = findViewById<TextInputEditText>(R.id.default_texture_url)
            defaultTextureValueInitial = TextureOverlayShaderData.defaultImageUrl
            defaultTextureValue = defaultTextureValueInitial
            showDefaultTextureImage(Uri.parse(defaultTextureValue))
            updateTextureUriText(Uri.parse(defaultTextureValue))

            filePickerLauncher = registerForActivityResult(FilePickerContract()) { uri ->
                if (uri != null) {
                    contentResolver.takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                    val publicUri = saveUploadedImage(uri)
                    defaultTextureValue = publicUri.toString()
                    showDefaultTextureImage(publicUri)
                }
            }

            setType("float")
            setOnClickListeners()
    }

    // Returns error string or null
    private fun validateForSave (): String? {
        val name = nameInput.text.toString()
        if (name.isEmpty()) {
            return "Parameter name cannot be empty"
        }
        val existingParamNames = EditorActivity.parameters.map { it.paramName }
        when (mode) {
            "new" -> {
                if (existingParamNames.contains(name)) {
                    return "Parameter name is already taken"
                }
            }
            "edit" -> {
                val otherExistingParamNames = existingParamNames.filter {
                    if (origParamName != null) {
                        origParamName != it
                    } else {
                        true
                    }
                }
                if (otherExistingParamNames.contains(name)) {
                    return "Parameter name is already taken"
                }
            }
        }
        if (!validVariableName(name)) {
            return """
                Name can only contain A-Z a-z 0-9 _
                Name cannot begin with 0-9
            """.trimIndent()
        }

        return null
    }

    private fun validVariableName(name: String): Boolean {
        // Contains only letters, numbers, and underscore
        // Cannot begin with digit
        var regex = Regex("^[A-Za-z_][A-Za-z_0-9]*$")
        return regex.matches(name)
    }

    private fun saveUploadedImage(uri: Uri): Uri {
        Toast.makeText(this, "Picked image: ${uri.path}", Toast.LENGTH_SHORT).show()
        return uri
    }

    class FilePickerContract : ActivityResultContract<String, Uri?>() {
        override fun parseResult(resultCode: Int, intent: Intent?): Uri? {
            if (resultCode != Activity.RESULT_OK) {
                return null
            } else {
                return intent?.data
            }
        }

        override fun createIntent(context: Context, input: String): Intent {
            return Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                addCategory(Intent.CATEGORY_OPENABLE)
                type = "image/*"
            }
        }
    }

    private fun setOnClickListeners() {
        colorRB.setOnClickListener { toggleParameterType(colorRB) }
        floatRB.setOnClickListener { toggleParameterType(floatRB) }
        textureRB.setOnClickListener { toggleParameterType(textureRB) }

        findViewById<Button>(R.id.pick_default_texture).setOnClickListener {
            filePickerLauncher.launch("")
        }

        findViewById<Button>(R.id.load_default_texture_url).setOnClickListener {
            val urlString: String = defaultTextureUrl.text.toString()
            defaultTextureValue = urlString
            showDefaultTextureImage(Uri.parse(urlString))
        }

        saveBtn.setOnClickListener {
            val err = validateForSave()
            if (err != null) {
                Toast.makeText(this, err, Toast.LENGTH_SHORT).show()
            } else {
                save()
                goBackToEditor()
            }
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
        lateinit var shaderParam: ShaderParam
        Log.i("DEBUG", "saving ${type}")
        when (type) {
            "float" -> {
                shaderParam = FloatShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultFloatInput.text.toString().toFloat(),
                    min = minFloatInput.text.toString().toFloat(),
                    max = maxFloatInput.text.toString().toFloat()
                )
            }
            "color" -> {
                shaderParam = ColorShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultColorValue
                )
            }
            "texture" -> {
                shaderParam = TextureShaderParam(
                    paramName = nameInput.text.toString(),
                    default = defaultTextureValue
                )
            }
            else -> {
                throw Exception("unimplemented")
            }
        }
        when (mode) {
            "new" -> {
                EditorActivity.parameters.add(shaderParam)
            }
            "edit" -> {
                EditorActivity.parameters.forEachIndexed { index, param ->
                    param.takeIf { it.paramName == origParamName }?.let {
                        EditorActivity.parameters[index] = shaderParam
                    }
                }
            }
        }
    }

    private fun deleteParam() {
        val param = EditorActivity.parameters.find {
            it.paramName == origParamName
        }
        EditorActivity.parameters.remove(param)
    }

    private fun resetValues() {
        nameInput.setText("")
        defaultFloatInput.setText(defaultFloatValueInitial.toString())
        maxFloatInput.setText(maxFloatValueInitial.toString())
        minFloatInput.setText(minFloatValueInitial.toString())
        defaultColorValue = defaultColorValueInitial
        defaultTextureValue = defaultTextureValueInitial
    }

    private fun toggleParameterType(view: View) {
        if (view is RadioButton) {
            floatLayout.visibility = View.GONE
            colorLayout.visibility = View.GONE
            textureLayout.visibility = View.GONE

            val checked = view.isChecked
            when (view.getId()) {
                R.id.colorRB ->
                    if (checked) {
                        type = "color"
                        colorLayout.visibility = View.VISIBLE
                    }
                R.id.floatRb ->
                    if (checked) {
                        type = "float"
                        floatLayout.visibility = View.VISIBLE
                    }
                R.id.textureRB ->
                    if (checked) {
                        type = "texture"
                        textureLayout.visibility = View.VISIBLE
                    }
            }
        }
    }
}