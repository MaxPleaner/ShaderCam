package com.skamz.shadercam.shaders.util

interface ShaderParam {
    val paramName: String
    val type: String
}

data class FloatShaderParam(
    override val paramName: String,
    val default: Float,
    val min: Float,
    val max: Float,
    override val type: String = "float",
) : ShaderParam

data class ColorShaderParam(
    override val paramName: String,
    val default: Int,
    override val type: String = "color",
) : ShaderParam