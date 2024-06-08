package com.example.lankytinu_objektu_atpazinimas

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson

@Suppress("DEPRECATION")
class MainActivity : AppCompatActivity(), View.OnClickListener {

    companion object {
        const val TAG = "TFLite - ODT"
        const val REQUEST_IMAGE_CAPTURE = 1
        const val REQUEST_SELECT_IMAGE = 2
    }

    private lateinit var captureImage: Button
    private lateinit var selectImage: Button
    private lateinit var inputImageView: ImageView
    private lateinit var tvPlaceholder: TextView
    private lateinit var cameraUtility: CameraUtility
    private lateinit var tvMainTitle: TextView
    private lateinit var tvSubtitle: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        captureImage = findViewById(R.id.captureImage)
        selectImage = findViewById(R.id.selectImage)
        inputImageView = findViewById(R.id.imageView)
        tvPlaceholder = findViewById(R.id.tvPlaceholder)
        cameraUtility = CameraUtility(this)
        tvMainTitle = findViewById(R.id.tvMainTitle)
        tvSubtitle = findViewById(R.id.tvSubtitle)
        captureImage.setOnClickListener(this)
        selectImage.setOnClickListener(this)
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_IMAGE_CAPTURE -> {
                    setViewAndDetect(cameraUtility.getCapturedImage())
                }
                REQUEST_SELECT_IMAGE -> {
                    data?.data?.let { uri ->
                        val bitmap = MediaStore.Images.Media.getBitmap(this.contentResolver, uri)
                        setViewAndDetect(bitmap)
                    }
                }
            }
        }
    }

    override fun onClick(v: View?) {
        when (v?.id) {
            R.id.captureImage -> {
                try {
                    cameraUtility.dispatchTakePictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "Klaida, kamera nepasiekiama.", e)
                }
            }
            R.id.selectImage -> {
                try {
                    dispatchSelectPictureIntent()
                } catch (e: ActivityNotFoundException) {
                    Log.e(TAG, "Klaida, galerija nepasiekiama.", e)
                }
            }
        }
    }

    private fun dispatchSelectPictureIntent() {
        val selectPictureIntent =
            Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        try {
            startActivityForResult(selectPictureIntent, REQUEST_SELECT_IMAGE)
        } catch (e: ActivityNotFoundException) {
            Log.e(TAG, "Klaida, galerija nepasiekiama.", e)
        }
    }

    private fun setViewAndDetect(bitmap: Bitmap) {
        inputImageView.setImageBitmap(bitmap)
        tvPlaceholder.visibility = View.GONE
        tvMainTitle.visibility = View.GONE
        tvSubtitle.visibility = View.GONE
        ObjectDetectionHelper.runObjectDetection(bitmap, this, inputImageView, object :
            ObjectDetectionHelper.ObjectDetectionCallback {
            override fun onDetectionComplete(detectedData: Map<String, Map<String, String>>) {
                detectedDataIntent(detectedData)
            }
        })
    }

    private fun detectedDataIntent(detectedData: Map<String, Map<String, String>>) {
        val gson = Gson()
        val detectedDataJson = gson.toJson(detectedData)
        val intent = Intent(this, ObjectDetectedActivity::class.java).apply {
            putExtra("detectedData", detectedDataJson)
        }

        startActivity(intent)
    }

}
