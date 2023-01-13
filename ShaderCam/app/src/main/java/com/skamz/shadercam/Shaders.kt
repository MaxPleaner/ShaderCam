package com.skamz.shadercam

class Shaders {
    companion object {
        val Bright = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTexture;
            varying vec2 vTextureCoord;
            void main() {
              vec4 color = texture2D(sTexture, vTextureCoord);
              gl_FragColor = 3.0 * color;
            }
        """.trimIndent()

        val Dark = """
            #extension GL_OES_EGL_image_external : require
            precision mediump float;
            uniform samplerExternalOES sTexture;
            varying vec2 vTextureCoord;
            void main() {
              vec4 color = texture2D(sTexture, vTextureCoord);
              gl_FragColor = 0.5 * color;
            }
        """.trimIndent()
    }
}