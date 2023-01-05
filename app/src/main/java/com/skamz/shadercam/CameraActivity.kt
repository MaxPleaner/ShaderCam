package com.skamz.shadercam

import android.content.ContentValues
import android.content.ContextWrapper
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Camera
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.filter.Filter
import com.otaliastudios.cameraview.filter.SimpleFilter
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


class CameraActivity : AppCompatActivity() {
    var shaderText:String = EditorActivity.buildShader(EditorActivity.defaultShaderText);
    lateinit var camera: CameraView;

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


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_camera)

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.camera_switch_front_back).setOnClickListener {
            camera.toggleFacing()
        }

        findViewById<ImageButton>(R.id.camera_take_photo).setOnClickListener {
            camera.takePicture()
        }

        camera = findViewById<CameraView>(R.id.camera_view)
        camera.setLifecycleOwner(this)

        camera.addCameraListener(MyCameraListener(this))

    }

    class MyCameraListener(parent: CameraActivity) : CameraListener() {
        var cameraActivity: CameraActivity = parent
        override fun onPictureTaken(result: PictureResult) {
            result.toBitmap { bmp ->
                val filename = System.currentTimeMillis().toString()
                val path = "${cameraActivity.getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$filename.jpg/"
                Log.i("DEBUG", path)
                cameraActivity.saveImage(bmp!!, path, cameraActivity)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val newShaderText = intent?.extras?.getString("shader")
        if (newShaderText != null) {
            shaderText = newShaderText
        } else {
            intent?.putExtra("shader", shaderText)
        }
        setShader(shaderText)
    }

    fun setShader(shaderText: String) {
        val filter: Filter = SimpleFilter(shaderText)
        camera.filter = filter;
    }
}

