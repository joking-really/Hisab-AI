package com.example.ai

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint

object OcrPreprocessor {
    fun preprocessImage(bitmap: Bitmap): Bitmap {
        // 1. Convert to grayscale
        val gray = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(gray)
        val paint = Paint().apply { 
            colorFilter = ColorMatrixColorFilter(ColorMatrix().apply { setSaturation(0f) }) 
        }
        canvas.drawBitmap(bitmap, 0f, 0f, paint)

        // 2. Increase contrast
        val contrastMatrix = ColorMatrix(floatArrayOf(
            1.5f, 0f, 0f, 0f, -50f,
            0f, 1.5f, 0f, 0f, -50f,
            0f, 0f, 1.5f, 0f, -50f,
            0f, 0f, 0f, 1f, 0f
        ))
        val contrast = Bitmap.createBitmap(bitmap.width, bitmap.height, Bitmap.Config.ARGB_8888)
        Canvas(contrast).drawBitmap(gray, 0f, 0f, Paint().apply { 
            colorFilter = ColorMatrixColorFilter(contrastMatrix) 
        })

        // 3. Resize to max 1024px width
        val maxWidth = 1024
        return if (contrast.width > maxWidth) {
            val scale = maxWidth.toFloat() / contrast.width
            Bitmap.createScaledBitmap(contrast, maxWidth, (contrast.height * scale).toInt(), true)
        } else {
            contrast
        }
    }
}
