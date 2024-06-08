package com.example.lankytinu_objektu_atpazinimas


import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.tensorflow.lite.support.image.TensorImage
import org.tensorflow.lite.task.vision.detector.ObjectDetector
import java.util.*

object ObjectDetectionHelper {
    private const val MAX_FONT_SIZE = 96F

    interface ObjectDetectionCallback {
        fun onDetectionComplete(detectedData: Map<String, Map<String, String>>)
    }

    fun runObjectDetection(
        bitmap: Bitmap,
        activity: AppCompatActivity,
        imageView: ImageView,
        callback: ObjectDetectionCallback
    ) {
        activity.lifecycleScope.launch(Dispatchers.Default) {
            val detector = ObjectDetector.createFromFileAndOptions(
                activity,
                "lankyt_objekt.tflite",
                ObjectDetector.ObjectDetectorOptions.builder()
                    .setMaxResults(1)
                    .setScoreThreshold(0.3f)
                    .build()
            )
            detector.use { detection ->
                val image = TensorImage.fromBitmap(bitmap)
                val results = detection.detect(image)
                val resultToDisplay = results.map {
                    DetectionResult(
                        it.boundingBox,
                        "${it.categories.first().label}, ${
                            it.categories.first().score.times(100).toInt()
                        }%"
                    )
                }
                val imgWithResult = drawDetectionResult(bitmap, resultToDisplay)
                val tagsToData = loadTagsToData(activity)
                val filteredTagsToData = tagsToData.filterKeys { tag ->
                    resultToDisplay.any { result ->
                        result.text.lowercase(Locale.getDefault()).contains(tag)
                    }
                }
                withContext(Dispatchers.Main) {
                    imageView.setImageBitmap(imgWithResult)
                    callback.onDetectionComplete(filteredTagsToData)
                }
            }
        }
    }


    private fun drawDetectionResult(
        bitmap: Bitmap,
        detectionResults: List<DetectionResult>
    ): Bitmap {
        val outputBitmap = bitmap.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(outputBitmap)
        val pen = Paint().apply {
            color = Color.RED
            strokeWidth = 8F
            style = Paint.Style.STROKE
            textAlign = Paint.Align.LEFT
        }

        detectionResults.forEach { result ->
            canvas.drawRect(result.boundingBox, pen)
            pen.color = Color.YELLOW
            pen.style = Paint.Style.FILL_AND_STROKE
            pen.strokeWidth = 2F
            pen.textSize = MAX_FONT_SIZE
            canvas.drawText(
                result.text,
                result.boundingBox.left, result.boundingBox.top + pen.textSize, pen
            )

        }
        return outputBitmap
    }

    private fun loadTagsToData(activity: AppCompatActivity): Map<String, Map<String, String>> {
        val tagsToData = mutableMapOf<String, Map<String, String>>()
        val resources = activity.resources
        val packageName = activity.packageName

        var i = 1
        while (true) {
            val tagId = resources.getIdentifier("tag$i", "string", packageName)
            val urlId = resources.getIdentifier("url$i", "string", packageName)
            val infoId = resources.getIdentifier("info$i", "string", packageName)
            val coordinatesId = resources.getIdentifier("coordinates$i", "string", packageName)

            if (tagId == 0 || urlId == 0 || infoId == 0 || coordinatesId == 0) break

            val tag = resources.getString(tagId).lowercase(Locale.getDefault())
            val url = resources.getString(urlId)
            val info = resources.getString(infoId)
            val coordinates = resources.getString(coordinatesId)
            tagsToData[tag] = mapOf("url" to url, "info" to info, "coordinates" to coordinates)

            i++
        }
        return tagsToData
    }

}
