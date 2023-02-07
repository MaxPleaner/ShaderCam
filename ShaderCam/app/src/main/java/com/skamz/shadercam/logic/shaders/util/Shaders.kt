package com.skamz.shadercam.logic.shaders.util

import com.skamz.shadercam.logic.shaders.camera_view_defaults.*


val defaultShaderMainText: String = """
        void main() {
            vec2 uv = vTextureCoord;
            vec4 color = texture2D(sTexture, vTextureCoord);
            gl_FragColor = color;
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

        val all: List<ShaderAttributes> = listOf(
            noopShader,
            brightShader,
            tintShader,
            textureOverlayShader,
            edgeDetectShader,
            pixelateShader
        )
    }
}