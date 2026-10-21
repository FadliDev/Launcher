package com.sally.editor

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.view.View
import kotlin.math.max

/** Gutter that follows the editor's vertical scroll position. */
class LineNumberView(context: Context) : View(context) {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF85818D.toInt()
        textSize = 12f * resources.displayMetrics.scaledDensity
        typeface = Typeface.MONOSPACE
    }
    var lineCount: Int = 1
        set(value) { field = max(1, value); requestLayout(); invalidate() }
    var lineHeightPx: Float = 0f
        set(value) { field = value; invalidate() }
    var verticalScrollPx: Int = 0
        set(value) { field = value; invalidate() }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val digits = lineCount.toString().length
        val width = (paint.measureText("0") * (digits + 2)).toInt() + paddingLeft + paddingRight
        setMeasuredDimension(resolveSize(width, widthMeasureSpec), resolveSize(0, heightMeasureSpec))
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val step = if (lineHeightPx > 0f) lineHeightPx else paint.textSize * 1.5f
        val firstLine = max(0, verticalScrollPx / step.toInt().coerceAtLeast(1))
        val baselineOffset = step * (firstLine + 1) - verticalScrollPx + paddingTop
        var line = firstLine + 1
        var baseline = baselineOffset
        while (baseline < height && line <= lineCount) {
            canvas.drawText(line.toString(), width - paddingRight - paint.measureText(line.toString()) - dp(8), baseline, paint)
            line++
            baseline += step
        }
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt().toFloat()
}
