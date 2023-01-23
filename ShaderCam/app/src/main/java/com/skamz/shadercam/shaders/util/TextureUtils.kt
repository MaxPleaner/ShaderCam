package com.skamz.shadercam.shaders.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.opengl.GLException
import android.opengl.GLUtils
import androidx.annotation.RawRes
import javax.microedition.khronos.opengles.GL10

///////////////////////////////////////////////////////////////////////////
// Functions extracted from WizardCamera library. Credit where it's due. //
///////////////////////////////////////////////////////////////////////////

class TextureUtils {

    /**
     * Passes params
     * @param program OGL program
     */
    // Note: context will have .resources called on it
    // channelIdx should be greater than 0, since 0 is the default texture (from the camera)
     fun setTextureParam(context: Context, channelName: String, channelIdx: Int, bmp: Bitmap, program: Int) {
        val textureId = loadTexture(context, bmp, IntArray(2))

        val sTextureLocation = GLES31.glGetUniformLocation(program, channelName)
        GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + channelIdx)
        GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, textureId)
        GLES31.glUniform1i(sTextureLocation, 1)
    }

    /**
     * Creates texture of type [type]
     * @return id (aka "name") of created  texture
     */
    private fun createTexture(type: Int): Int {
        // Generates id and  binds it to a texture object
        val genBuf = IntArray(1)
        GLES31.glGenTextures(1, genBuf, 0)
        GLES31.glBindTexture(type, genBuf[0])

        // Set texture default draw parameters
        if (type == GLES11Ext.GL_TEXTURE_EXTERNAL_OES) {
            // For external texture (from a camera, for example)
            GLES31.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
            GLES31.glTexParameterf(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
            GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_S, GL10.GL_CLAMP_TO_EDGE)
            GLES31.glTexParameteri(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, GL10.GL_TEXTURE_WRAP_T, GL10.GL_CLAMP_TO_EDGE)

        } else {
            // For 2D texture (from some image, for example)
            GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_MIN_FILTER, GL10.GL_LINEAR.toFloat())
            GLES31.glTexParameterf(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_MAG_FILTER, GL10.GL_LINEAR.toFloat())
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT)
            GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT)
        }

        return genBuf[0]
    }

    /**
     * Load a bitmap resource into a texture
     */
    fun loadTexture(context: Context, bmp: Bitmap, size: IntArray): Int {
        val texId = createTexture(GLES31.GL_TEXTURE_2D)

        if(texId == 0) {
            throw GLException(0, "Can't create texture!")
        }

        // Decode bounds
//        val options = BitmapFactory.Options()
//        options.inScaled = false
//        options.inJustDecodeBounds = true
//
//        BitmapFactory.decodeResource(context.resources, resourceId, options)
//
//        // Set return size
//        size[0] = options.outWidth
//        size[1] = options.outHeight
//
//        // Decode
//        options.inJustDecodeBounds = false
//        val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

        // Load the bitmap into the bound texture.
        GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bmp, 0)

        // Recycle the bitmap, since its data has been loaded into OpenGL.
        bmp.recycle()

        return texId
    }
}