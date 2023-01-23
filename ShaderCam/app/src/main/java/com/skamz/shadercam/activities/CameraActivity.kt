package com.skamz.shadercam.activities

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.SurfaceTexture
import android.opengl.GLES20.*
import android.os.Build
import android.os.Bundle
import android.util.Log.*
import android.view.View
import android.widget.*
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.room.Room
import androidx.room.RoomDatabase
import com.google.android.material.slider.Slider
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.opengl.core.EglCore
import com.otaliastudios.opengl.program.GlShader
import com.otaliastudios.opengl.surface.EglWindowSurface
import com.otaliastudios.opengl.texture.GlTexture
import com.skamz.shadercam.*
import com.skamz.shadercam.database.AppDatabase
import com.skamz.shadercam.database.ShaderDao
import com.skamz.shadercam.databinding.ActivityCameraBinding
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader
import com.skamz.shadercam.shaders.util.*
import com.skamz.shadercam.util.IoUtil
import java.io.*


//test
class CameraActivity : AppCompatActivity() {
    private lateinit var camera: CameraView
    private var mode:Mode = Mode.PICTURE

    private lateinit var viewBinding: ActivityCameraBinding

    companion object {
        private const val TAG = "DEBUG"
        var shaderAttributes: ShaderAttributes = GenericShader.shaderAttributes
            set(value) {
                // Due to BaseFilter (and therefore GenericShader) internally using
                // .newInstance() while capturing photo/video, we cannot pass arguments to the
                // constructor. So, it is instead configured using the `shaderAttributes` static property.
                GenericShader.shaderAttributes = value
                shader = GenericShader()
                field = value
            }

        var shader: GenericShader = GenericShader()

        var shaderHasError: Boolean = false
        var shaderErrorMsg: String? = null

        lateinit var db: RoomDatabase
        lateinit var shaderDao: ShaderDao
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "shadercam-db"
        ).fallbackToDestructiveMigration().build()

        shaderDao = (db as AppDatabase).shaderDao()

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        findViewById<Button>(R.id.shader_editor_link).setOnClickListener {
            val intent = Intent(this, ShaderSelectActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.camera_switch_front_back).setOnClickListener {
            camera.toggleFacing()
        }

        val captureBtn = findViewById<ImageButton>(R.id.camera_capture_btn)
        captureBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                camera.takePictureSnapshot() // See MyCameraListener for callback
                Toast.makeText(applicationContext, "Took Picture", Toast.LENGTH_SHORT).show()
            } else {
                if (camera.isTakingVideo) {
                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    camera.stopVideo()
                } else {
                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    val path = IoUtil.buildVideoPath(this)
                    camera.takeVideoSnapshot(File(path))
                }
            }
        }

        val switchModeBtn = findViewById<ImageButton>(R.id.switch_photo_video)
        switchModeBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                mode = Mode.VIDEO
                switchModeBtn.setImageResource(R.drawable.camera_mode_icon)
                Toast.makeText(applicationContext, "Switched to Video mode", Toast.LENGTH_SHORT).show()
            } else {
                mode = Mode.PICTURE
                switchModeBtn.setImageResource(R.drawable.video_mode_icon)
                Toast.makeText(applicationContext, "Switched to Picture mode", Toast.LENGTH_SHORT).show()
            }
            camera.mode = mode
        }

        camera = findViewById(R.id.camera_view)
        camera.setLifecycleOwner(this)
        camera.addCameraListener(MyCameraListener(this))

        setShader(shader)
    }

    class MyCameraListener(parent: CameraActivity) : CameraListener() {
        private var cameraActivity: CameraActivity = parent
        override fun onPictureTaken(result: PictureResult) {
            result.toBitmap { bmp ->
                val path = IoUtil.buildPhotoPath(cameraActivity)
                i(TAG, path)
                IoUtil.saveImage(bmp!!, path, cameraActivity)
            }
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            VideoPreviewActivity.videoResult = result
            val intent = Intent(cameraActivity, VideoPreviewActivity::class.java)
            cameraActivity.startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setShader(shader)

        val deletedShaderName = intent?.getStringExtra("DeletedShader")
        if (deletedShaderName != null) {
            Toast.makeText(this, "Deleted shader $deletedShaderName", Toast.LENGTH_SHORT).show()
            shaderAttributes = NoopShader
            setShader(shader)
        }

        val updatedColorName = intent?.getStringExtra("UPDATED_COLOR_NAME")
        if (updatedColorName != null) {
            val updatedColorValue = intent!!.getIntExtra("UPDATED_COLOR_VALUE", Color.BLACK)
            updateColorShaderParam(updatedColorName, updatedColorValue!!)
            setShader(shader) // TODO: don't really need to rebuild the whole shader here.
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateFloatShaderParam(paramName: String, num: Float) {
        shader.dataValues[paramName] = num
        updateShaderText()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateColorShaderParam(paramName: String, color: Int) {
        shader.dataValues[paramName] = color
        updateShaderText()
    }

    private fun Float.format(digits: Int) = "%.${digits}f".format(this)

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateShaderText(nameOverride: String? = null) {
        val shaderTitle = findViewById<TextView>(R.id.shader_title)
        var text = "Current shader: ${nameOverride ?: shader.name}"
        if (shader.params.isNotEmpty()) {
            var paramHints = ""
            shader.params.forEachIndexed { index, shaderParam ->
                val shaderVal = shader.dataValues[shaderParam.paramName]
                val shaderValString: String = when(shaderParam.paramType) {
                    "float" -> {
                        if(shaderVal == null) {
                            (shaderParam as FloatShaderParam).default
                        } else {
                            shaderVal as Float
                        }.format(2)
                    }
                    "color" -> {
                        val colorInt = if(shaderVal == null) {
                            (shaderParam as ColorShaderParam).default
                        } else {
                            (shaderVal as Int)
                        }
                        val color = Color.valueOf(colorInt)
                        "${color.red().format(2)}, ${color.green().format(2)}, ${color.blue().format(2)}"
                    }
                    "texture" -> {
                        (shaderParam as TextureShaderParam).default ?: "default noise texture"
                    }
                    else -> {
                        throw Exception("param type not handled in CameraActivity.updateShaderText")
                    }
                }
                paramHints += "\n  ${index + 1}. ${shaderParam.paramName} (${shaderValString})"
            }
            text += "\n Params: $paramHints"
        }
        if (shaderHasError) {
            text += "\n SHADER HAS ERROR\n\n $shaderErrorMsg"
        }
        shaderTitle.text = text
    }

    private fun fit(value: Float, oldMin: Float, oldMax: Float, newMin: Float, newMax: Float): Float {
        val inputRange: Float = oldMax - oldMin
        val outputRange: Float = newMax - newMin

        return (value - oldMin) * outputRange / inputRange + newMin
    }

    // Returns null if shader is valid. Otherwise, returns error message
    private fun validateShader(shader: GenericShader): String? {
        val core = EglCore()
        val texture = GlTexture()
        val surfaceTexture = SurfaceTexture(texture.id)
        val window = EglWindowSurface(core, surfaceTexture)
        window.makeCurrent()

        try {
            GlShader(GL_FRAGMENT_SHADER, shader.fragmentShader)
        } catch (e: java.lang.RuntimeException) {
            return e.message
        } finally {
            core.release()
        }
        return null
    }

    private fun useFallbackShader() {
        GenericShader.shaderAttributes = NoopShader
        camera.filter = GenericShader()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleShaderError (error: String) {
        useFallbackShader()
        val uiContainer = findViewById<LinearLayout>(R.id.dynamic_ui)
        uiContainer.removeAllViews()
        shaderHasError = true
        shaderErrorMsg = error
        updateShaderText(shader.name)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setShader(shader: GenericShader) {
        val error = validateShader(shader)
        if (error != null) {
            handleShaderError(error)
            return
        }

        shaderHasError = false
        shaderErrorMsg = null

        camera.filter = shader

        val uiContainer = findViewById<LinearLayout>(R.id.dynamic_ui)
        uiContainer.removeAllViews()

        updateShaderText()

        shader.params.forEach { it ->
            when (it.paramType) {
                "float" -> {
                    val inflatedView: View = View.inflate(this, R.layout.param_slider, uiContainer)
                    val slider = inflatedView.findViewById<Slider>(R.id.slider)
                    val paramTitle = inflatedView.findViewById<TextView>(R.id.param_slider_name)
                    paramTitle.text = it.paramName

                    val shaderParam = it as FloatShaderParam
                    val shaderValue = (shader.dataValues[shaderParam.paramName] ?: shaderParam.default) as Float
                    val default01 = fit(shaderValue, shaderParam.min, shaderParam.max, 0.0f, 1.0f)

                    slider.value = default01

                    slider.addOnChangeListener { _, value, _ ->
                        val remappedVal = fit(value, 0.0f,1.0f, shaderParam.min, shaderParam.max)
                        updateFloatShaderParam(it.paramName, remappedVal)
                    }
                }
                "color" -> {
                    val inflatedView: View = View.inflate(this, R.layout.param_color, uiContainer)
                    val button = inflatedView.findViewById<Button>(R.id.color_param_button)
                    val paramTitle = inflatedView.findViewById<TextView>(R.id.param_color_name)
                    paramTitle.text = it.paramName

                    val shaderParam = it as ColorShaderParam
                    val colorInt = (shader.dataValues[shaderParam.paramName] ?: shaderParam.default) as Int
                    ViewCompat.setBackgroundTintList(button, ColorStateList.valueOf(colorInt));

                    button.setOnClickListener { _ ->
                        CameraColorPickerActivity.startingColor = colorInt
                        CameraColorPickerActivity.paramName = it.paramName
                        val intent = Intent(this, CameraColorPickerActivity::class.java)
                        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
                        startActivity(intent)
                    }
                }
                "texture" -> {
                    throw Exception("texture handler not implemented in CameraActivity.setShader")
                }
                else -> {
                    throw Exception("unknown type")
                }
            }

        }
    }
}
