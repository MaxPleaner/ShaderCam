package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.*

class TextureOverlayShaderData {
    companion object {
        val shaderMainText: String = """
        vec3 mainImage(vec2 uv, vec3 color) {
            vec3 overlay = texture2D(overlayTexture, uv).rgb;
            return color - (overlay * amt);
        }    
        """.trimIndent()
        val defaultImageUrl = TextureUtils.resourceIdToUri("R.drawable.noise_texture").toString()

        val params: MutableList<ShaderParam> = mutableListOf(
            TextureShaderParam("overlayTexture", defaultImageUrl),
            FloatShaderParam("amt", 0.75f, 0.0f, 1.0f)
        )
    }
}

val TextureOverlayShader = ShaderAttributes(
    name = "003 - Overlay - Texture Param Example",
    shaderMainText = TextureOverlayShaderData.shaderMainText,
    params = TextureOverlayShaderData.params,
)