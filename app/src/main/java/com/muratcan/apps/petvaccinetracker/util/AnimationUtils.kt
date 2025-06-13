package com.muratcan.apps.petvaccinetracker.util

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.view.View
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.DecelerateInterpolator

object AnimationUtils {
    private const val ANIMATION_DURATION = 300L
    private const val PULSE_SCALE = 1.1f

    fun pulseAnimation(view: View) {
        val scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, PULSE_SCALE, 1f)
        val scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, PULSE_SCALE, 1f)

        val animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY)
        animator.duration = ANIMATION_DURATION
        animator.interpolator = AccelerateDecelerateInterpolator()
        animator.start()
    }

    fun crossFadeViews(viewToShow: View, viewToHide: View) {
        viewToShow.alpha = 0f
        viewToShow.visibility = View.VISIBLE

        viewToShow.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .start()

        viewToHide.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(DecelerateInterpolator())
            .setListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    viewToHide.visibility = View.GONE
                }
            })
            .start()
    }
} 