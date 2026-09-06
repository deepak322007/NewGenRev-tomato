package com.example.tomatodiseasedetector

import android.content.Context
import android.graphics.Bitmap
import android.os.Build
import org.tensorflow.lite.Interpreter
import java.nio.ByteBuffer
import java.nio.ByteOrder

class Classifier(private val context: Context) {

    private lateinit var interpreter: Interpreter

    private val imageSize = 224

    init {
        loadModel()
    }

    private fun loadModel() {

        val inputStream =
            context.assets.open("model_unquant.tflite")

        val modelBytes =
            inputStream.readBytes()

        inputStream.close()

        val modelBuffer =
            ByteBuffer.allocateDirect(modelBytes.size)

        modelBuffer.order(ByteOrder.nativeOrder())

        modelBuffer.put(modelBytes)

        modelBuffer.rewind()

        interpreter = Interpreter(modelBuffer)
    }

    fun classify(bitmap: Bitmap): Pair<String, Float> {
        var resizedBitmap =
            Bitmap.createScaledBitmap(
                bitmap,
                imageSize,
                imageSize,
                true
            )

        // Added: Convert Hardware Bitmap to Software Bitmap to allow pixel access
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            resizedBitmap.config == Bitmap.Config.HARDWARE
        ) {
            resizedBitmap = resizedBitmap.copy(Bitmap.Config.ARGB_8888, false)
        }

        val inputBuffer =
            ByteBuffer.allocateDirect(
                4 * imageSize * imageSize * 3
            )

        inputBuffer.order(ByteOrder.nativeOrder())

        val pixels =
            IntArray(imageSize * imageSize)

        resizedBitmap.getPixels(
            pixels,
            0,
            imageSize,
            0,
            0,
            imageSize,
            imageSize
        )

        for (pixel in pixels) {

            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            inputBuffer.putFloat(r / 255.0f)
            inputBuffer.putFloat(g / 255.0f)
            inputBuffer.putFloat(b / 255.0f)
        }

        inputBuffer.rewind()

        val output =
            Array(1) {
                FloatArray(10)
            }

        interpreter.run(
            inputBuffer,
            output
        )

        val probabilities = output[0]

        var maxIndex = 0

        for (i in probabilities.indices) {

            if (probabilities[i] >
                probabilities[maxIndex]
            ) {
                maxIndex = i
            }
        }

        val labels = loadLabels()

        return Pair(
            labels[maxIndex],
            probabilities[maxIndex]
        )
    }

    private fun loadLabels(): List<String> {

        return context.assets
            .open("labels.txt")
            .bufferedReader()
            .readLines()
            .map {
                it.trim()
            }
            .filter {
                it.isNotEmpty()
            }
    }
}