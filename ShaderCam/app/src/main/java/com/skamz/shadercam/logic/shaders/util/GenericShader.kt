package com.skamz.shadercam.logic.shaders.util

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Surface
import androidx.annotation.RequiresApi
import androidx.core.view.drawToBitmap
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.R
import com.skamz.shadercam.logic.shaders.camera_view_defaults.NoopShader
import com.skamz.shadercam.ui.activities.CameraActivity

class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Any?> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()
    var updatedValues: MutableMap<String, Boolean> = mutableMapOf()

    var name: String = shaderAttributes.name
    var shaderMainText: String = shaderAttributes.shaderMainText
    var params: MutableList<ShaderParam> = shaderAttributes.params

    // Only set in feedback shaders
    var prevFrame: Bitmap? = null
    // The ID of the texture to re-use when setting prevFrame.
    var textureId: Int? = null
    // When using feedback (prevFrame) and in video mode, there's some particular error handling
    var videoMode: Boolean = false

    companion object {
        var shaderAttributes: ShaderAttributes = NoopShader
        var programHandle: Int = 0
        lateinit var context: CameraActivity
        var errorCallback: (String) -> Unit = {}
    }

    fun setValue(key: String, value: Any?) {
        dataValues[key] = value
        updatedValues[key] = true
    }

    private fun utilityFunctions() : String {
        return """
            // It requires different UVs to sample the camera vs a texture (in screen space).
            // Thus, we use this rotation function.
            // I got the different values from trial and error.
            vec2 cameraToScreenUv(vec2 cameraSpaceUv) {
                if (screenRotation == 0) { return vec2(1. - cameraSpaceUv.y, cameraSpaceUv.x); }
                else if (screenRotation == 90) { return vec2(1.0 - cameraSpaceUv.x, 1. - cameraSpaceUv.y); }
                else if (screenRotation == -90) { return vec2(cameraSpaceUv.x, cameraSpaceUv.y); }
                else if (screenRotation == 180) { return vec2(cameraSpaceUv.y, 1.0 - cameraSpaceUv.x); }
            }
            
            // Users should be able to take camera samples without having to juggle two different UVs.
            // So, they can pass the screen space UVs to sampleCamera and it will internally convert them
            // to camera space UVs using this function.
            // Numbers here taken from trial and error.
            vec2 screenToCameraUv(vec2 screenSpaceUv) {
                if (screenRotation == 0) { return vec2(screenSpaceUv.y, 1.0 - screenSpaceUv.x); }
                else if (screenRotation == 90) { return vec2(1. - screenSpaceUv.x, 1. - screenSpaceUv.y); }
                else if (screenRotation == -90) { return vec2(screenSpaceUv.x, screenSpaceUv.y); }
                else if (screenRotation == 180) { return vec2(1.0 - screenSpaceUv.y, screenSpaceUv.x); }                
            }
            
            vec3 sampleCamera(vec2 screenSpaceUv) {
                return texture2D(sTexture, screenToCameraUv(screenSpaceUv)).rgb;
            }
        """.trimIndent()
    }

    override fun getFragmentShader() : String {
        return """
                #extension GL_OES_EGL_image_external : require
                precision mediump float;

                // CameraView default built ins
                uniform samplerExternalOES sTexture;
                varying vec2 vTextureCoord;
                
                ${customBuiltInUniforms()}

                // User defined uniforms
                ${buildUserUniformsList()}
                
                ${utilityFunctions()}                
                $shaderMainText

                void main() {
                    vec2 uv = cameraToScreenUv(vTextureCoord);

                    // Here we call the user's `image` function ...
                    vec3 color = image(uv, sampleCamera(uv));

                    gl_FragColor = vec4(color, 1.0);
                }
        """.trimIndent()
    }

    private fun buildUserUniformsList(): String {
        return params.map {
            val type = when (it.paramType) {
                "float" -> "float"
                "color" -> "vec3"
                "texture" -> "sampler2D"
                else -> {
                    throw Exception("param type not handled in GenericShader.buildUserUniformsList")
                }
            }
            "uniform ${type} ${it.paramName};"
        }.joinToString("\n")
    }

    // This is called internally by CameraView as part of the snapshot process
    override fun copy(): GenericShader {
        val copy = onCopy()
        if (size != null) {
            copy.setSize(size.width, size.height)
        }
        copy.dataValues = this.dataValues
        shaderAttributes = ShaderAttributes(name, shaderMainText, params)
        return copy
    }

    // This is called internally by CameraView as part of the snapshot process
    override fun onCopy(): GenericShader {
        return try {
            javaClass.newInstance()
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        }
    }

    override fun onCreate(newProgramHandle: Int) {
        programHandle = newProgramHandle
        super.onCreate(newProgramHandle)
        setupNewShader()
    }

    // Attempts to use the provided shader attributes.
    // If things fail, switches to a fallback and communicates this to CameraView via callback.
    fun setupNewShader() {
        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        params = shaderAttributes.params.toMutableList()

        getDefaultParamLocations(programHandle)

        params.forEach {
            dataLocations[it.paramName] = GLES20.glGetUniformLocation(programHandle, it.paramName)
            updatedValues[it.paramName] = true
            if (dataValues[it.paramName] == null) {
                val paramVal = when (it.paramType) {
                    "float" -> (it as FloatShaderParam).default
                    "color" -> (it as ColorShaderParam).default
                    "texture" -> (it as TextureShaderParam).default
                    else -> {
                        throw Exception("Can't handle this type (${it.paramType}) in the shader")
                    }
                }
                dataValues[it.paramName] = paramVal
            }
            try {
                Egloo.checkGlProgramLocation(dataLocations[it.paramName]!!, it.paramName)
            } catch (e: Exception) {
                useFallbackShader(e)
            }
        }
    }

    fun useFallbackShader(e: Exception) {
        dataLocations = mutableMapOf()
        dataValues = mutableMapOf()
        errorCallback(e.message ?: "Unknown shader error")
        shaderAttributes = NoopShader
        setupNewShader()
    }

    override fun onDestroy() {
        super.onDestroy()
        params.forEach {
            dataLocations[it.paramName] = -1
        }
    }

    fun getScreenRotation() : Int {
        val rotation: Int = context.windowManager.defaultDisplay.rotation
        var angle = 0
        angle = when (rotation) {
            Surface.ROTATION_90 -> -90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 90
            else -> 0
        }
        return angle
    }

    fun getScreenSize(): Map<String, Float> {
        val size = context.camera.pictureSize
        var width: Float
        var height: Float
        if (size == null) {
            // Use screen size as a fallback
            val displayMetrics = DisplayMetrics()
            context.windowManager.defaultDisplay.getMetrics(displayMetrics)
            width = displayMetrics.widthPixels.toFloat()
            height = displayMetrics.heightPixels.toFloat()
        } else {
            width = size!!.width.toFloat()
            height = size!!.width.toFloat()
        }

        return mapOf("x" to width, "y" to height)
    }

    private fun customBuiltInUniforms() : String {
        return """
            // Our custom "built ins"
            uniform vec2 iResolution;
            uniform int screenRotation;
            uniform sampler2D prevFrame;
        """.trimIndent()
    }

    fun getDefaultParamLocations (programHandle: Int) {
        dataLocations["iResolution"] = GLES20.glGetUniformLocation(programHandle, "iResolution")
        dataLocations["screenRotation"] = GLES20.glGetUniformLocation(programHandle, "screenRotation")
        dataLocations["prevFrame"] = GLES20.glGetUniformLocation(programHandle, "prevFrame")
    }

    fun assignDefaultParamValues() {
        val screenSize = getScreenSize()
        val rotation = getScreenRotation()
        GLES20.glUniform2f(dataLocations["iResolution"]!!, screenSize["x"]!!, screenSize["y"]!!)
        GLES20.glUniform1i(dataLocations["screenRotation"]!!, rotation)
        if (dataLocations["prevFrame"] != null && dataLocations["prevFrame"]!! > 0) {
            extractPrevFrameBitmap()
        }
        if (prevFrame != null && !prevFrame!!.isRecycled) {
            val newTextureId = TextureUtils.setTextureParam(
                context,
                "prevFrame",
                1,
                bitmap = prevFrame,
                program = programHandle,
                textureId = textureId
            )
            textureId = newTextureId
        }
    }

    class PrevFrameAvailable(shader: GenericShader) : BitmapReadyCallbacks {
        private val ctx = shader
        override fun onBitmapReady(bitmap: Bitmap?) {
            ctx.prevFrame = bitmap
        }
    }

    private fun extractPrevFrameBitmap() {
        val glSurfaceView = context.camera.findViewById<GLSurfaceView>(com.otaliastudios.cameraview.R.id.gl_surface_view)
        val callbacks: BitmapReadyCallbacks = PrevFrameAvailable(this)
        captureBitmap(glSurfaceView, callbacks);
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
        super.onPreDraw(timestampUs, transformMatrix)

        assignDefaultParamValues()
        var textureIdx = 0
        params.forEach {
            if (updatedValues[it.paramName] != true) { return@forEach }
            updatedValues[it.paramName] = false

            when (it.paramType) {
                "float" -> {
                    val value = dataValues[it.paramName]!! as Float
                    GLES20.glUniform1f(dataLocations[it.paramName]!!, value)
                }
                "color" -> {
                    val value = dataValues[it.paramName]!! as Int
                    val valueColor = Color.valueOf(value)
                    GLES20.glUniform3f(
                        dataLocations[it.paramName]!!,
                        valueColor.red(),
                        valueColor.green(),
                        valueColor.blue()
                    )
                }
                "texture" -> {
                    val param = it as TextureShaderParam
                    TextureUtils.setTextureParam(
                        context,
                        param.paramName,
                        2 + textureIdx,
                        Uri.parse(param.default),
                        program = programHandle
                    )
//                    }
                    textureIdx += 1;
                }
                else -> {
                    throw Exception("Unknown type")
                }
            }

            Egloo.checkGlError("glUniform1f")
        }
    }
}