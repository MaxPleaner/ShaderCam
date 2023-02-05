package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.ColorShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes
import com.skamz.shadercam.logic.shaders.util.ShaderParam

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
    TintShaderData.params,
//    templateParams = TintShaderData.params
)