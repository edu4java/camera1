package com.dabyz.camera1

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.util.DisplayMetrics
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import kotlin.math.abs

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState); setContentView(R.layout.activity_main)
    }

    override fun onResume() {
        super.onResume()
        fun hasPermission() = listOf(Manifest.permission.CAMERA)
            .filterNot { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
            .let { needs ->
                if (needs.isNotEmpty()) ActivityCompat.requestPermissions(this, needs.toTypedArray(), 0)
                needs.isEmpty()
            }
        if (hasPermission()) init()
    }

    private fun openCamera() = Camera.open().also { camera ->
        var params = camera.parameters
        fun ratio(size: Camera.Size) = size.width.toDouble() / size.height.toDouble()
        var size = params.getSupportedPictureSizes().filter { s -> s.height > 300 }
            .filter { s -> params.getPreferredPreviewSizeForVideo().let { p -> abs(ratio(p) - ratio(s)) <= 0.01 } }
            .minBy { it.height }!!
        params.setPictureSize(size.width, size.height)
        camera.setDisplayOrientation(90)
        camera.parameters = params

        camera_preview.addView(SurfaceView(this).also {
            it.holder.addCallback(object : SurfaceHolder.Callback {
                override fun surfaceCreated(holder: SurfaceHolder) {
                    camera.setPreviewDisplay(holder); camera.startPreview()
                }

                override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {}
                override fun surfaceDestroyed(holder: SurfaceHolder) {
                    camera.stopPreview(); camera.release()
                }
            })
        })
        //TODO "ADD AUTO FOCUS"
    }

    private fun init() = openCamera().let { camera ->
        fun View.changeTop(t: Int) = { (layoutParams as MarginLayoutParams).topMargin = t; requestLayout() }()
        fun getScreenWidth() = DisplayMetrics().let { d -> windowManager.defaultDisplay.getMetrics(d); d.widthPixels }
        front_ground.changeTop(getScreenWidth())
        button.setOnClickListener {
            camera.takePicture(null, null) { data, _ ->
                val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)
                fun rotate(bmp: Bitmap) = Matrix().let { it.postRotate(90F); Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, it, true) }
                fun cut(bmp: Bitmap) = minOf(bmp.width, bmp.height).let { Bitmap.createBitmap(bmp, 0, 0, it, it) }
                captured_image.setImageBitmap(rotate(cut(bitmap))) //TODO "Scale down to 300 px
                camera_preview.visibility = View.GONE; button.visibility = View.GONE; captured_image.visibility = View.VISIBLE
            }
        }
        captured_image.setOnClickListener {
            camera.startPreview()
            camera_preview.visibility = View.VISIBLE; button.visibility = View.VISIBLE; captured_image.visibility = View.GONE
        }
    }
}
