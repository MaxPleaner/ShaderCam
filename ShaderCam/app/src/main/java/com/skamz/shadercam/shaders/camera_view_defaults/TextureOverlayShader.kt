package com.skamz.shadercam.shaders.camera_view_defaults

import android.graphics.Color
import com.skamz.shadercam.shaders.util.ColorShaderParam
import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.TextureShaderParam

class TextureOverlayShaderData {
    companion object {
        val shaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          vec4 overlay = texture2D(overlayTexture, vTextureCoord);
          gl_FragColor = overlay * color;
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            TextureShaderParam("overlayTexture", null)
        )

    }
}

val TextureOverlayShader = ShaderAttributes(
    "(Template) Texture Overlay",
    TintShaderData.shaderMainText,
    TintShaderData.params
)