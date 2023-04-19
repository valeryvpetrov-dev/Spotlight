package com.takusemba.spotlight

import android.graphics.PointF
import android.view.View
import androidx.annotation.Px
import com.takusemba.spotlight.effet.Effect
import com.takusemba.spotlight.effet.EmptyEffect
import com.takusemba.spotlight.shape.Circle
import com.takusemba.spotlight.shape.Shape

/**
 * Target represents the spot that Spotlight will cast.
 */
class Target(
    private val deferredAnchor: DeferredAnchor,
    val shape: Shape,
    val effect: Effect,
    val overlay: View,
    val verticalOffset: Int,
    val listener: OnTargetListener?
) {

  val anchor: PointF
    get() = deferredAnchor.invoke()

  /**
   * Checks if point on edge or inside of the Shape.
   *
   * @param point point to check against contains.
   * @return true if contains, false - otherwise.
   */
  fun contains(point: PointF): Boolean = shape.contains(anchor, point)

  /**
   * [Builder] to build a [Target].
   * All parameters should be set in this [Builder].
   */
  class Builder {

    private var anchor: DeferredAnchor = { DEFAULT_ANCHOR }
    private var shape: Shape = DEFAULT_SHAPE
    private var effect: Effect = DEFAULT_EFFECT
    private lateinit var overlay: View
    private var listener: OnTargetListener? = null
    private var verticalOffset: Int = 0

    /**
     * Sets a pointer to start a [Target].
     */
    fun setAnchor(view: View): Builder = apply {
      anchor = {
        val location = IntArray(2)
        view.getLocationInWindow(location)
        val x = location[0] + view.width / 2f
        val y = location[1] + view.height / 2f
        PointF(x, y)
      }
    }

    /**
     * Sets an anchor point to start [Target].
     */
    fun setAnchor(x: Float, y: Float): Builder = apply {
      setAnchor(PointF(x, y))
    }

    /**
     * Sets an anchor point to start [Target].
     */
    fun setAnchor(anchor: PointF): Builder = apply {
      this.anchor = { anchor }
    }

    fun setVerticalOffset(@Px verticalOffset: Int) = apply {
      this.verticalOffset = verticalOffset
    }

    /**
     * Sets [shape] of the spot of [Target].
     */
    fun setShape(shape: Shape): Builder = apply {
      this.shape = shape
    }

    /**
     * Sets [effect] of the spot of [Target].
     */
    fun setEffect(effect: Effect): Builder = apply {
      this.effect = effect
    }

    /**
     * Sets [overlay] to be laid out to describe [Target].
     */
    fun setOverlay(overlay: View): Builder = apply {
      this.overlay = overlay
    }

    /**
     * Sets [OnTargetListener] to notify the state of [Target].
     */
    fun setOnTargetListener(listener: OnTargetListener): Builder = apply {
      this.listener = listener
    }

    fun build() = Target(
        deferredAnchor = anchor,
        shape = shape,
        effect = effect,
        overlay = overlay,
        verticalOffset = verticalOffset,
        listener = listener
    )

    companion object {

      private val DEFAULT_ANCHOR = PointF(0f, 0f)

      private val DEFAULT_SHAPE = Circle(100f)

      private val DEFAULT_EFFECT = EmptyEffect()
    }
  }
}

typealias DeferredAnchor = () -> PointF
