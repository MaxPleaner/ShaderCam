package com.skamz.shadercam.ui.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredWidth
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.fragment.app.Fragment
import com.godaddy.android.colorpicker.ClassicColorPicker
import com.godaddy.android.colorpicker.HsvColor
import com.skamz.shadercam.R
import com.skamz.shadercam.ui.activities.ParametersActivity

class ParametersActivityColorPickerFragmentActivity : Fragment(R.layout.fragment_color_picker) {
    companion object {
        var startingColor: Color = Color.Red
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = inflater.inflate(R.layout.fragment_color_picker, container, false)
        val composeView = binding.findViewById<ComposeView>(R.id.compose_view)

        composeView.apply {
            setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
            setContent { BuildContent() }
        }
        return binding
    }

    @Composable
    fun BuildContent() {
        var selectedColor: Color by remember { mutableStateOf(startingColor) }

        Box {
            Text(
                "Default Value: ",
                color = Color.White,
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
                        (activity as ParametersActivity).defaultColorValue = selectedColor.toArgb()
                    }
                )
            }

        }
    }
}
