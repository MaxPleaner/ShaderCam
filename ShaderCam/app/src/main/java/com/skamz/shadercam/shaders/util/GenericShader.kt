package com.skamz.shadercam.shaders.util

import android.content.Context
import android.graphics.Color
import android.net.Uri
import android.opengl.GLES20
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.shaders.camera_view_defaults.TintShader


class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Any?> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()
    var updatedValues: MutableMap<String, Boolean> = mutableMapOf()

    var name: String = shaderAttributes.name
    var shaderMainText: String = shaderAttributes.shaderMainText
    var params: MutableList<ShaderParam> = shaderAttributes.params

    companion object {
        var shaderAttributes: ShaderAttributes = TintShader
        lateinit var context: Context
        var programHandle: Int = 0
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
                    ${buildUniformsList()}
                    varying vec2 vTextureCoord;
                    void main() {
                        vec2 uv = vTextureCoord;
                        $shaderMainText
                    }
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

        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        Log.e("DEBUG", "setting attributes: ${shaderAttributes.params}")
        params = shaderAttributes.params.toMutableList()

        params.forEach {
            dataLocations[it.paramName] = GLES20.glGetUniformLocation(newProgramHandle, it.paramName)
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
            Egloo.checkGlProgramLocation(dataLocations[it.paramName]!!, it.paramName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        params.forEach {
            dataLocations[it.paramName] = -1
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
        super.onPreDraw(timestampUs, transformMatrix)
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