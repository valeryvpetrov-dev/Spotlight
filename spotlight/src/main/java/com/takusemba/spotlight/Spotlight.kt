package com.takusemba.spotlight

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.KeyEvent
import android.view.ViewGroup
import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import androidx.annotation.ColorInt
import androidx.annotation.ColorRes
import androidx.annotation.TransitionRes
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.core.view.contains
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionInflater
import androidx.transition.TransitionManager
import androidx.transition.doOnEnd
import androidx.transition.doOnStart

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
    private val container: ViewGroup,
    private val spotlightListener: OnSpotlightListener?,
    finishOnTouchOutsideOfCurrentTarget: Boolean,
    finishOnBackPress: Boolean,
    enterTransition: Any,
    exitTransition: Any
) {
  private val enterTransition: Transition by lazy(LazyThreadSafetyMode.NONE) {
    if (enterTransition is Int && enterTransition != ResourcesCompat.ID_NULL) {
      TransitionInflater.from(spotlightView.context).inflateTransition(enterTransition)
    } else if (enterTransition is Transition) {
      enterTransition
    } else {
      AutoTransition()
    }
  }

  private val exitTransition: Transition by lazy(LazyThreadSafetyMode.NONE) {
    if (exitTransition is Int && exitTransition != ResourcesCompat.ID_NULL) {
      TransitionInflater.from(spotlightView.context).inflateTransition(exitTransition)
    } else if (exitTransition is Transition) {
      exitTransition
    } else {
      AutoTransition()
    }
  }

  var currentIndex = NO_POSITION
    private set

  init {
    spotlightView.apply {
      if (finishOnTouchOutsideOfCurrentTarget) {
        setOnTouchOutsideOfCurrentTargetListener {
          finishSpotlight()
        }
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

  fun invalidateTargetLocation() {
    spotlightView.invalidateTargetLocation()
  }

  /**
   * Starts Spotlight.
   */
  private fun startSpotlight() {
    enterTransition.doOnStart {
      spotlightListener?.onStarted()
    }
    enterTransition.doOnEnd {
      showTarget(0)
    }
    if (spotlightView !in container) {
      TransitionManager.beginDelayedTransition(container, enterTransition)

      container.addView(spotlightView, MATCH_PARENT, MATCH_PARENT)
    }
  }

  /**
   * Closes the current [Target] if exists, and show the [Target] at [index].
   */
  private fun showTarget(index: Int) {
    if (currentIndex == NO_POSITION) {
      val target = targets[index]
      currentIndex = index
      target.listener?.onStarting(target, index)
      spotlightView.startTarget(target)
      target.listener?.onStarted(target, index)
    } else {
      spotlightView.finishTarget(object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
          val previousIndex = currentIndex
          val previousTarget = targets[previousIndex]
          previousTarget.listener?.onEnded(previousTarget, previousIndex)
          if (index < targets.size) {
            val target = targets[index]
            currentIndex = index
            target.listener?.onStarting(target, index)
            spotlightView.startTarget(target)
            target.listener?.onStarted(target, index)
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
    if (currentIndex == NO_POSITION) return

    exitTransition.doOnEnd {
      spotlightView.cleanup()
      spotlightListener?.onEnded()
      currentIndex = NO_POSITION
    }

    if (spotlightView in container) {
      TransitionManager.beginDelayedTransition(container, exitTransition)
      container.removeView(spotlightView)
    }
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

    @ColorInt
    private var backgroundColor: Int = DEFAULT_OVERLAY_COLOR
    private var container: ViewGroup? = null
    private var listener: OnSpotlightListener? = null

    // Finish on touch outside of current target feature is disabled by default
    private var finishOnTouchOutsideOfCurrentTarget: Boolean = false
    private var finishOnBackPress: Boolean = false

    private var enterTransition: Any = AutoTransition()
    private var exitTransition: Any = enterTransition

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
    ) = apply {
      this.finishOnTouchOutsideOfCurrentTarget = finishOnTouchOutsideOfCurrentTarget
    }

    fun setFinishOnBackPress(finishOnBackPress: Boolean) = apply {
      this.finishOnBackPress = finishOnBackPress
    }

    fun setEnterTransition(enterTransition: Any) = apply {
      this.enterTransition = enterTransition
    }

    fun setEnterTransition(@TransitionRes enterTransition: Int) = apply {
      this.enterTransition = enterTransition
    }

    fun setExitTransition(exitTransition: Any) = apply {
      this.exitTransition = exitTransition
    }

    fun setExitTransition(@TransitionRes exitTransition: Int) = apply {
      this.exitTransition = exitTransition
    }

    fun build(): Spotlight {
      val spotlightView = SpotlightView(context, null, 0, backgroundColor)
      val targets = requireNotNull(targets) { "targets should not be null. " }
      val container = container ?: tryGetActivity(context)!!.window.decorView as ViewGroup
      return Spotlight(
          spotlightView = spotlightView,
          targets = targets,
          container = container,
          spotlightListener = listener,
          finishOnTouchOutsideOfCurrentTarget,
          finishOnBackPress,
          enterTransition,
          exitTransition
      )
    }

    companion object {
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
