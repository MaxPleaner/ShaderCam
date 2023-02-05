package com.skamz.shadercam.ui.activities

import android.content.Intent
import android.graphics.Color
import androidx.compose.ui.graphics.Color as ComposeColor
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.skamz.shadercam.R

class CameraColorPickerActivity: AppCompatActivity() {
    companion object {
        lateinit var paramName: String
        var startingColor: Int = Color.RED
        var finalColor: Int = startingColor
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.camera_activity_color_picker)
        addColorPicker()

        findViewById<Button>(R.id.camera_link).setOnClickListener {
            val i = Intent(this, CameraActivity::class.java)
            i.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
            i.putExtra("UPDATED_COLOR_NAME", paramName)
            i.putExtra("UPDATED_COLOR_VALUE", finalColor)
            startActivity(i)
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        addColorPicker()
    }

    private fun addColorPicker() {
        val composeView = findViewById<ComposeView>(R.id.compose_view)

        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { BuildContent() }
        }
    }

    @Composable
    fun BuildContent() {
        var selectedColor: androidx.compose.ui.graphics.Color by remember { mutableStateOf(
            ComposeColor(startingColor)
        ) }
        finalColor = selectedColor.toArgb()

        Box {
            Text(
                paramName,
                color = androidx.compose.ui.graphics.Color.Red,
                fontSize = 18.sp,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .padding(top = 10.dp)
            )

            Box(
                Modifier
                    .requiredWidth(180.dp)
                    .height(50.dp)
                    .padding(start = 130.dp, end = 0.dp)
                    .background(selectedColor)
                    .align(Alignment.TopStart)
            )

            Box {
                ClassicColorPicker(
                    color = HsvColor.from(selectedColor),
                    showAlphaBar = false,
                    modifier = Modifier
                        .height(300.dp)
                        .requiredWidth(300.dp)
                        .padding(top = 70.dp)
                        .align(Alignment.TopStart),
                    onColorChanged = { color: HsvColor ->
                        selectedColor = color.toColor()
                        finalColor = selectedColor.toArgb()
                    }
                )
            }

        }
    }
}