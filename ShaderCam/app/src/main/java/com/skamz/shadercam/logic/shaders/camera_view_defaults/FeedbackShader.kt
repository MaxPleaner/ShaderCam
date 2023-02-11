package com.skamz.shadercam.logic.shaders.camera_view_defaults
import com.skamz.shadercam.logic.shaders.util.*

class FeedbackShaderData {
    companion object {
        val shaderMainText: String = """
        vec3 image(vec2 uv, vec3 color) {
            vec3 overlay = texture2D(prevFrame, uv).rgb;
            return mix(color, overlay, amt);
        }    
        """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            FloatShaderParam("amt", 0.75f, 0.0f, 1.0f)
        )
    }
}

val FeedbackShader = ShaderAttributes(
    name = "006 - Feedback",
    shaderMainText = FeedbackShaderData.shaderMainText,
    params = FeedbackShaderData.params,
)