package com.skamz.shadercam.activities

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.widget.Button
import android.widget.MediaController
import android.widget.VideoView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import com.otaliastudios.cameraview.VideoResult
import com.skamz.shadercam.R


class VideoPreviewActivity : AppCompatActivity() {
    companion object {
        var videoResult: VideoResult? = null
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_preview)

        val cameraLink = findViewById<Button>(R.id.camera_link)
        val cameraActivityIntent = Intent(this, CameraActivity::class.java)
        cameraActivityIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        cameraLink.setOnClickListener {
            startActivity(cameraActivityIntent)
        }

        val saveLink = findViewById<Button>(R.id.save_link)
        saveLink.setOnClickListener {
            val sharingIntent = Intent(Intent.ACTION_SEND)
            sharingIntent.type = "video/*"
            sharingIntent.putExtra(Intent.EXTRA_SUBJECT, "Hey this is the video subject")
            sharingIntent.putExtra(Intent.EXTRA_TEXT, "Hey this is the video text")

            val photoURI = FileProvider.getUriForFile(
                baseContext,
                baseContext.applicationContext.packageName + ".provider",
                videoResult!!.file
            )
            sharingIntent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            sharingIntent.putExtra(Intent.EXTRA_STREAM,photoURI)

            startActivity(Intent.createChooser(sharingIntent,"Share Video"))
        }

        startVideo()
    }

    private fun startVideo() {
        val videoView = findViewById<VideoView>(R.id.video)
        val controller = MediaController(this)
        controller.setAnchorView(videoView)
        controller.setMediaPlayer(videoView)
        videoView.setMediaController(controller)

        videoView.setVideoURI(Uri.fromFile(videoResult!!.file))

        videoView.setOnPreparedListener { mp ->
            val lp = videoView.layoutParams
            val videoWidth = mp.videoWidth.toFloat()
            val videoHeight = mp.videoHeight.toFloat()
            val viewWidth = videoView.width.toFloat()
            lp.height = (viewWidth * (videoHeight / videoWidth)).toInt()
            videoView.layoutParams = lp
            videoView.start()
        }
    }
}