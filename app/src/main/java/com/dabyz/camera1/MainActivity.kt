package com.dabyz.camera1

import android.Manifest
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.os.Bundle
import android.util.DisplayMetrics
import android.util.Log
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import kotlinx.android.synthetic.main.activity_main.*
import java.io.ByteArrayOutputStream
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
        camera.parameters.let { params ->
            fun ratio(size: Camera.Size) = size.width.toDouble() / size.height.toDouble()
            params.getSupportedPictureSizes().filter { s -> s.height < 2000 }
                .filter { s -> params.getPreferredPreviewSizeForVideo().let { p -> abs(ratio(p) - ratio(s)) <= 0.01 } }
                .maxBy { it.height }!!
                .let { size -> params.setPictureSize(size.width, size.height) }
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE)
            camera.parameters = params
        }
        camera.setDisplayOrientation(90)
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
    }

    private fun init() = openCamera().let { camera ->
        fun View.changeTop(t: Int) = { (layoutParams as MarginLayoutParams).topMargin = t; requestLayout() }()
        fun getScreenWidth() = DisplayMetrics().let { d -> windowManager.defaultDisplay.getMetrics(d); d.widthPixels }
        front_ground.changeTop(getScreenWidth())
        button.setOnClickListener {
            camera.takePicture(null, null) { photo, _ ->
                Log.e(null, "photo.size:${photo.size}")
                fun scale(photo: ByteArray, width: Int): Bitmap = BitmapFactory.Options().let { op ->
                    with(op) { inScaled = true; inDensity = width; inTargetDensity = 77 }
                    BitmapFactory.decodeByteArray(photo, 0, photo.size, op)
                }
                fun cut(bmp: Bitmap) = minOf(bmp.width, bmp.height).let { Bitmap.createBitmap(bmp, 0, 0, it, it) }
                fun rotate(bmp: Bitmap) = Matrix().let { it.postRotate(90F); Bitmap.createBitmap(bmp, 0, 0, bmp.width, bmp.height, it, true) }
                fun scale(bmp: Bitmap): Bitmap = BitmapFactory.Options().let { op ->
                    with(op) { inScaled = true; inDensity = bmp.width; inTargetDensity = 500 }
                    bmp2Bytes(bmp).let { BitmapFactory.decodeByteArray(it, 0, it.size, op) }
                }
                val bitmap0 = BitmapFactory.decodeByteArray(photo, 0, photo.size)
                bmpLog(bitmap0)
                val bitmap = rotate(scale(cut(BitmapFactory.decodeByteArray(photo, 0, photo.size))))
                bmpLog(bitmap)

                captured_image.setImageBitmap(bitmap)
                camera_preview.visibility = View.GONE; button.visibility = View.GONE; captured_image.visibility = View.VISIBLE
            }
        }
        captured_image.setOnClickListener {
            camera.startPreview()
            camera_preview.visibility = View.VISIBLE; button.visibility = View.VISIBLE; captured_image.visibility = View.GONE
        }
    }

    fun bmp2Bytes(bmp: Bitmap): ByteArray = ByteArrayOutputStream()
        .let { stream -> bmp.compress(Bitmap.CompressFormat.WEBP, 80, stream).let { stream.toByteArray() } }

    fun bmpLog(b: Bitmap) {
        Log.e(null, "d:${b.rowBytes * b.height}-w:${b.width}-h:${b.height}-d:${b.density}")
        Log.e(null, "photo.size:${bmp2Bytes(b).size}")
    }
}
