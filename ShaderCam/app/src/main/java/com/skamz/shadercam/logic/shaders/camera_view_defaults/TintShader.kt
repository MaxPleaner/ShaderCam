package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.ColorShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes

class TintShaderData {
    companion object {
        val shaderMainText: String = """
            vec3 image(vec2 uv, vec3 color) {
                return tint * color;
            }
        }
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            ColorShaderParam("tint", android.graphics.Color.BLUE)
        )

    }
}

val TintShader = ShaderAttributes(
    "002 - Tint - Color Param Example",
    TintShaderData.shaderMainText,
    TintShaderData.params,
)