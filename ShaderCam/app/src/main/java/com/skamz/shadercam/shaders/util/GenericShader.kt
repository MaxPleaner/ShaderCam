package com.skamz.shadercam.shaders.util

import android.graphics.Color
import android.opengl.GLES20
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader


class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Any> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()

    var name: String = shaderAttributes.name
    var shaderMainText: String = shaderAttributes.shaderMainText
    var params: MutableList<ShaderParam> = shaderAttributes.params

    companion object {
        var shaderAttributes: ShaderAttributes = NoopShader
    }

    override fun getFragmentShader(): String {
        return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES sTexture;
                    ${buildUniformsList()}
                    varying vec2 vTextureCoord;
                    void main() {
                        $shaderMainText
                    }
                """.trimIndent()
    }

    private fun buildUniformsList(): String {
        return params.map {
            val type = when (it.type) {
                "float" -> "float"
                "color" -> "vec3"
                else -> {
                    throw Exception("Unknown type ${it.type}")
                }
            }
            "uniform float ${it.paramName};"
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

    override fun onCreate(programHandle: Int) {
        super.onCreate(programHandle)

        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        params = shaderAttributes.params

        params.forEach {
            dataLocations[it.paramName] = GLES20.glGetUniformLocation(programHandle, it.paramName)
            if (dataValues[it.paramName] == null) {
                val paramVal = when (it.type) {
                    "float" -> (it as FloatShaderParam).default
                    "color" -> (it as ColorShaderParam).default
                    else -> {
                        throw Exception("Can't handle this type (${it.type}) in the shader")
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
            when (it.type) {
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
                        valueColor.blue(),
                        valueColor.green()
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