package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.FloatShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes

class EdgeDetectShaderData {
    companion object {
        val shaderMainText: String = """
        ////////////////////////////////////////
        // CREDIT: shadertoy.com/view/ctlSzM
        ////////////////////////////////////////
        
        void make_kernel(inout vec4 n[9], vec2 coord)
        {
        	float w = 1./iResolution.x;
        	float h = 1./iResolution.y;

        	n[0] = sampleCamera(coord + vec2( -w, -h));
        	n[1] = sampleCamera(coord + vec2(0.0, -h));
        	n[2] = sampleCamera(coord + vec2(  w, -h));
        	n[3] = sampleCamera(coord + vec2( -w, 0.0));
        	n[4] = sampleCamera(coord);
        	n[5] = sampleCamera(coord + vec2(  w, 0.0));
        	n[6] = sampleCamera(coord + vec2( -w, h));
        	n[7] = sampleCamera(coord + vec2(0.0, h));
        	n[8] = sampleCamera(coord + vec2(  w, h));
        }

        vec3 mainImage(vec2 uv, vec3 color)
        {
            vec4 n[9];
        	make_kernel( n, uv );

        	vec4 sobel_edge_h = n[2] + (2.0*n[5]) + n[8] - (n[0] + (2.0*n[3]) + n[6]);
          	vec4 sobel_edge_v = n[0] + (2.0*n[1]) + n[2] - (n[6] + (2.0*n[7]) + n[8]);
        	vec4 sobel = sqrt((sobel_edge_h * sobel_edge_h) + (sobel_edge_v * sobel_edge_v));

        	vec3 newColor = 1.0 - sobel.rgb;

            newColor *= contrast;
            newColor += vec3(brightness, brightness, brightness);

            return newColor, 1.0);
        }
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            FloatShaderParam("brightness", 1.0f, 0.5f, 1.5f),
            FloatShaderParam("contrast", 1.0f, 0.5f, 3.0f),
        )
    }
}


val EdgeDetectShader = ShaderAttributes(
    "004 - Edge Detect",
    EdgeDetectShaderData.shaderMainText,
    EdgeDetectShaderData.params,
)