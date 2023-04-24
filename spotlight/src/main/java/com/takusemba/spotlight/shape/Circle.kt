package com.takusemba.spotlight.shape

import android.animation.TimeInterpolator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.view.animation.DecelerateInterpolator
import java.util.concurrent.TimeUnit

/**
 * [Shape] of Circle with customizable radius.
 */
class Circle @JvmOverloads constructor(
    private val radius: Float,
    override val duration: Long = TimeUnit.MILLISECONDS.toMillis(500),
    override val interpolator: TimeInterpolator = DecelerateInterpolator(2f)
) : Shape {

  override fun draw(canvas: Canvas, rectangle: Rect, value: Float, paint: Paint) {
    canvas.drawCircle(rectangle.exactCenterX(), rectangle.exactCenterY(), value * radius, paint)
  }

  override fun contains(rectangle: Rect, point: PointF): Boolean {
    val x = rectangle.exactCenterX()
    val y = rectangle.exactCenterY()
    val xNorm = point.x - x
    val yNorm = point.y - y
    return (xNorm * xNorm + yNorm * yNorm) <= radius * radius
  }
}
