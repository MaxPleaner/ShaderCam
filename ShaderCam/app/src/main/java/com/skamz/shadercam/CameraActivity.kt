package com.skamz.shadercam

import android.Manifest
import android.content.ContentValues
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.View
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.video.VideoCapture
import androidx.core.content.ContextCompat
import androidx.core.content.PermissionChecker
import com.google.common.util.concurrent.ListenableFuture
import com.otaliastudios.cameraview.CameraListener
import com.otaliastudios.cameraview.CameraView
import com.otaliastudios.cameraview.PictureResult
import com.otaliastudios.cameraview.VideoResult
import com.otaliastudios.cameraview.controls.Mode
import com.otaliastudios.cameraview.filter.Filter
import com.otaliastudios.cameraview.filter.SimpleFilter
import java.io.ByteArrayOutputStream
import java.io.File
import com.skamz.shadercam.databinding.ActivityCameraBinding
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ExecutorService


class CameraActivity : AppCompatActivity() {
    var shaderText:String = EditorActivity.buildShader(EditorActivity.defaultShaderText);
    lateinit var camera: CameraView;
    var mode:Mode = Mode.PICTURE
    private var lensFacing = CameraSelector.LENS_FACING_BACK

    private lateinit var viewBinding: ActivityCameraBinding
    private var imageCapture: ImageCapture? = null
    private var videoCapture: VideoCapture<Recorder>? = null
    private var recording: Recording? = null
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var cameraProviderFuture:
            ListenableFuture<ProcessCameraProvider>
    private var lastToast: Toast? = null

    companion object {
        private const val TAG = "DEBUG"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
        private const val REQUEST_CODE_PERMISSIONS = 10
    }

    private fun immediateToast(msg: String, duration: Int = Toast.LENGTH_SHORT) {
        lastToast?.cancel()
        lastToast = Toast.makeText(baseContext, msg, duration)
        lastToast?.show()
    }

    private fun takePhoto() {
        val imageCapture = imageCapture ?: return

        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg")
            if(Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/ShaderCam-Image")
            }
        }

        // Create output options object which contains file + metadata
        val outputOptions = ImageCapture.OutputFileOptions
            .Builder(contentResolver,
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                contentValues)
            .build()

        // Set up image capture listener, which is triggered after photo has
        // been taken
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(this),
            object : ImageCapture.OnImageSavedCallback {
                override fun onError(exc: ImageCaptureException) {
                    Log.e(TAG, "Photo capture failed: ${exc.message}", exc)
                }

                override fun
                        onImageSaved(output: ImageCapture.OutputFileResults){
                    val msg = "Photo capture succeeded: ${output.savedUri}"
                    immediateToast(msg)
                    Log.d(TAG, msg)
                }
            }
        )
    }

    // Implements VideoCapture use case, including start and stop capturing.
    private fun captureVideo() {
        val videoCapture = this.videoCapture ?: return

        val curRecording = recording
        if (curRecording != null) {
            immediateToast("Stopping Video")
            // Stop the current recording session.
            curRecording.stop()
            recording = null
            return
        }
        immediateToast("Starting Video")

        // create and start a new recording session
        val name = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
            .format(System.currentTimeMillis())

        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, name)
            put(MediaStore.MediaColumns.MIME_TYPE, "video/mp4")
            if (Build.VERSION.SDK_INT > Build.VERSION_CODES.P) {
                put(MediaStore.Video.Media.RELATIVE_PATH, "Movies/ShaderCam-Video")
            }
        }

        val mediaStoreOutputOptions = MediaStoreOutputOptions
            .Builder(contentResolver, MediaStore.Video.Media.EXTERNAL_CONTENT_URI)
            .setContentValues(contentValues)
            .build()
        recording = videoCapture.output
            .prepareRecording(this, mediaStoreOutputOptions)
            .apply {
                if (PermissionChecker.checkSelfPermission(this@CameraActivity,
                        Manifest.permission.RECORD_AUDIO) ==
                    PermissionChecker.PERMISSION_GRANTED)
                {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(this)) { recordEvent ->
                when(recordEvent) {
                    is VideoRecordEvent.Start -> {
//                        viewBinding.videoCaptureButton.apply {
//                            text = getString(R.string.stop_capture)
//                            isEnabled = true
//                        }
                    }
                    is VideoRecordEvent.Finalize -> {
                        if (!recordEvent.hasError()) {
                            val msg = "Video capture succeeded: " +
                                    "${recordEvent.outputResults.outputUri}"
                            immediateToast(msg)
                            Log.d(TAG, msg)
                        } else {
                            recording?.close()
                            recording = null
                            Log.e(TAG, "Video capture ends with error: " +
                                    "${recordEvent.error}")
                        }
//                        viewBinding.videoCaptureButton.apply {
//                            text = getString(R.string.start_capture)
//                            isEnabled = true
//                        }
                    }
                }
            }
    }

    private fun startCamera() {
        cameraProviderFuture = ProcessCameraProvider.getInstance(this)

        cameraProviderFuture.addListener({
            bindCameraUseCases()
        }, ContextCompat.getMainExecutor(this))
    }

    private fun bindCameraUseCases() {
        // Used to bind the lifecycle of cameras to the lifecycle owner
        val cameraProvider: ProcessCameraProvider = cameraProviderFuture.get()

        // Preview
        val preview = Preview.Builder()
            .build()
            .also {
                it.setSurfaceProvider(viewBinding.previewView.surfaceProvider)
            }

        val recorder = Recorder.Builder()
            .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
            .build()

        videoCapture = VideoCapture.withOutput(recorder)

        imageCapture = ImageCapture.Builder()
            .build()

        // Select back camera as a default
        val cameraSelector = CameraSelector.Builder().requireLensFacing(lensFacing).build()

        try {
            // Unbind use cases before rebinding
            cameraProvider.unbindAll()

            // Bind use cases to camera
            val camera = cameraProvider.bindToLifecycle(
                this, cameraSelector, preview, imageCapture, videoCapture)

            val cameraInfo = camera.cameraInfo
            val cameraControl = camera.cameraControl

            val pinchListener = object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector): Boolean {
                    // Get the camera's current zoom ratio
                    val currentZoomRatio = cameraInfo.zoomState.value?.zoomRatio ?: 0F

                    // Get the pinch gesture's scaling factor
                    val delta = detector.scaleFactor

                    // Update the camera's zoom ratio. This is an asynchronous operation that returns
                    // a ListenableFuture, allowing you to listen to when the operation completes.
                    cameraControl.setZoomRatio(currentZoomRatio * delta)

                    // Return true, as the event was handled
                    return true
                }
            }
            val scaleGestureDetector = ScaleGestureDetector(baseContext, pinchListener)

            viewBinding.previewView.setOnTouchListener { view: View, event: MotionEvent ->
                scaleGestureDetector.onTouchEvent(event)
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> return@setOnTouchListener true
                    MotionEvent.ACTION_UP -> {
                        val factory = viewBinding.previewView.getMeteringPointFactory()
                        val point = factory.createPoint(event.x, event.y)
                        val action = FocusMeteringAction.Builder(point).build()
                        cameraControl.startFocusAndMetering(action)
                        return@setOnTouchListener true
                    }
                    else -> return@setOnTouchListener false
                }
            }


        } catch(exc: Exception) {
            Log.e(TAG, "Use case binding failed", exc)
        }
    }

//    fun saveImage(bmp: Bitmap, path: String, activity: CameraActivity) {
//        ByteArrayOutputStream().apply {
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, this)
//        }
//        val imageFile = File(path)
//        val contentResolver = ContentValues().apply {
//            put(MediaStore.Images.Media.DATE_ADDED, System.currentTimeMillis())
//            put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg")
//            put(MediaStore.Images.Media.DATA, imageFile.absolutePath)
//        }
//
//        activity.contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, contentResolver).apply {
//            bmp.compress(Bitmap.CompressFormat.JPEG, 100, activity.contentResolver.openOutputStream(this!!))
//        }
//    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewBinding = ActivityCameraBinding.inflate(layoutInflater)
        setContentView(viewBinding.root)

        startCamera()

        findViewById<Button>(R.id.editor_link).setOnClickListener {
            val intent = Intent(this, EditorActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent)
        }

        findViewById<ImageButton>(R.id.camera_switch_front_back).setOnClickListener {
            lensFacing = if (CameraSelector.LENS_FACING_FRONT == lensFacing) {
                CameraSelector.LENS_FACING_BACK
            } else {
                CameraSelector.LENS_FACING_FRONT
            }
            bindCameraUseCases()
//            camera.toggleFacing()
        }

        val captureBtn = findViewById<ImageButton>(R.id.camera_capture_btn)
        captureBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                takePhoto()
//                camera.takePicture() // See MyCameraListener for callback
//                  immediateToast("Took Picture")
            } else {
                Log.i(TAG, "called captureVideo()")
                captureVideo()
//                if (camera.isTakingVideo) {
//                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.white))
//                    camera.stopVideo()
//                } else {
//                    captureBtn.setBackgroundColor(ContextCompat.getColor(this, R.color.red))
//                    val path = buildVideoPath()
//                    Log.i(TAG, path)
//                    camera.takeVideo(File(path))
//                }
            }
        }

        val switchModeBtn = findViewById<ImageButton>(R.id.switch_photo_video)
        switchModeBtn.setOnClickListener {
            if (mode == Mode.PICTURE) {
                mode = Mode.VIDEO
                switchModeBtn.setImageResource(R.drawable.camera_mode_icon)
                immediateToast("Switched to Video mode")
            } else {
                mode = Mode.PICTURE
                switchModeBtn.setImageResource(R.drawable.video_mode_icon)
                immediateToast("Switched to Picture mode")
            }
//            camera.mode = mode
        }

//        camera = findViewById<CameraView>(R.id.camera_view)
//        camera.setLifecycleOwner(this)

//        camera.addCameraListener(MyCameraListener(this))
    }

//    fun buildPhotoPath(): String {
//        val filename = System.currentTimeMillis().toString()
//        return "${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$filename.jpg"
//    }
//
//    fun buildVideoPath(): String {
//        val filename = System.currentTimeMillis().toString()
//        return "${getExternalFilesDir(Environment.DIRECTORY_PICTURES)}/$filename.webm"
//    }


//    class MyCameraListener(parent: CameraActivity) : CameraListener() {
//        var cameraActivity: CameraActivity = parent
//        override fun onPictureTaken(result: PictureResult) {
//            result.toBitmap { bmp ->
//                val path = cameraActivity.buildPhotoPath()
//                Log.i(TAG, path)
//                cameraActivity.saveImage(bmp!!, path, cameraActivity)
//            }
//        }
//
//        fun saveVideo(videoFile: File): Uri? {
//            val values = ContentValues(3)
//            values.put(MediaStore.Video.Media.TITLE, "My video title")
//            values.put(MediaStore.Video.Media.MIME_TYPE, "video/webm")
//            values.put(MediaStore.Video.Media.DATA, videoFile.path)
//            return cameraActivity.contentResolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, values)
//        }
//
//        override fun onVideoTaken(result: VideoResult) {
//            super.onVideoTaken(result)
//            saveVideo(result.file)
//        }
//    }

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
//        camera.filter = filter;
    }
}

