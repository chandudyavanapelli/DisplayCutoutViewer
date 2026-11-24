/*
 * SPDX-FileCopyrightText: 2023 Sebastiano Barezzi
 * SPDX-License-Identifier: Apache-2.0
 */

package dev.sebaubuntu.displaycutoutviewer

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Color
import android.os.Bundle
import android.util.DisplayMetrics
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import dev.sebaubuntu.displaycutoutviewer.ext.next
import dev.sebaubuntu.displaycutoutviewer.ui.PathView
import dev.sebaubuntu.displaycutoutviewer.utils.CutoutSpecification

class MainActivity : AppCompatActivity() {
    // Views
    private val applyPathButton by lazy { findViewById<Button>(R.id.applyPathButton) }
    private val changeColorButton by lazy { findViewById<Button>(R.id.changeColorButton) }
    private val pathEditText by lazy { findViewById<EditText>(R.id.pathEditText) }
    private val pathView by lazy { findViewById<PathView>(R.id.pathView) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        hideSystemBars()

        setContentView(R.layout.activity_main)

        // Set the default cutout value
        pathEditText.setText(getDefaultCutout())

        applyPathButton.setOnClickListener {
            val pathData = pathEditText.text.toString().takeUnless {
                it.isBlank()
            } ?: return@setOnClickListener

            val physicalDisplayWidth = pathView.width
            val physicalDisplayHeight = pathView.height

            // Like the framework, the cutout is specified for the stable (largest) display mode
            // and @dp values use the stable density, then everything is scaled to the current mode
            val density = DisplayMetrics.DENSITY_DEVICE_STABLE / DisplayMetrics.DENSITY_DEFAULT.toFloat()

            val path = CutoutSpecification.Parser(
                density, physicalDisplayWidth, physicalDisplayHeight,
                getPhysicalPixelDisplaySizeRatio()
            ).parse(pathData).path ?: run {
                Toast.makeText(this, R.string.invalid_path, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            pathView.setPath(path)
        }

        changeColorButton.setOnClickListener {
            pathView.color = COLORS.next(pathView.color)
        }
    }

    private fun hideSystemBars() {
        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        // Configure the behavior of the hidden system bars
        windowInsetsController.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        // Hide the status bar
        windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
    }

    private fun getPhysicalPixelDisplaySizeRatio(): Float {
        val display = display ?: return 1f
        val mode = display.mode
        val stableMode = display.supportedModes.maxByOrNull {
            it.physicalWidth * it.physicalHeight
        } ?: return 1f

        val currentWidth = minOf(mode.physicalWidth, mode.physicalHeight)
        val stableWidth = minOf(stableMode.physicalWidth, stableMode.physicalHeight)

        return currentWidth.toFloat() / stableWidth
    }

    @SuppressLint("DiscouragedApi")
    private fun getDefaultCutout(): String {
        val resources = Resources.getSystem()

        for (resourceName in CUTOUT_RESOURCE_NAMES) {
            resources.getIdentifier(
                resourceName, "string", "android"
            ).takeIf { it > 0 }?.let { id ->
                resources.getString(id).takeIf { path ->
                    path.isNotBlank()
                }?.let { path ->
                    return path
                }
            }
        }

        return getString(R.string.example_path)
    }

    companion object {
        private val CUTOUT_RESOURCE_NAMES = listOf(
            "config_mainBuiltInDisplayCutout",
            "config_mainBuiltInDisplayCutoutRectApproximation",
        )

        private val COLORS = listOf(
            null,
            Color.WHITE,
            Color.BLACK,
            Color.RED,
            Color.GREEN,
            Color.BLUE,
        )
    }
}
