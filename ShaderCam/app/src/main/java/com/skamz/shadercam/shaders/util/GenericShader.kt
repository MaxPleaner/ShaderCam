package com.skamz.shadercam.shaders.util

import android.opengl.GLES20
import android.os.Bundle
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader


class GenericShader() : BaseFilterPatch() {

    var dataValues: MutableMap<String, Float> = mutableMapOf()
    var dataLocations: MutableMap<String, Int> = mutableMapOf()

    var initialized: Boolean = false;
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
//            GenericShader(ShaderAttributes(name, shaderMainText, params))
            javaClass.newInstance()
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        }
    }

    fun forceInitialize() {
        if (initialized) { return }

        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        params = shaderAttributes.params

        initialized = true
    }

    override fun onCreate(programHandle: Int) {
        super.onCreate(programHandle)

        forceInitialize()
        params.forEach {
            dataLocations[it.paramName] = GLES20.glGetUniformLocation(programHandle, it.paramName)
            dataValues[it.paramName] = it.default
            Egloo.checkGlProgramLocation(dataLocations[it.paramName]!!, it.paramName)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        params.forEach {
            dataLocations[it.paramName] = -1
        }
    }

    override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
        super.onPreDraw(timestampUs, transformMatrix)
        params.forEach {
            GLES20.glUniform1f(dataLocations[it.paramName]!!, dataValues[it.paramName]!!)
            Egloo.checkGlError("glUniform1f")
        }
    }
}