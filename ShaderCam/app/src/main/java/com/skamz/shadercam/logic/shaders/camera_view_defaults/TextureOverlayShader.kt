package com.skamz.shadercam.logic.shaders.camera_view_defaults

import android.net.Uri
import com.skamz.shadercam.logic.shaders.util.*

class TextureOverlayShaderData {
    companion object {
        val shaderMainText: String = """
        vec3 image(vec2 uv, vec3 color) {
            vec3 overlay = texture2D(overlayTexture, uv).rgb;
            vec3 overlay2 = texture2D(overlayTexture2, uv).rgb;
            return mix(mix(color, overlay, amt), overlay2, amt2);
        }    
        """.trimIndent()
        val defaultImageUrl = TextureUtils.resourceIdToUri("R.drawable.noise_texture").toString()

        val params: MutableList<ShaderParam> = mutableListOf(
            TextureShaderParam("overlayTexture", defaultImageUrl),
            TextureShaderParam("overlayTexture2", "https://y2w9c4y8.stackpathcdn.com/showimg_hhl59_full.jpg"),
            FloatShaderParam("amt", 0.5f, 0.0f, 1.0f),
            FloatShaderParam("amt2", 0.5f, 0.0f, 1.0f)
        )
    }
}

val TextureOverlayShader = ShaderAttributes(
    name = "003 - Overlay - Texture Param Example",
    shaderMainText = TextureOverlayShaderData.shaderMainText,
    params = TextureOverlayShaderData.params,
)