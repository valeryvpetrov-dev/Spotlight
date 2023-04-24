@file:Suppress("DEPRECATION")

package com.takusemba.spotlight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.animation.ValueAnimator.INFINITE
import android.animation.ValueAnimator.ofFloat
import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import android.widget.AbsoluteLayout
import androidx.annotation.ColorInt
import androidx.core.view.updateLayoutParams

/**
 * [SpotlightView] starts/finishes [Spotlight], and starts/finishes a current [Target].
 */
internal class SpotlightView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
    @ColorInt backgroundColor: Int,
) : AbsoluteLayout(context, attrs, defStyleAttr) {
  private val offsetBuffer = IntArray(2)

  private val shapePaint by lazy {
    Paint().apply { xfermode = PorterDuffXfermode(PorterDuff.Mode.CLEAR) }
  }

  private val effectPaint by lazy { Paint() }

  private val invalidator = AnimatorUpdateListener { invalidate() }

  private var shapeAnimator: ValueAnimator? = null
  private var effectAnimator: ValueAnimator? = null
  private var target: Target? = null

  private var onTouchOutsideOfCurrentTargetListener: (() -> Unit)? = null

  init {
    setWillNotDraw(false)
    setLayerType(View.LAYER_TYPE_HARDWARE, null)
    setBackgroundColor(backgroundColor)
  }

  override fun onDraw(canvas: Canvas) {
    super.onDraw(canvas)
    val currentTarget = target
    val currentShapeAnimator = shapeAnimator
    val currentEffectAnimator = effectAnimator
    val localLocation = currentTarget?.getLocalLocation() ?: return
    if (currentEffectAnimator != null && currentShapeAnimator != null && !currentShapeAnimator.isRunning) {
      currentTarget.effect.draw(
          canvas = canvas,
          rectangle = localLocation,
          value = currentEffectAnimator.animatedValue as Float,
          paint = effectPaint
      )
    }
    if (currentShapeAnimator != null) {
      currentTarget.shape.draw(
          canvas = canvas,
          rectangle = localLocation,
          value = currentShapeAnimator.animatedValue as Float,
          paint = shapePaint
      )
    }
  }

  /**
   * Based on guide:
   * https://developer.android.com/guide/topics/ui/accessibility/custom-views#custom-click-events
   */
  override fun onTouchEvent(event: MotionEvent): Boolean {
    super.onTouchEvent(event)
    return when (event.action) {
      MotionEvent.ACTION_UP -> {
        performClick() // Call this method to handle the response, and
        // thereby enable accessibility services to
        // perform this action for a user who cannot
        // click the touchscreen.
        true
      }

      MotionEvent.ACTION_DOWN -> {
        val currentTarget = this.target ?: return false
        onTouchOutsideOfCurrentTargetListener?.also {
          val touchPoint = PointF(event.x, event.y)
          val localLocation = currentTarget.getLocalLocation()
          if (!currentTarget.contains(localLocation, touchPoint)) {
            it.invoke()
          }
        }
        true
      }

      else -> false
    }
  }

  override fun performClick(): Boolean {
    // Calls the super implementation, which generates an AccessibilityEvent
    // and calls the onClick() listener on the view, if any
    super.performClick()
    // Handle the action for the custom click here
    return true
  }

  /**
   * Starts the provided [Target].
   */
  fun startTarget(target: Target) {
    this.target = target

    removeAllViews()
    val localLocation = target.getLocalLocation()
    val childLayoutParams = target.overlay.layoutParams?.let { source ->
      if (source is LayoutParams) {
        source.x = 0
        source.y = localLocation.bottom + target.verticalOffset
        source
      } else {
        LayoutParams(source.width, source.height, 0,
            localLocation.bottom + target.verticalOffset)
      }
    } ?: LayoutParams(MATCH_PARENT, WRAP_CONTENT, 0,
        localLocation.bottom + target.verticalOffset)

    addView(target.overlay, childLayoutParams)

    shapeAnimator = shapeAnimator?.apply {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
    }.run {
      ofFloat(0f, 1f).apply {
        duration = target.shape.duration
        interpolator = target.shape.interpolator
        addUpdateListener(invalidator)
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }

          override fun onAnimationCancel(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }
        })
      }
    }.also(ValueAnimator::start)

    effectAnimator = effectAnimator?.apply {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
    }.run {
      ofFloat(0f, 1f).apply {
        startDelay = target.shape.duration
        duration = target.effect.duration
        interpolator = target.effect.interpolator
        repeatMode = target.effect.repeatMode
        repeatCount = INFINITE
        addUpdateListener(invalidator)
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }

          override fun onAnimationCancel(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }
        })
      }
    }.also(ValueAnimator::start)
  }

  /**
   * Finishes the current [Target].
   */
  fun finishTarget(listener: Animator.AnimatorListener) {
    val currentTarget = target ?: return
    val currentAnimatedValue = shapeAnimator?.animatedValue ?: return

    shapeAnimator = shapeAnimator?.apply {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
    }.run {
      ofFloat(currentAnimatedValue as Float, 0f).apply {
        duration = currentTarget.shape.duration
        interpolator = currentTarget.shape.interpolator
        addUpdateListener(invalidator)
        addListener(listener)
        addListener(object : AnimatorListenerAdapter() {
          override fun onAnimationEnd(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }

          override fun onAnimationCancel(animation: Animator) {
            removeAllListeners()
            removeAllUpdateListeners()
          }
        })
      }
    }.also(ValueAnimator::start)

    effectAnimator = effectAnimator?.run {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
      null
    }
  }

  fun cleanup() {
    effectAnimator = effectAnimator?.run {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
      null
    }

    shapeAnimator = shapeAnimator?.run {
      removeAllListeners()
      removeAllUpdateListeners()
      cancel()
      null
    }

    removeAllViews()
  }

  fun invalidateTargetLocation() {
    val currentTarget = target ?: return
    val localLocation = currentTarget.getLocalLocation()

    currentTarget.overlay.updateLayoutParams<LayoutParams> {
      y = localLocation.bottom + currentTarget.verticalOffset
    }
  }

  fun setOnTouchOutsideOfCurrentTargetListener(listener: () -> Unit) {
    onTouchOutsideOfCurrentTargetListener = listener
  }

  private fun Target.getLocalLocation(): Rect {
    // adjust anchor in case where custom container is set.
    getLocationInWindow(offsetBuffer)

    return windowLocation.apply { offset(-offsetBuffer[0], -offsetBuffer[1]) }
  }
}
