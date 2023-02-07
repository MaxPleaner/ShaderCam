package com.skamz.shadercam.logic.shaders.util

data class ShaderAttributes(
    val name: String,
    val shaderMainText: String,
    val params: MutableList<ShaderParam>,
    var isTemplate: Boolean = false,
    var isPublic: Boolean = false
)