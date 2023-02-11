package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.FloatShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes

class BrightShaderData {
    companion object {
        val shaderMainText: String = """
        vec3 image(vec2 uv, vec3 color) {
          return brightness * color;
        }            
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            FloatShaderParam("brightness", 1.0f, 0.0f, 5.0f)
        )
    }
}

val BrightShader = ShaderAttributes(
    "001 - Brightness - Float Param Example",
    BrightShaderData.shaderMainText,
    BrightShaderData.params,
)