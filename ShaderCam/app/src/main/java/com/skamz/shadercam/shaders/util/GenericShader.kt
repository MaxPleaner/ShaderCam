package com.skamz.shadercam.shaders.util

import android.graphics.Color
import android.opengl.GLES20
import android.opengl.GLES31
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.annotation.RequiresApi
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader
import com.skamz.shadercam.shaders.camera_view_defaults.TintShader


class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Any?> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()

    var name: String = shaderAttributes.name
    var shaderMainText: String = shaderAttributes.shaderMainText
    var params: MutableList<ShaderParam> = shaderAttributes.params

    companion object {
        var shaderAttributes: ShaderAttributes = TintShader
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
                "texture" -> "samplerExternalOES"
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

    override fun onCreate(programHandle: Int) {
        super.onCreate(programHandle)

        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        params = shaderAttributes.params

        params.forEach {
            dataLocations[it.paramName] = GLES20.glGetUniformLocation(programHandle, it.paramName)
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
                }
                else -> {
                    throw Exception("Unknown type")
                }
            }

            Egloo.checkGlError("glUniform1f")
        }
    }
}
//
//val textureId = loadTexture(settings.texture)
//
//val sTextureLocation = GLES31.glGetUniformLocation(program, "iChannel1")
//GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + 1)
//GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId)
//GLES31.glUniform1i(sTextureLocation, 1)