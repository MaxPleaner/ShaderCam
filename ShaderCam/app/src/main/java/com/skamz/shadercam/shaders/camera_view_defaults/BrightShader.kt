package com.skamz.shadercam.shaders.camera_view_defaults

import com.skamz.shadercam.shaders.util.ShaderParam
import com.skamz.shadercam.shaders.util.ShaderAttributes

val shaderMainText: String = """
      vec4 color = texture2D(sTexture, vTextureCoord);
      gl_FragColor = brightness * color;
    """.trimIndent()

val params: MutableList<ShaderParam> = mutableListOf(
    ShaderParam("brightness", 1.0f, 0.0f, 5.0f)
)

val BrightShader = ShaderAttributes(
    "(Template) Brightness Adjust",
    shaderMainText,
    params
)