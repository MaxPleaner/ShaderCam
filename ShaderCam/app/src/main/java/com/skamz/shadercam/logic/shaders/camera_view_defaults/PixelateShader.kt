package com.skamz.shadercam.logic.shaders.camera_view_defaults

import com.skamz.shadercam.logic.shaders.util.FloatShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderParam
import com.skamz.shadercam.logic.shaders.util.ShaderAttributes

class PixelateShaderData {
    companion object {
        val shaderMainText: String = """   
        vec3 getPos(vec2 uv)
        {
            return floor(texture2D(sTexture, floor(uv * iResolution.xy / colStepDiff) * colStepDiff / iResolution.xy).rgb * 8.) / 8.;
        }
        
        vec3 colorize(vec3 col)
        {
            float avgCol = (col.r + col.g + col.b) / 3.;
            if(avgCol < .2) col.b += .1;
            if(avgCol > .8) col.g += .1;
            if(abs(col.r - col.g) > .2 && abs(col.g / col.b - 1.) > 1.) col.r = col.g + .2 * sign(col.r - col.g);
            if(abs(col.b - col.g) > .2 && abs(col.g / col.r - 1.) > 1.) col.b = col.g + .2 * sign(col.b - col.g);
            return col*.45+.25;
        }
    
        vec3 getCol(vec2 uv)
        {
            vec3 col = getPos(uv);
            vec3 stepFactor = vec3(colStepDiff * 2. / iResolution.x, colStepDiff * 2. / iResolution.y, 0.);
            vec3 colU = getPos(uv + stepFactor.zy);
            vec3 colD = getPos(uv - stepFactor.zy);
            vec3 colR = getPos(uv + stepFactor.xz);
            vec3 colL = getPos(uv - stepFactor.xz);
            vec3 avgCol = (colU+colR+colD+colL) / 4.;
            float rDiff = abs(col.r - avgCol.r);
            float gDiff = abs(col.g - avgCol.g);
            float bDiff = abs(col.b - avgCol.b);
            if((rDiff<.3?1:0) + (gDiff<.3?1:0) + (bDiff<.3?1:0) == 1){
                if(rDiff < .3) col.r = (col.g + col.b) / 3.;
                if(gDiff < .3) col.g = (col.b + col.r) / 3.;
                if(bDiff < .3) col.b = (col.g + col.r) / 3.;
            }
            return floor((colorize(col) * 6. - colorize(colU) - colorize(colD) - colorize(colR) - colorize(colL)) / 2. * 32.) / 32.;
        }
        
        void main()
        {   
            vec2 uv = vTextureCoord;
            gl_FragColor = vec4(getCol(uv),1.);
        }              
    """.trimIndent()

        val params: MutableList<ShaderParam> = mutableListOf(
            FloatShaderParam("colStepDiff", 4.0f, 1.0f, 10.0f)
        )
    }
}

val PixelateShader = ShaderAttributes(
    "005 - Pixelate",
    PixelateShaderData.shaderMainText,
    PixelateShaderData.params,
)