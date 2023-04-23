package com.takusemba.spotlight

/**
 * Listener to notify the state of Target.
 */
interface OnTargetListener {
  /**
   * Called when Target is starting
   */
  fun onStarting(target: Target, index: Int) {}

  /**
   * Called when Target is started
   */
  fun onStarted(target: Target, index: Int) {}

  /**
   * Called when Target is started
   */
  fun onEnded(target: Target, index: Int) {}
}
