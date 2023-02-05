package com.skamz.shadercam.shaders.camera_view_defaults

import com.skamz.shadercam.shaders.util.ShaderAttributes
import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.defaultShaderMainText

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
//    templateParams = NoopShaderData.params
)