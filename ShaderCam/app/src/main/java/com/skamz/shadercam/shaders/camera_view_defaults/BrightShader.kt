package com.skamz.shadercam.shaders.camera_view_defaults

import android.opengl.GLES20
import android.util.Log
import com.otaliastudios.opengl.core.Egloo
import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.AbstractShader

class BrightShader : AbstractShader() {
    override fun getFragmentShader(): String {
        return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES sTexture;
                    uniform float brightness;
                    varying vec2 vTextureCoord;
                    void main() {
                      vec4 color = texture2D(sTexture, vTextureCoord);
                      gl_FragColor = brightness * color;
                    }
                """.trimIndent()
    }

    private val dataLocations: MutableMap<String, Int> = mutableMapOf()

    private class BrightnessParam : ShaderParam {
        override val paramName: String = "brightness"
        override val default: Float = 1.0f
        override val min: Float = 0.0f;
        override val max: Float = 5.0f;
    }

    private val brightnessParam: BrightnessParam = BrightnessParam()
    override val name: String = "Brightness Adjust"

    override var params: MutableList<ShaderParam> = mutableListOf(brightnessParam)
    override var dataValues: MutableMap<String, Float> = mutableMapOf("brightness" to brightnessParam.default)

    override fun onCreate(programHandle: Int) {
        super.onCreate(programHandle)
        dataLocations["brightness"] = GLES20.glGetUniformLocation(programHandle, "brightness")
        Egloo.checkGlProgramLocation(dataLocations["brightness"]!!, "brightness")
    }

    override fun onDestroy() {
        super.onDestroy()
        dataLocations["brightness"] = -1
    }

    override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
        super.onPreDraw(timestampUs, transformMatrix)
        GLES20.glUniform1f(dataLocations["brightness"]!!, dataValues["brightness"]!!)
        Egloo.checkGlError("glUniform1f")

        val bright = dataValues["brightness"]
        Log.i("DEBUG", "PRE DRAW $bright")
    }
}