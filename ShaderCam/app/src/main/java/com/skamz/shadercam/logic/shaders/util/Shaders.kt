package com.skamz.shadercam.logic.shaders.util

import com.skamz.shadercam.logic.shaders.camera_view_defaults.*

val defaultShaderMainText: String = """
        vec3 image(vec2 uv, vec3 color) {
            return color;
        }
        """.trimIndent()

class Shaders {
    companion object {
        val noopShader: ShaderAttributes = NoopShader
        val brightShader: ShaderAttributes = BrightShader
        val tintShader: ShaderAttributes = TintShader
        val textureOverlayShader: ShaderAttributes = TextureOverlayShader
        val edgeDetectShader: ShaderAttributes = EdgeDetectShader
        val pixelateShader: ShaderAttributes = PixelateShader
        val feedbackShader: ShaderAttributes = FeedbackShader

        val all: List<ShaderAttributes> = listOf(
            noopShader,
            brightShader,
            tintShader,
            textureOverlayShader,
            edgeDetectShader,
            pixelateShader,
            FeedbackShader
        )
    }
}