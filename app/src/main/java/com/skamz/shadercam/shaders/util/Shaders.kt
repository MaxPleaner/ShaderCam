package com.skamz.shadercam.shaders.util

import com.skamz.shadercam.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader


val defaultShaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          gl_FragColor = color;
        """.trimIndent()

class Shaders {
    companion object {
        val noopShader: ShaderAttributes = NoopShader
        val brightShader: ShaderAttributes = BrightShader
    }
}