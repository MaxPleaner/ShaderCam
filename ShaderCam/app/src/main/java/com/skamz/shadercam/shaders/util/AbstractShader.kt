package com.skamz.shadercam.shaders.util

import android.graphics.Shader

class AbstractShader(shaderAttributes: ShaderAttributes) : BaseFilterPatch() {
    open var dataValues: MutableMap<String, Float> = mutableMapOf()
    open var params: MutableList<ShaderParam> = mutableListOf()
    open var dataLocations: MutableMap<String, Int> = mutableMapOf()

    var name: String

    open var shaderMainText: String = defaultShaderMainText

    init {
        name = shaderAttributes.name
        shaderMainText = shaderAttributes.shaderMainText
        params = shaderAttributes.params
    }

    companion object {
        val defaultShaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          gl_FragColor = color;
        """.trimIndent()
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

    fun buildUniformsList(): String {
        return params.map {
            "uniform float ${it.paramName};"
        }.joinToString("\n")
    }


    override fun copy(): AbstractShader {
        val copy = onCopy()
        if (size != null) {
            copy.setSize(size.width, size.height)
        }
        copy.dataValues = this.dataValues
        return copy
    }

    override fun onCopy(): AbstractShader {
        return try {
            AbstractShader(ShaderAttributes(name, shaderMainText, params))
//            javaClass.newInstance(ShaderAttributes(name, shaderMainText, params))
        } catch (e: IllegalAccessException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        } catch (e: InstantiationException) {
            throw RuntimeException("Filters should have a public no-arguments constructor.", e)
        }
    }
}