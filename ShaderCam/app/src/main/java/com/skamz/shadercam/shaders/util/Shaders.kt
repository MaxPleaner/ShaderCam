package com.skamz.shadercam.shaders.util

import com.skamz.shadercam.shaders.camera_view_defaults.BrightShader
import com.skamz.shadercam.shaders.camera_view_defaults.NoopShader

class Shaders {
    companion object {
        val noopShader: ShaderAttributes = NoopShader
        val brightShader: ShaderAttributes = BrightShader
    }
}