package com.skamz.shadercam

import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color.rgb
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.MediaStore.Video
import android.util.Log
import android.util.Log.*
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.filter.Filter
import com.otaliastudios.cameraview.filter.Filters
import com.otaliastudios.cameraview.filter.SimpleFilter
import com.otaliastudios.cameraview.filters.BlackAndWhiteFilter
import com.otaliastudios.cameraview.filters.DuotoneFilter
import com.otaliastudios.cameraview.filters.GrainFilter
import com.otaliastudios.cameraview.filters.PosterizeFilter
import com.skamz.shadercam.databinding.ActivityCameraBinding
import java.io.*


class CameraActivity : AppCompatActivity() {
    var shaderText:String = EditorActivity.buildShader(EditorActivity.defaultShaderText);
    lateinit var camera: CameraView;
    var mode:Mode = Mode.PICTURE

    private lateinit var viewBinding: ActivityCameraBinding

    companion object {
        private const val TAG = "DEBUG"
    }

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

        Log.i("DEBUG", "Camera on  create")

        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<Button>(R.id.shader_editor_link).setOnClickListener {
            val intent = Intent(this, ShaderSelectActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.camera_switch_front_back).setOnClickListener {
            camera.toggleFacing()
        }

        val captureBtn = findViewById<ImageButton>(R.id.camera_capture_btn)
        captureBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                camera.takePictureSnapshot() // See MyCameraListener for callback
                Toast.makeText(applicationContext, "Took Picture", Toast.LENGTH_SHORT).show()
            } else {
                if (camera.isTakingVideo) {
                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
                    camera.stopVideo()
                } else {
                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
                    val path = buildVideoPath()
                    camera.takeVideoSnapshot(File(path))
                }
            }
        }

        val switchModeBtn = findViewById<ImageButton>(R.id.switch_photo_video)
        switchModeBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                mode = Mode.VIDEO
                switchModeBtn.setImageResource(R.drawable.camera_mode_icon)
                Toast.makeText(applicationContext, "Switched to Video mode", Toast.LENGTH_SHORT).show()
            } else {
                mode = Mode.PICTURE
                switchModeBtn.setImageResource(R.drawable.video_mode_icon)
                Toast.makeText(applicationContext, "Switched to Picture mode", Toast.LENGTH_SHORT).show()
            }
            camera.mode = mode
        }

        camera = findViewById(R.id.camera_view)
        camera.setLifecycleOwner(this)

        camera.addCameraListener(MyCameraListener(this))

        setShader("foo")
    }

    fun buildPhotoPath(): String {
        val filename = System.currentTimeMillis().toString()
        return "${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$filename.jpg"
    }

    private fun buildVideoPath(): String {
        val filename = System.currentTimeMillis().toString()
        return "${getExternalFilesDir(Environment.DIRECTORY_MOVIES)}/$filename.mp4"
    }

    class MyCameraListener(parent: CameraActivity) : CameraListener() {
        var cameraActivity: CameraActivity = parent
        val contentResolver = cameraActivity.contentResolver
        override fun onPictureTaken(result: PictureResult) {
            result.toBitmap { bmp ->
                val path = cameraActivity.buildPhotoPath()
                i(TAG, path)
                cameraActivity.saveImage(bmp!!, path, cameraActivity)
            }
        }

        // Results in corrupt video for some reason. Very frustrating.
        private fun saveVideo(result: VideoResult) {
            val videoFile = result.file

            val contentValues = ContentValues()
            contentValues.put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
//            contentValues.put(Video.Media.DISPLAY_NAME, System.currentTimeMillis().toString())
            contentValues.put(Video.Media.MIME_TYPE, "video/mp4")
            contentValues.put(Video.Media.DATA, videoFile.absolutePath)
//            contentValues.put(Video.Media.RELATIVE_PATH, "Movies/ShaderCam")

            val uri = contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, contentValues)

            //            contentValues.put(Video.Media.DATA, Uri.fromFile(videoFile).toString())

//            contentValues.put(Video.Media.DATA, videoFile.readBytes())

            val codec = result.videoCodec
            val location = result.location
            val sizeKb = result.file.length() / 1024
            i(TAG, "$sizeKb KB in video file")
            i(TAG, videoFile.absolutePath)
            i(TAG, "CODEC $codec")
            i(TAG, "LOCATION: $location")

//            VideoPreviewActivity.videoResult = result
            VideoPreviewActivity.uri = uri
            val intent = Intent(cameraActivity, VideoPreviewActivity::class.java)
            cameraActivity.startActivity(intent)
            i(TAG, uri?.path!!)

//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//
//            }

//            val mediaStoreOutputOptions = MediaStoreOutputOptions
//                .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)s
//                .setContentValues(contentValues)
//                .build()

//            contentResolver.insert(MediaStore.Files.getContentUri("external"), contentValues)
//            contentResolver.insert(Video.Media.EXTERNAL_CONTENT_URI, contentValues)


//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//


            //        recording = videoCapture.output
//            .prepareRecording(this, mediaStoreOutputOptions)
//            .apply {
//                if (PermissionChecker.checkSelfPermission(this@CameraActivity,
//                        Manifest.permission.RECORD_AUDIO) ==
//                    PermissionChecker.PERMISSION_GRANTED)
//                {
//                    withAudioEnabled()
//                }
//            }
//            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
        }

        override fun onVideoTaken(result: VideoResult) {
            super.onVideoTaken(result)
            VideoPreviewActivity.videoResult = result
            val intent = Intent(cameraActivity, VideoPreviewActivity::class.java)
            cameraActivity.startActivity(intent)
//            saveVideo(result)
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

    private fun setShader(shaderText: String) {
//        val filter: Filter = SimpleFilter(shaderText)

//        Log.i(TAG, shaderText)
//        val filter = Filters.DUOTONE
        val filter = Filters.GRAIN.newInstance()
//        filter.
        camera.filter = filter;

        val filterControl: GrainFilter = camera.filter as GrainFilter
        filterControl.strength =
//        duotoneFilter.firstColor = rgb(255, 0, 0)
//        duotoneFilter.secondColor = rgb(0, 255, 0)
    }
}

