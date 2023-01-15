package com.skamz.shadercam.shaders.camera_view_defaults

import com.skamz.shadercam.shaders.util.AbstractShader.Companion.defaultShaderMainText
import com.skamz.shadercam.shaders.util.ShaderAttributes

val NoopShader = ShaderAttributes(
    "Pass Through",
    defaultShaderMainText,
    mutableListOf()
)