package com.takusemba.spotlight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.TimeInterpolator
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.animation.DecelerateInterpolator
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.core.content.ContextCompat
import java.util.concurrent.TimeUnit

/**
 * Holds all of the [Target]s and [SpotlightView] to show/hide [Target], [SpotlightView] properly.
 * [SpotlightView] can be controlled with [start]/[finish].
 * All of the [Target]s can be controlled with [next]/[previous]/[show].
 *
 * Once you finish the current [Spotlight] with [finish], you can not start the [Spotlight] again
 * unless you create a new [Spotlight] to start again.
 */
class Spotlight private constructor(
    private val spotlightView: SpotlightView,
    private val targets: Array<Target>,
    private val duration: Long,
    private val interpolator: TimeInterpolator,
    private val container: ViewGroup,
    private val spotlightListener: OnSpotlightListener?,
    finishOnTouchOutsideOfCurrentTarget: Boolean,
    finishOnBackPress: Boolean
) {
  var currentIndex = NO_POSITION
    private set

  init {
    spotlightView.apply {
      if (finishOnTouchOutsideOfCurrentTarget) {
        setOnTouchOutsideOfCurrentTargetListener(
            object : OnTouchOutsideOfCurrentTargetListener {
              override fun onEvent() = finishSpotlight()
            }
        )
      }

      if (finishOnBackPress) {
        isFocusable = true
        isFocusableInTouchMode = true
        setOnKeyListener { _, keyCode, event ->
          if (event.action == KeyEvent.ACTION_UP && keyCode == KeyEvent.KEYCODE_BACK) {
            finishSpotlight()
            true
          } else false
        }
      }
    }

    container.addView(spotlightView, MATCH_PARENT, MATCH_PARENT)
  }

  /**
   * Starts [SpotlightView] and show the first [Target].
   */
  fun start() {
    startSpotlight()
  }

  /**
   * Closes the current [Target] if exists, and shows a [Target] at the specified [index].
   * If target is not found at the [index], it will throw an exception.
   */
  fun show(index: Int) {
    showTarget(index)
  }

  /**
   * Closes the current [Target] if exists, and shows the next [Target].
   * If the next [Target] is not found, Spotlight will finish.
   */
  fun next() {
    showTarget(currentIndex + 1)
  }

  /**
   * Closes the current [Target] if exists, and shows the previous [Target].
   * If the previous target is not found, it will throw an exception.
   */
  fun previous() {
    showTarget(currentIndex - 1)
  }

  /**
   * Closes Spotlight and [SpotlightView] will remove all children and be removed from the [container].
   */
  fun finish() {
    finishSpotlight()
  }

  /**
   * Starts Spotlight.
   */
  private fun startSpotlight() {
    spotlightView.requestFocus()
    spotlightView.startSpotlight(duration, interpolator, object : AnimatorListenerAdapter() {
      override fun onAnimationStart(animation: Animator) {
        spotlightListener?.onStarted()
      }

      override fun onAnimationEnd(animation: Animator) {
        showTarget(0)
      }
    })
  }

  /**
   * Closes the current [Target] if exists, and show the [Target] at [index].
   */
  private fun showTarget(index: Int) {
    if (currentIndex == NO_POSITION) {
      val target = targets[index]
      currentIndex = index
      spotlightView.startTarget(target)
      target.listener?.onStarted(currentIndex)
    } else {
      spotlightView.finishTarget(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          val previousIndex = currentIndex
          val previousTarget = targets[previousIndex]
          previousTarget.listener?.onEnded(previousIndex)
          if (index < targets.size) {
            val target = targets[index]
            currentIndex = index
            spotlightView.startTarget(target)
            target.listener?.onStarted(index)
          } else {
            finishSpotlight()
          }
        }
      })
    }
  }

  /**
   * Closes Spotlight.
   */
  private fun finishSpotlight() {
    spotlightView.finishSpotlight(duration, interpolator, object : AnimatorListenerAdapter() {
      override fun onAnimationEnd(animation: Animator) {
        spotlightView.cleanup()
        container.removeView(spotlightView)
        spotlightListener?.onEnded()
        currentIndex = NO_POSITION
      }
    })
  }

  companion object {

    const val NO_POSITION = -1
  }

  /**
   * Builder to build [Spotlight].
   * All parameters should be set in this [Builder].
   */
  class Builder(private val context: Context) {

    private var targets: Array<Target>? = null
    private var duration: Long = DEFAULT_DURATION
    private var interpolator: TimeInterpolator = DEFAULT_ANIMATION

    @ColorInt
    private var backgroundColor: Int = DEFAULT_OVERLAY_COLOR
    private var container: ViewGroup? = null
    private var listener: OnSpotlightListener? = null

    // Finish on touch outside of current target feature is disabled by default
    private var finishOnTouchOutsideOfCurrentTarget: Boolean = false

    private var finishOnBackPress: Boolean = false

    /**
     * Sets [Target]s to show on [Spotlight].
     */
    fun setTargets(vararg targets: Target): Builder = apply {
      require(targets.isNotEmpty()) { "targets should not be empty. " }
      this.targets = arrayOf(*targets)
    }

    /**
     * Sets [Target]s to show on [Spotlight].
     */
    fun setTargets(targets: List<Target>): Builder = apply {
      require(targets.isNotEmpty()) { "targets should not be empty. " }
      this.targets = targets.toTypedArray()
    }

    /**
     * Sets [duration] to start/finish [Spotlight].
     */
    fun setDuration(duration: Long): Builder = apply {
      this.duration = duration
    }

    /**
     * Sets [backgroundColor] resource on [Spotlight].
     */
    fun setBackgroundColorRes(@ColorRes backgroundColorRes: Int): Builder = apply {
      this.backgroundColor = ContextCompat.getColor(context, backgroundColorRes)
    }

    /**
     * Sets [backgroundColor] on [Spotlight].
     */
    fun setBackgroundColor(@ColorInt backgroundColor: Int): Builder = apply {
      this.backgroundColor = backgroundColor
    }

    /**
     * Sets [interpolator] to start/finish [Spotlight].
     */
    fun setAnimation(interpolator: TimeInterpolator): Builder = apply {
      this.interpolator = interpolator
    }

    /**
     * Sets [container] to hold [SpotlightView]. DecoderView will be used if not specified.
     */
    fun setContainer(container: ViewGroup) = apply {
      this.container = container
    }

    /**
     * Sets [OnSpotlightListener] to notify the state of [Spotlight].
     */
    fun setOnSpotlightListener(listener: OnSpotlightListener): Builder = apply {
      this.listener = listener
    }

    /**
     * Sets [finishOnTouchOutsideOfCurrentTarget] flag
     * to enable/disable (true/false) finishing on touch outside feature.
     */
    fun setFinishOnTouchOutsideOfCurrentTarget(
        finishOnTouchOutsideOfCurrentTarget: Boolean
    ): Builder = apply {
      this.finishOnTouchOutsideOfCurrentTarget = finishOnTouchOutsideOfCurrentTarget
    }

    fun setFinishOnBackPress(finishOnBackPress: Boolean): Builder = apply {
      this.finishOnBackPress = finishOnBackPress
    }

    fun build(): Spotlight {
      val spotlightView = SpotlightView(context, null, 0, backgroundColor)
      val targets = requireNotNull(targets) { "targets should not be null. " }
      val container = container ?: tryGetActivity(context)!!.window.decorView as ViewGroup
      return Spotlight(
          spotlightView = spotlightView,
          targets = targets,
          duration = duration,
          interpolator = interpolator,
          container = container,
          spotlightListener = listener,
          finishOnTouchOutsideOfCurrentTarget,
          finishOnBackPress
      )
    }

    companion object {

      private val DEFAULT_DURATION = TimeUnit.SECONDS.toMillis(1)

      private val DEFAULT_ANIMATION = DecelerateInterpolator(2f)

      @ColorInt
      private val DEFAULT_OVERLAY_COLOR: Int = 0x6000000

      private fun tryGetActivity(context: Context): Activity? {
        var ctx = context
        while (ctx is ContextWrapper) {
          if (ctx is Activity) {
            return ctx
          }

          ctx = ctx.baseContext
        }
        return null
      }
    }
  }
}
