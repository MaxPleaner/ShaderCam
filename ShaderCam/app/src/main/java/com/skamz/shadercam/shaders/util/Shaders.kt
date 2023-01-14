package com.skamz.shadercam.shaders.util

import com.skamz.shadercam.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader

interface ShaderParam {
    val paramName: String
    val default: Float
    val min: Float
    val max: Float
}

class Shaders {
    companion object {
        val noopShader: NoopShader = NoopShader()
        val brightShader: BrightShader = BrightShader()
    }
}