package com.skamz.shadercam

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.otaliastudios.cameraview.VideoResult


class VideoPreviewActivity : AppCompatActivity() {
    companion object {
        var videoResult: VideoResult? = null
        var uri: Uri? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i("DEBUG", "on video preview activity create")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        val cameraLink = findViewById<Button>(R.id.camera_link);
        EditorActivity.cameraActivityIntent = Intent(this, CameraActivity::class.java)
        EditorActivity.cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        cameraLink.setOnClickListener {
            startActivity(EditorActivity.cameraActivityIntent)
        }

        val saveLink = findViewById<Button>(R.id.save_link)
        saveLink.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND);
            sharingIntent.type = "video/*";
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey this is the video subject")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, "Hey this is the video text")
//            sharingIntent.putExtra(Intent.EXTRA_STREAM,Uri.fromFile(videoResult!!.file))

            val photoURI = FileProvider.getUriForFile(
                baseContext,
                baseContext.applicationContext.packageName + ".provider",
                videoResult!!.file
            )
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            sharingIntent.putExtra(Intent.EXTRA_STREAM,photoURI)

            startActivity(Intent.createChooser(sharingIntent,"Share Video"))
        }

        startVideo()
    }

    fun startVideo() {
        val videoView = findViewById<VideoView>(R.id.video)
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView.setMediaController(controller)

//        Log.i("DEBUG", Uri.fromFile(videoResult!!.file).toString())
        videoView.setVideoURI(Uri.fromFile(videoResult!!.file))
//        videoView.setVideoURI(uri)

        videoView.setOnPreparedListener { mp ->
            val lp = videoView.layoutParams
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val viewWidth = videoView.width.toFloat()
            lp.height = (viewWidth * (videoHeight / videoWidth)).toInt()
            videoView.layoutParams = lp
            videoView.start()
//            if (result.isSnapshot) {
//                // Log the real size for debugging reason.
//                Log.e("VideoPreview", "The video full size is " + videoWidth + "x" + videoHeight)
//            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        Log.i("DEBUG", "on video preview activity new intent")

    }
}