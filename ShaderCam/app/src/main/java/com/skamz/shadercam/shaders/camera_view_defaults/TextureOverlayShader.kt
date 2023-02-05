package com.skamz.shadercam.shaders.camera_view_defaults

import com.skamz.shadercam.shaders.util.*

class TextureOverlayShaderData {
    companion object {
        val shaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          vec4 overlay = texture2D(overlayTexture, vTextureCoord);
          gl_FragColor = color - (overlay * amt);
        """.trimIndent()
        val defaultImageUrl = TextureUtils.resourceIdToUri("R.drawable.noise_texture").toString()

        val params: MutableList<ShaderParam> = mutableListOf(
            TextureShaderParam("overlayTexture", defaultImageUrl),
            FloatShaderParam("amt", 0.75f, 0.0f, 1.0f)
        )
    }
}

val TextureOverlayShader = ShaderAttributes(
    name = "(Template) Texture Overlay",
    shaderMainText = TextureOverlayShaderData.shaderMainText,
    params = TextureOverlayShaderData.params,
//    templateParams = TextureOverlayShaderData.params
)