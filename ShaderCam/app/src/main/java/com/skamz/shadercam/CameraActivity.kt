package com.skamz.shadercam

//import androidx.camera.core.CameraSelector
//import androidx.camera.core.ImageCapture
//import androidx.camera.core.ImageCaptureException
//import androidx.camera.core.Preview
//import androidx.camera.lifecycle.ProcessCameraProvider
//import androidx.camera.video.*
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
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
import com.otaliastudios.cameraview.filter.SimpleFilter
import com.skamz.shadercam.databinding.ActivityCameraBinding
import java.io.*
import java.util.*
import java.util.concurrent.ExecutorService


class CameraActivity : AppCompatActivity() {
    var shaderText:String = EditorActivity.buildShader(EditorActivity.defaultShaderText);
    lateinit var camera: CameraView;
    var mode:Mode = Mode.PICTURE

    private lateinit var viewBinding: ActivityCameraBinding
//    private var imageCapture: ImageCapture? = null
//    private var videoCapture: VideoCapture<Recorder>? = null
//    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService

    companion object {
        private const val TAG = "DEBUG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

//    private fun takePhoto() {
//        // Get a stable reference of the modifiable image capture use case
////        val imageCapture = imageCapture ?: return
//
//        // Create time stamped name and MediaStore entry.
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
//            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P)    {
//                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/CameraX-Image")
//            }
//        }
//
//        // Create output options object which contains file + metadata
//        val outputOptions = ImageCapture.OutputFileOptions
//            .Builder(contentResolver,
//                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
//                contentValues)
//            .build()
//
//        // Set up image capture listener, which is triggered after photo has
//        // been taken
//        imageCapture.takePicture(
//            outputOptions,
//            ContextCompat.getMainExecutor(this),
//            object : ImageCapture.OnImageSavedCallback {
//                override fun onError(exc: ImageCaptureException) {
//                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
//                }
//
//                override fun
//                        onImageSaved(output: ImageCapture.OutputFileResults){
//                    val msg = "Photo capture succeeded: ${output.savedUri}"
//                    Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT).show()
//                    Log.d(TAG, msg)
//                }
//            }
//        )
//    }

    // Implements VideoCapture use case, including start and stop capturing.
//    private fun captureVideo() {
//        val videoCapture = this.videoCapture ?: return
//
//        val curRecording = recording
//        if (curRecording != null) {
//            Toast.makeText(baseContext, "Stopping Video", Toast.LENGTH_SHORT)
//                .show()
//            // Stop the current recording session.
//            curRecording.stop()
//            recording = null
//            return
//        }
//        Toast.makeText(baseContext, "Starting Video", Toast.LENGTH_SHORT)
//            .show()
//
//        // create and start a new recording session
//        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
//            .format(System.currentTimeMillis())
//        val contentValues = ContentValues().apply {
//            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
//            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
//            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
//                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/CameraX-Video")
//            }
//        }
//
//        val mediaStoreOutputOptions = MediaStoreOutputOptions
//            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
//            .setContentValues(contentValues)
//            .build()
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
//                when(recordEvent) {
//                    is VideoRecordEvent.Start -> {
////                        viewBinding.videoCaptureButton.apply {
////                            text = getString(R.string.stop_capture)
////                            isEnabled = true
////                        }
//                    }
//                    is VideoRecordEvent.Finalize -> {
//                        if (!recordEvent.hasError()) {
//                            val msg = "Video capture succeeded: " +
//                                    "${recordEvent.outputResults.outputUri}"
//                            Toast.makeText(baseContext, msg, Toast.LENGTH_SHORT)
//                                .show()
//                            Log.d(TAG, msg)
//                        } else {
//                            recording?.close()
//                            recording = null
//                            Log.e(TAG, "Video capture ends with error: " +
//                                    "${recordEvent.error}")
//                        }
////                        viewBinding.videoCaptureButton.apply {
////                            text = getString(R.string.start_capture)
////                            isEnabled = true
////                        }
//                    }
//                }
//            }
//    }

//    private fun startCamera() {
//        val cameraProviderFuture = ProcessCameraProvider.getInstance(this)
//
//        cameraProviderFuture.addListener({
//            // Used to bind the lifecycle of cameras to the lifecycle owner
//            val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()
//
//            // Preview
//            val preview = Preview.Builder()
//                .build()
//                .also {
//                    it.setSurfaceProvider(viewBinding.viewFinder.surfaceProvider)
//                }
//
//            val recorder = Recorder.Builder()
//                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
//                .build()
//            videoCapture = VideoCapture.withOutput(recorder)
//
//            imageCapture = ImageCapture.Builder()
//                .build()
//
//            // Select back camera as a default
//            val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
//
//            try {
//                // Unbind use cases before rebinding
//                cameraProvider.unbindAll()
//
//                // Bind use cases to camera
//                cameraProvider.bindToLifecycle(
//                    this, cameraSelector, preview, imageCapture, videoCapture)
//
//            } catch(exc: Exception) {
//                Log.e(TAG, "Use case binding failed", exc)
//            }
//
//        }, ContextCompat.getMainExecutor(this))
//    }

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

        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

//        startCamera()

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.camera_switch_front_back).setOnClickListener {
            camera.toggleFacing()
        }

        val captureBtn = findViewById<ImageButton>(R.id.camera_capture_btn)
        captureBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
//                takePhoto()
                camera.takePictureSnapshot() // See MyCameraListener for callback
                Toast.makeText(applicationContext, "Took Picture", Toast.LENGTH_SHORT).show()
            } else {
//                captureVideo()
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

        camera = findViewById<CameraView>(R.id.camera_view)
        camera.setLifecycleOwner(this)

        camera.addCameraListener(MyCameraListener(this))
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

        fun saveVideo2(videoResult: VideoResult) {
            val sourceuri = videoResult.file.toURI()

            val sourceFilename: String = sourceuri.path
            val time = System.currentTimeMillis().toString()
            val destinationFilename =
                Environment.getExternalStorageDirectory().path + File.separatorChar + "$time.mp4"
            var bis: BufferedInputStream? = null
            var bos: BufferedOutputStream? = null
            try {
                bis = BufferedInputStream(FileInputStream(sourceFilename))
                bos = BufferedOutputStream(FileOutputStream(destinationFilename, false))
                val buf = ByteArray(1024)
                bis.read(buf)
                do {
                    bos.write(buf)
                } while (bis.read(buf) !== -1)
            } catch (e: IOException) {
                e.printStackTrace()
            } finally {
                try {
                    if (bis != null) bis.close()
                    if (bos != null) bos.close()
                } catch (e: IOException) {
                    e.printStackTrace()
                }
            }
            Log.i("DEBUG", destinationFilename)
        }

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
//            saveVideo2(result)
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

