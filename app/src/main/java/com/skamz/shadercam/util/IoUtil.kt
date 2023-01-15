package com.skamz.shadercam.util

import android.content.ContentValues
import android.graphics.Bitmap
import android.os.Environment
import android.provider.MediaStore
import com.skamz.shadercam.activities.CameraActivity
import java.io.ByteArrayOutputStream
import java.io.File

class IoUtil {

    companion object {
        fun saveImage(bmp: Bitmap, path: String, activity: CameraActivity) {
            ByteArrayOutputStream().apply {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
            }
            val imageFile = File(path)
            val contentResolver = ContentValues().apply {
                put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
                put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
                put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
            }

            activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentResolver).apply {
                bmp.compress(Bitmap.CompressFormat.JPEG, 100, activity.contentResolver.openOutputStream(this!!))
            }
        }

        fun buildPhotoPath(activity: CameraActivity): String {
            val filename = System.currentTimeMillis().toString()
            return "${activity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$filename.jpg"
        }

        fun buildVideoPath(activity: CameraActivity): String {
            val filename = System.currentTimeMillis().toString()
            return "${activity.getExternalFilesDir(Environment.DIRECTORY_MOVIES)}/$filename.mp4"
        }

    }
}