package com.skamz.shadercam.shaders.camera_view_defaults

import android.graphics.Color
import com.skamz.shadercam.shaders.util.*

class TextureOverlayShaderData {
    companion object {
        val shaderMainText: String = """
          vec4 color = texture2D(sTexture, vTextureCoord);
          vec4 overlay = texture2D(overlayTexture, vTextureCoord);
          gl_FragColor = overlay * color;
        """.trimIndent()

        // Set in CameraActivity.onCreate because it needs to use context to build a Uri string
        lateinit var default: String

        val params: MutableList<ShaderParam> = mutableListOf(
            TextureShaderParam("overlayTexture", null)
        )
    }
}

val TextureOverlayShader = ShaderAttributes(
    "(Template) Texture Overlay",
    TextureOverlayShaderData.shaderMainText,
    TextureOverlayShaderData.params
)