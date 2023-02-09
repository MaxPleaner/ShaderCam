package com.skamz.shadercam.logic.shaders.util

import android.content.Context
import android.graphics.Camera
import android.graphics.Color
import android.net.Uri
import android.opengl.GLES20
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.WindowManager
import androidx.annotation.RequiresApi
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.logic.shaders.camera_view_defaults.NoopShader
import com.skamz.shadercam.logic.shaders.camera_view_defaults.TintShader
import com.skamz.shadercam.ui.activities.CameraActivity

class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Any?> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()
    var updatedValues: MutableMap<String, Boolean> = mutableMapOf()

    var name: String = shaderAttributes.name
    var shaderMainText: String = shaderAttributes.shaderMainText
    var params: MutableList<ShaderParam> = shaderAttributes.params

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

    override fun getFragmentShader(): String {
        return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES sTexture;
                    uniform vec2 iResolution;
                    ${buildUniformsList()}
                    varying vec2 vTextureCoord;
                    $shaderMainText
                """.trimIndent()
    }

    private fun buildUniformsList(): String {
        return params.map {
            val type = when (it.paramType) {
                "float" -> "float"
                "color" -> "vec3"
                "texture" -> "sampler2D"
                else -> {
                    throw Exception("param type not handled in GenericShader.buildUniformsList")
                }
            }
            "uniform ${type} ${it.paramName};"
        }.joinToString("\n")
    }

    override fun copy(): GenericShader {
        val copy = onCopy()
        if (size != null) {
            copy.setSize(size.width, size.height)
        }
        copy.dataValues = this.dataValues
        shaderAttributes = ShaderAttributes(name, shaderMainText, params)
        return copy
    }

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
        super.onCreate(newProgramHandle)

        programHandle = newProgramHandle
        setupNewShader()
    }

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
                dataLocations = mutableMapOf()
                dataValues = mutableMapOf()
                errorCallback(e.message ?: "Unknown shader error")
                shaderAttributes = NoopShader
                setupNewShader()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        params.forEach {
            dataLocations[it.paramName] = -1
        }
    }

    fun getScreenSize(): Map<String, Float> {
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        var width = displayMetrics.widthPixels.toFloat()
        var height = displayMetrics.heightPixels.toFloat()
        return mapOf("x" to width, "y" to height)
    }

    fun getDefaultParamLocations (programHandle: Int) {
        dataLocations["iResolution"] = GLES20.glGetUniformLocation(programHandle, "iResolution")
    }

    fun assignDefaultParamValues() {
        val screenSize = getScreenSize()
        GLES20.glUniform2f(dataLocations["iResolution"]!!, screenSize["x"]!!, screenSize["y"]!!)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
        super.onPreDraw(timestampUs, transformMatrix)
        assignDefaultParamValues()
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
//                    throw Exception("need to implement texture handler in GenericShader.onPreDraw")
                    // TODO: Call functions in TextureUtils
                    val param = it as TextureShaderParam
                    TextureUtils.setTextureParam(
                        context,
                        param.paramName,
                        1,
                        Uri.parse(param.default),
                        programHandle
                    )
                }
                else -> {
                    throw Exception("Unknown type")
                }
            }

            Egloo.checkGlError("glUniform1f")
        }
    }
}