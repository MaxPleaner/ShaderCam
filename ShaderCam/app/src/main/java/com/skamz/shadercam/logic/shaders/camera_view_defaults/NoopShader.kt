package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.defaultShaderMainText
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes

class NoopShaderData {
    companion object {
        val shaderMainText = defaultShaderMainText
        val params: MutableList<ShaderParam> = mutableListOf()
    }
}
val NoopShader = ShaderAttributes(
    "(Template) Pass Through",
    NoopShaderData.shaderMainText,
    NoopShaderData.params,
)