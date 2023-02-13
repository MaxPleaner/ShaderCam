package com.skamz.shadercam.logic.shaders.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.opengl.*
import android.opengl.GLES10.glReadPixels
import android.opengl.GLES20.glReadPixels
import android.opengl.GLES30.glReadPixels
import android.os.ParcelFileDescriptor
import android.util.Log
import com.skamz.shadercam.logic.shaders.camera_view_defaults.TextureOverlayShaderData
import java.io.FileDescriptor
import java.net.URL
import java.nio.IntBuffer
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLContext.getEGL
import javax.microedition.khronos.opengles.GL10

// import javax.microedition.khronos.opengles.GL10


///////////////////////////////////////////////////////////////////////////
// Functions extracted from WizardCamera library. Credit where it's due. //
///////////////////////////////////////////////////////////////////////////

interface BitmapReadyCallbacks {
    fun onBitmapReady(bitmap: Bitmap?)
}

/* Usage code
   captureBitmap(new BitmapReadyCallbacks() {

        @Override
        public void onBitmapReady(Bitmap bitmap) {
            someImageView.setImageBitmap(bitmap);
        }
   });
*/

/* Usage code
   captureBitmap(new BitmapReadyCallbacks() {

        @Override
        public void onBitmapReady(Bitmap bitmap) {
            someImageView.setImageBitmap(bitmap);
        }
   });
*/
// supporting methods
fun captureBitmap(glSurfaceView: GLSurfaceView, bitmapReadyCallbacks: BitmapReadyCallbacks) {
    glSurfaceView.queueEvent(Runnable {
//        val egl = EGLContext.eg as GLES31
//        val gl = egl.eglGetCurrentContext().gl as GL10
        val snapshotBitmap =
            createBitmapFromGLSurface(0, 0, glSurfaceView.width, glSurfaceView.height)
        bitmapReadyCallbacks.onBitmapReady(snapshotBitmap)
//        runOnUiThread(Runnable { bitmapReadyCallbacks.onBitmapReady(snapshotBitmap) })
    })
}

// from other answer in this question
private fun createBitmapFromGLSurface(x: Int, y: Int, w: Int, h: Int): Bitmap? {
    val bitmapBuffer = IntArray(w * h)
    val bitmapSource = IntArray(w * h)
    val intBuffer: IntBuffer = IntBuffer.wrap(bitmapBuffer)
    intBuffer.position(0)
    try {
        GLES31.glReadPixels(x, y, w, h, GL10.GL_RGBA, GL10.GL_UNSIGNED_BYTE, intBuffer)
        var offset1: Int
        var offset2: Int
        for (i in 0 until h) {
            offset1 = i * w
            offset2 = (h - i - 1) * w
            for (j in 0 until w) {
                val texturePixel = bitmapBuffer[offset1 + j]
                val blue = texturePixel shr 16 and 0xff
                val red = texturePixel shl 16 and 0x00ff0000
                val pixel = texturePixel and -0xff0100 or red or blue
                bitmapSource[offset2 + j] = pixel
            }
        }
    } catch (e: GLException) {
        Log.e("DEBUG", "createBitmapFromGLSurface: " + e.message, e)
        return null
    }
    return Bitmap.createBitmap(bitmapSource, w, h, Bitmap.Config.ARGB_8888)
}

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
            uri: Uri? = null,
            bitmap: Bitmap? = null,
            isOES: Boolean = false,
            program: Int,
            textureId: Int? = null
        ): Int {
            var bmp: Bitmap
            if (bitmap == null) {
                bmp = bitmapFromUri(context, uri!!)
            } else {
                bmp = bitmap
            }
//            val textureId = loadTexture(context, IntArray(2))
//            GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + channelIdx)
            var newTextureId: Int = 0
            if (textureId != null) {
                loadTexture(bmp, IntArray(2), textureId=textureId)
                newTextureId = textureId
            } else {
                newTextureId = loadTexture(bmp, IntArray(2))
//            Log.e("DEBUG", "newTextureId: ${newTextureId}")
//            bmp.recycle() // Recycle the bitmap, since its data has been loaded into OpenGL.

                val sTextureLocation = GLES31.glGetUniformLocation(program, channelName)
//            Log.e("DEBUG", "sTextureLocation: ${sTextureLocation}")
                Log.e("DEBUG", "Binding texture $channelName to channel $channelIdx (newTextureId: $newTextureId) (location - $sTextureLocation) (active - ${GLES31.GL_TEXTURE0 + channelIdx})")
                GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + channelIdx)
//            if (isOES) {
//                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, newTextureId)
//                GLES31.glBindTexture(GLES31.GL_SAMPLER_2D, newTextureId)
//                GLES31.glBindTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES, newTextureId)
//            } else {
                GLES31.glBindTexture(GLES31.GL_TEXTURE_2D, newTextureId)
//            }
                GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
                GLES31.glUniform1i(sTextureLocation, channelIdx)
            }
            return newTextureId!!
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
            size: IntArray,
            textureId: Int? = null
        ): Int {
//            val texId = createTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES)

            var finalTextureId = textureId
            if (finalTextureId == null) {
                finalTextureId = createTexture(GLES31.GL_TEXTURE_2D)
                if (finalTextureId == 0) {
                    throw GLException(0, "Can't create texture!")
                }
                GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
            } else {
//                Log.e("DEBUG", "using existing texture (textureId: $textureId)   " )
//                GLES31.glActiveTexture(GLES31.GL_TEXTURE0 + 1)
                GLUtils.texSubImage2D(GLES31.GL_TEXTURE_2D, 0, 0, 0, bitmap)
//                GLES31.glActiveTexture(GLES31.GL_TEXTURE0)
//                GLUtils.texImage2D(GLES31.GL_TEXTURE_2D, 0, bitmap, 0)
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


//            bitmap.recycle()

            return finalTextureId
        }
    }
}