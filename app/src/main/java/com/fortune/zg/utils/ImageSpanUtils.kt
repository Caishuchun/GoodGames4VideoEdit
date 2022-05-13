package com.fortune.zg.utils

import android.graphics.Bitmap
import android.graphics.Matrix

/**
 * 文本显示上的图片设置
 */
object ImageSpanUtils {
    fun imageScale(bitmap: Bitmap, width: Float, height: Float): Bitmap {
        val bitmapWidth = bitmap.width
        val bitmapHeight = bitmap.height

        val scaleW = width / bitmapWidth
        val scaleH = height / bitmapHeight

        val matrix = Matrix()
        matrix.postScale(scaleW, scaleH)
        return Bitmap.createBitmap(bitmap, 0, 0, bitmapWidth, bitmapHeight, matrix, true)
    }
}