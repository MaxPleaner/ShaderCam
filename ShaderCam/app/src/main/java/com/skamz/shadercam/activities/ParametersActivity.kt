package com.skamz.shadercam.activities

import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.textfield.TextInputEditText
import com.skamz.shadercam.R
import top.defaults.colorpicker.ColorPickerPopup
import top.defaults.colorpicker.ColorPickerPopup.ColorPickerObserver


class ParametersActivity : AppCompatActivity() {

    private val TAG = "ParametersActivity"

    private lateinit var floatLayout: LinearLayout

    private lateinit var colorLayout: LinearLayout

    private lateinit var colorRB: RadioButton

    private lateinit var floatRB: RadioButton

    private lateinit var setDefaultValue: TextInputEditText

    private lateinit var cancelBtn: TextView

    private lateinit var save: TextView

    // text view variable to set the color for GFG text
    private lateinit var gfgTextView: TextView

    // two buttons to open color picker dialog and one to
    // set the color for GFG text
    private lateinit var mSetColorButton: TextView   // two buttons to open color picker dialog and one to

    // set the color for GFG text
    private lateinit var mPickColorButton: TextView

    // view box to preview the selected color
    private lateinit var mColorPreview: View

    // this is the default color of the preview box
    private var mDefaultColor = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_parameters)

        init()
    }

    private fun init() {
        try {
            colorLayout = findViewById(R.id.colorLayout)

            floatLayout = findViewById(R.id.floatLayout)

            colorRB = findViewById(R.id.colorRB)

            floatRB = findViewById(R.id.floatRb)

            setDefaultValue = findViewById(R.id.setDefaultValue)

            save = findViewById(R.id.saveParameters)

            // register the GFG text with appropriate ID
            gfgTextView = findViewById(R.id.gfg_heading)

            cancelBtn = findViewById(R.id.cancelParameters)

            // register two of the buttons with their
            // appropriate IDs
            mPickColorButton = findViewById(R.id.pick_color_button)
            mSetColorButton = findViewById(R.id.set_color_button)

            // and also register the view which shows the
            // preview of the color chosen by the user
            mColorPreview = findViewById(R.id.preview_selected_color)

            // set the default color to 0 as it is black
            mDefaultColor = 0

            setOnClickListeners()

        } catch (e: Exception) {
            Log.e(TAG, "setOnClickListeners: $e")
        }
    }

    private fun setOnClickListeners() {
        try {
            //initiate pick color dialog
            pickColorDialog()
            //set picked color
            setPickedColor()
            // RB
            colorRB.setOnClickListener { onRadioButtonClicked(colorRB) }

            floatRB.setOnClickListener { onRadioButtonClicked(floatRB) }

            //buttons
            save.setOnClickListener {
                super.onBackPressed()
            }
            cancelBtn.setOnClickListener {
                super.onBackPressed()
            }

        } catch (e: Exception) {
            Log.e(TAG, "setOnClickListeners: $e")
        }
    }

    private fun setPickedColor() {
        try {
            // handling the Set Color button to set the selected
            // color for the GFG text.

            // handling the Set Color button to set the selected
            // color for the GFG text.
            mSetColorButton.setOnClickListener {
                // now change the value of the GFG text
                // as well.
                gfgTextView.setTextColor(mDefaultColor)

                setDefaultValue.setText(mDefaultColor)
            }
        } catch (e: Exception) {
            Log.e(TAG, "setPickedColor: $e")
        }
    }

    private fun pickColorDialog() {
        try {
            // handling the Pick Color Button to open color
            // picker dialog

            // handling the Pick Color Button to open color
            // picker dialog
            mPickColorButton.setOnClickListener { v ->
                ColorPickerPopup.Builder(this).initialColor(
                    Color.RED
                ) // set initial color
                    // of the color
                    // picker dialog
                    .enableBrightness(
                        true
                    ) // enable color brightness
                    // slider or not
                    .enableAlpha(
                        true
                    ) // enable color alpha
                    // changer on slider or
                    // not
                    .okTitle(
                        "Choose"
                    ) // this is top right
                    // Choose button
                    .cancelTitle(
                        "Cancel"
                    ) // this is top left
                    // Cancel button which
                    // closes the
                    .showIndicator(
                        true
                    ) // this is the small box
                    // which shows the chosen
                    // color by user at the
                    // bottom of the cancel
                    // button
                    .showValue(
                        true
                    ) // this is the value which
                    // shows the selected
                    // color hex code
                    // the above all values can be made
                    // false to disable them on the
                    // color picker dialog.
                    .build()
                    .show(
                        v,
                        object : ColorPickerObserver() {
                            override fun onColorPicked(color: Int) {
                                // set the color
                                // which is returned
                                // by the color
                                // picker
                                mDefaultColor = color

                                // now as soon as
                                // the dialog closes
                                // set the preview
                                // box to returned
                                // color
                                mColorPreview.setBackgroundColor(mDefaultColor)
                            }
                        })
            }
        } catch (e: Exception) {
            Log.e(TAG, "pickColorDialog: $e")
        }
    }


    private fun onRadioButtonClicked(view: View) {
        try {

        } catch (e: Exception) {
            Log.e(TAG, "onRadioButtonClicked: $e")
        }
        if (view is RadioButton) {
            // Is the button now checked?
            val checked = view.isChecked

            // Check which radio button was clicked
            when (view.getId()) {
                R.id.colorRB ->
                    if (checked) {
                        floatLayout.visibility = View.GONE
                        colorLayout.visibility = View.VISIBLE
                    }
                R.id.floatRb ->
                    if (checked) {
                        floatLayout.visibility = View.VISIBLE
                        colorLayout.visibility = View.GONE
                    }
            }
        }
    }
}