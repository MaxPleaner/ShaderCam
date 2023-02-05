package com.skamz.shadercam.logic.shaders.util
import com.skamz.shadercam.logic.shaders.util.ShaderParam

data class ShaderAttributes(
    val name: String,
    val shaderMainText: String,
    val params: MutableList<ShaderParam>,
)