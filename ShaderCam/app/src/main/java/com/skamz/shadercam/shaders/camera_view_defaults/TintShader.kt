package com.skamz.shadercam.shaders.camera_view_defaults

import androidx.compose.ui.graphics.Color
import com.skamz.shadercam.shaders.util.ColorShaderParam
import com.skamz.shadercam.shaders.util.FloatShaderParam
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.ShaderParam

class TintShaderData {
    companion object {
        val shaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          gl_FragColor = vec4(tint, 1.0) * color;
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            ColorShaderParam("tint", android.graphics.Color.BLUE)
        )

    }
}

val TintShader = ShaderAttributes(
    "(Template) Tint",
    TintShaderData.shaderMainText,
    TintShaderData.params
)