package com.takusemba.spotlight.transition

import androidx.transition.Transition
import androidx.transition.TransitionListenerAdapter

internal inline fun Transition.doOnceOnEnd(
    crossinline action: (transition: Transition) -> Unit
) {
  addListener(object : TransitionListenerAdapter() {
    override fun onTransitionEnd(transition: Transition) {
      transition.removeListener(this)
      action.invoke(transition)
    }
  })
}

internal inline fun Transition.doOnceOnStart(
    crossinline action: (transition: Transition) -> Unit
) {
  addListener(object : TransitionListenerAdapter() {
    override fun onTransitionStart(transition: Transition) {
      transition.removeListener(this)
      action.invoke(transition)
    }
  })
}
