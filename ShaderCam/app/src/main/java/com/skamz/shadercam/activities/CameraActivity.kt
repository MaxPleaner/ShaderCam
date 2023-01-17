package com.skamz.shadercam.activities

import android.content.Intent
import android.graphics.SurfaceTexture
import android.opengl.GLES20
import android.opengl.GLES20.*
import android.os.Bundle
import android.util.Log
import android.util.Log.*
import android.view.SurfaceView
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
import com.skamz.shadercam.shaders.util.GenericShader
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.util.IoUtil
import java.io.*
import java.nio.CharBuffer
import java.nio.IntBuffer

//test
class CameraActivity : AppCompatActivity() {
    lateinit var camera: CameraView;
    var mode:Mode = Mode.PICTURE

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

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java, "shadercam-db"
        ).build()

        shaderDao = (db as AppDatabase).shaderDao()

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<Button>(R.id.shader_editor_link).setOnClickListener {
            val intent = Intent(this, ShaderSelectActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
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
        var cameraActivity: CameraActivity = parent
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

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setShader(shader)
    }

    private fun updateShaderParams(paramName: String, num: Float) {
        shader.dataValues[paramName] = num
        updateShaderText()
    }

    fun Float.format(digits: Int) = "%.${digits}f".format(this)

    private fun updateShaderText(nameOverride: String? = null) {
        val shaderTitle = findViewById<TextView>(R.id.shader_title)
        var text = "Current shader: ${nameOverride ?: shader.name}"
        if (shader.params.count() > 0) {
            var paramHints = ""
            shader.params.forEachIndexed { index, shaderParam ->
                var shaderVal = shader.dataValues[shaderParam.paramName]
                if (shaderVal == null) {
                    shaderVal = shaderParam.default
                }
                paramHints += "\n  ${index + 1}. ${shaderParam.paramName} (${shaderVal.format(2)})"
            }
            text += "\n Params: $paramHints"
        }
        if (shaderHasError) {
            text += "\n SHADER HAS ERROR\n\n $shaderErrorMsg"
        }
        shaderTitle.text = text
    }

    private fun fit(value: Float, oldMin: Float, oldMax: Float, newMin: Float, newMax: Float): Float {
        val input_range: Float = oldMax - oldMin
        val output_range: Float = newMax - newMin

        return (value - oldMin) * output_range / input_range + newMin
    }

    // Returns null if shader is valid. Otherwise, returns error message
    fun validateShader(shader: GenericShader): String? {
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

    private fun handleShaderError (error: String, shaderName: String) {
        useFallbackShader()
        val uiContainer = findViewById<LinearLayout>(R.id.dynamic_ui)
        uiContainer.removeAllViews()
        shaderHasError = true
        shaderErrorMsg = error
        updateShaderText(shader.name)
    }

    @OptIn(ExperimentalStdlibApi::class)
    private fun setShader(shader: GenericShader) {
        val error = validateShader(shader)
        if (error != null) {
            handleShaderError(error, shader.name)
            return
        }

        shaderHasError = false
        shaderErrorMsg = null

        camera.filter = shader;

        val uiContainer = findViewById<LinearLayout>(R.id.dynamic_ui)
        uiContainer.removeAllViews()

        updateShaderText()

        shader.params.forEach {
            val inflatedView: View = View.inflate(this, R.layout.param_slider, uiContainer)
            val slider = inflatedView.findViewById<Slider>(R.id.slider)
            val default01 = fit(it.default, it.min, it.max, 0.0f, 1.0f)
            slider.value = default01

            slider.addOnChangeListener { _, value, _ ->
                val remappedVal = fit(value, 0.0f,1.0f, it.min, it.max)
                updateShaderParams(it.paramName, remappedVal)
            }
        }
    }
}
