package com.skamz.shadercam

import android.opengl.GLES20
import android.util.Log
import com.otaliastudios.cameraview.filter.BaseFilter
import com.otaliastudios.cameraview.filter.OneParameterFilter
import com.otaliastudios.cameraview.filter.TwoParameterFilter
import com.otaliastudios.opengl.core.Egloo.checkGlError
import com.otaliastudios.opengl.core.Egloo.checkGlProgramLocation

interface ShaderParam {
    val paramName: String
    val default: Float
    val min: Float
    val max: Float
}

abstract class AbstractShader : BaseFilter() {
    abstract val dataValues: MutableMap<String, Float>
    abstract val params: MutableList<ShaderParam>

    override fun copy(): AbstractShader {
        val copy = onCopy()
        if (size != null) {
            copy.setSize(size.width, size.height)
        }
        if (this is OneParameterFilter) {
            (copy as OneParameterFilter).parameter1 = (this as OneParameterFilter).parameter1
        }
        if (this is TwoParameterFilter) {
            (copy as TwoParameterFilter).parameter2 = (this as TwoParameterFilter).parameter2
        }
        return copy
    }
}

class Shaders {
    companion object {
        class NoopShader : AbstractShader () {
            override val dataValues: MutableMap<String, Float> = mutableMapOf()
            override val params: MutableList<ShaderParam> = mutableListOf()
            override fun getFragmentShader(): String {
                return """
                    #extension GL_OES_EGL_image_external : require
                    precision mediump float;
                    uniform samplerExternalOES sTexture;
                    varying vec2 vTextureCoord;
                    void main() {
                      vec4 color = texture2D(sTexture, vTextureCoord);
                      gl_FragColor = 3.0 * color;
                    }
                """.trimIndent()
            }
        }

        class BrightShader : AbstractShader(), OneParameterFilter {
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
                override val default: Float = 3.0f
                override val min: Float = 0.0f;
                override val max: Float = 5.0f;
            }

            private val brightnessParam: BrightnessParam = BrightnessParam()

            override val params: MutableList<ShaderParam> = mutableListOf(brightnessParam)
            override val dataValues: MutableMap<String, Float> = mutableMapOf("brightness" to brightnessParam.default)

            override fun onCreate(programHandle: Int) {
                super.onCreate(programHandle)
                dataLocations["brightness"] = GLES20.glGetUniformLocation(programHandle, "brightness")
                checkGlProgramLocation(dataLocations["brightness"]!!, "brightness")
            }

            override fun onDestroy() {
                super.onDestroy()
                dataLocations["brightness"] = -1
            }

            override fun setParameter1(value: Float) {
                Log.i("DEBUG", "value is $value")
                TODO("Not yet implemented")
            }

            override fun getParameter1(): Float {
//                Log.i("DEBUG", "value is $value")
                TODO("Not yet implemented")
//                return 0.0f;
            }

            override fun onPreDraw(timestampUs: Long, transformMatrix: FloatArray) {
                super.onPreDraw(timestampUs, transformMatrix)
                GLES20.glUniform1f(dataLocations["brightness"]!!, dataValues["brightness"]!!)
                checkGlError("glUniform1f")

                val bright = dataValues["brightness"]
                Log.i("DEBUG", "PRE DRAW $bright")
            }
        }
    }
}