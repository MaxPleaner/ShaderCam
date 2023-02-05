package com.skamz.shadercam.shaders.util

import android.R
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.GLES11Ext
import android.opengl.GLES31
import android.opengl.GLException
import android.opengl.GLUtils
import android.os.ParcelFileDescriptor
import android.util.Log
import com.skamz.shadercam.shaders.camera_view_defaults.TextureOverlayShaderData
import java.io.FileDescriptor
import java.net.URL
import javax.microedition.khronos.opengles.GL10


///////////////////////////////////////////////////////////////////////////
// Functions extracted from WizardCamera library. Credit where it's due. //
///////////////////////////////////////////////////////////////////////////

class TextureUtils {

    companion object {

        // There are various answers on the internet for how to do this,
        // but I found it difficult to load the Bitmap from them.
        // So, this just uses a custom solution in conjunction with .getIdentifier()
        // Basically, we store the identifier as a string and prepend a custom protocol ..
        // see bitmapFromUri for how it's parsed.
        fun resourceIdToUri(resourcePath: String): Uri {
            return Uri.parse("hardcodedResource://${resourcePath}")
        }

        fun bitmapFromUri(context: Context, uri: Uri): Bitmap {
            when (uri.scheme) {
                "content" -> {
                    val parcelFileDescriptor: ParcelFileDescriptor =
                        context.contentResolver.openFileDescriptor(uri, "r")
                            ?: return bitmapFromUri(context, Uri.parse(TextureOverlayShaderData.defaultImageUrl))
                    val fileDescriptor: FileDescriptor = parcelFileDescriptor.fileDescriptor
                    val image: Bitmap = BitmapFactory.decodeFileDescriptor(fileDescriptor)
                    parcelFileDescriptor.close()
                    return image
                }
                "http", "https" -> {
                    return bitmapFromHttpUri(uri)
                }
                "hardcodedResource" -> {
                    val uriString = uri.toString().replace("hardcodedResource://", "")
                    val components = uriString.split(".")
                    val type = components[1]!!
                    val resourceName = components[2]!!
                    val id = context.resources.getIdentifier(resourceName, type, context.packageName)
                    return BitmapFactory.decodeResource(context.resources, id)
                }
                else -> {
                    Log.e("DEBUG", "UNHANDLED SCHEME ${uri.scheme}")
                    throw Exception("unhandled scheme ${uri.scheme}")
                }
            }
        }

        fun bitmapFromHttpUri(uri: Uri): Bitmap {
            var url = URL(uri.toString())
            return BitmapFactory.decodeStream(url.openConnection().getInputStream())
        }

        /**
         * Passes params
         * @param program OGL program
         */
        // Note: context will have .resources called on it
        // channelIdx should be greater than 0, since 0 is the default texture (from the camera)
        fun setTextureParam(
            context: Context,
            channelName: String,
            channelIdx: Int,
            uri: Uri,
            program: Int
        ) {
//            val textureId = loadTexture(context, IntArray(2))
            val bmp = bitmapFromUri(context, uri)
            val textureId = loadTexture(bmp, IntArray(2))
//            Log.e("DEBUG", "textureId: ${textureId}")
//            bmp.recycle() // Recycle the bitmap, since its data has been loaded into OpenGL.

            val sTextureLocation = GLES31.glGetUniformLocation(program, channelName)
//            Log.e("DEBUG", "sTextureLocation: ${sTextureLocation}")
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
                GLES31.glTexParameterf(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_LINEAR.toFloat()
                )
                GLES31.glTexParameterf(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_LINEAR.toFloat()
                )
                GLES31.glTexParameteri(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_S,
                    GL10.GL_CLAMP_TO_EDGE
                )
                GLES31.glTexParameteri(
                    GLES11Ext.GL_TEXTURE_EXTERNAL_OES,
                    GL10.GL_TEXTURE_WRAP_T,
                    GL10.GL_CLAMP_TO_EDGE
                )

            } else {
                // For 2D texture (from some image, for example)
                GLES31.glTexParameterf(
                    GLES31.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MIN_FILTER,
                    GL10.GL_LINEAR.toFloat()
                )
                GLES31.glTexParameterf(
                    GLES31.GL_TEXTURE_2D,
                    GL10.GL_TEXTURE_MAG_FILTER,
                    GL10.GL_LINEAR.toFloat()
                )
                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_S, GL10.GL_REPEAT)
                GLES31.glTexParameteri(GLES31.GL_TEXTURE_2D, GL10.GL_TEXTURE_WRAP_T, GL10.GL_REPEAT)
            }

            return genBuf[0]
        }

        /**
         * Load a bitmap resource into a texture
         */
        fun loadTexture(
//            bmp: Bitmap,
            bitmap: Bitmap,
            size: IntArray
        ): Int {
//            val texId = createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)
            val texId = createTexture(GLES31.GL_TEXTURE_2D)

            if (texId == 0) {
                throw GLException(0, "Can't create texture!")
            }

//            val resourceId = com.skamz.shadercam.R.drawable.noise_texture
////            val resourceId = com.skamz.shadercam.R.raw.noise_texture;
//            // Decode bounds
//                    val options = BitmapFactory.Options()
//                    options.inScaled = false
//                    options.inJustDecodeBounds = true
//            //
//                    BitmapFactory.decodeResource(context.resources, resourceId, options)
//            //
//            //        // Set return size
//                    size[0] = options.outWidth
//                    size[1] = options.outHeight
//            //
//            //        // Decode
//                    options.inJustDecodeBounds = false
//                    val bitmap = BitmapFactory.decodeResource(context.resources, resourceId, options)

            // Load the bitmap into the bound texture.
            GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()

            return texId
        }
    }
}