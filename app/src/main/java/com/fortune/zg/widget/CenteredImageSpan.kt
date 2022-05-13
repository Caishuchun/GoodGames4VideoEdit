package com.fortune.zg.widget

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.text.style.ImageSpan

/**
 *  设置文本带有图片
 */
class CenteredImageSpan(context: Context, bitmap: Bitmap) : ImageSpan(context, bitmap) {

    override fun draw(
        canvas: Canvas,
        text: CharSequence?,
        start: Int,
        end: Int,
        x: Float,
        top: Int,
        y: Int,
        bottom: Int,
        paint: Paint
    ) {
        val bitmap = drawable
        val fontMetricsInt = paint.fontMetricsInt

        val transY =
            (y + fontMetricsInt.descent + y + fontMetricsInt.ascent) / 2 - bitmap.bounds.bottom / 2
        canvas.save()
        canvas.translate(x, transY.toFloat())
        bitmap.draw(canvas)
        canvas.restore()
    }
}