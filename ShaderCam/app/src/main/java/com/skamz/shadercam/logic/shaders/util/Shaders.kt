package com.skamz.shadercam.logic.shaders.util

import com.skamz.shadercam.logic.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.logic.shaders.camera_view_defaults.NoopShader
import com.skamz.shadercam.logic.shaders.camera_view_defaults.TintShader


val defaultShaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          gl_FragColor = color;
        """.trimIndent()

class Shaders {
    companion object {
        val noopShader: ShaderAttributes = NoopShader
        val brightShader: ShaderAttributes = BrightShader
        val tintShader: ShaderAttributes = TintShader

        val all: List<ShaderAttributes> = listOf(
            noopShader,
            brightShader,
            tintShader
        )
    }
}