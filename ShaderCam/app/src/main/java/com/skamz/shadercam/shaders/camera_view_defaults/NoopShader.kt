package com.skamz.shadercam.shaders.camera_view_defaults

import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.AbstractShader

class NoopShader : AbstractShader() {
    override var dataValues: MutableMap<String, Float> = mutableMapOf()
    override var params: MutableList<ShaderParam> = mutableListOf()
    override val name: String = "Pass Through"
    override fun getFragmentShader(): String {
        return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES sTexture;
                    varying vec2 vTextureCoord;
                    void main() {
                      vec4 color = texture2D(sTexture, vTextureCoord);
                      gl_FragColor = color;
                    }
                """.trimIndent()
    }
}