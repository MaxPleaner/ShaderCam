package com.skamz.shadercam.shaders.util

@kotlinx.serialization.Serializable
sealed interface ShaderParam {
    val paramName: String
    val paramType: String
}

@kotlinx.serialization.Serializable
data class FloatShaderParam(
    override val paramName: String,
    var default: Float,
    var min: Float,
    var max: Float,
    override val paramType: String = "float",
) : ShaderParam

@kotlinx.serialization.Serializable
data class ColorShaderParam(
    override val paramName: String,
    var default: Int,
    override val paramType: String = "color",
) : ShaderParam