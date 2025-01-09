package com.muratcan.apps.petvaccinetracker.util;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;

public class AnimationUtils {
    private static final int ANIMATION_DURATION = 300;
    private static final float PULSE_SCALE = 1.1f;

    public static void pulseAnimation(View view) {
        PropertyValuesHolder scaleX = PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, PULSE_SCALE, 1f);
        PropertyValuesHolder scaleY = PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, PULSE_SCALE, 1f);
        
        ObjectAnimator animator = ObjectAnimator.ofPropertyValuesHolder(view, scaleX, scaleY);
        animator.setDuration(ANIMATION_DURATION);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.start();
    }

    public static void crossFadeViews(View viewToShow, View viewToHide) {
        viewToShow.setAlpha(0f);
        viewToShow.setVisibility(View.VISIBLE);

        viewToShow.animate()
            .alpha(1f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .start();

        viewToHide.animate()
            .alpha(0f)
            .setDuration(ANIMATION_DURATION)
            .setInterpolator(new DecelerateInterpolator())
            .setListener(new AnimatorListenerAdapter() {
                @Override
                public void onAnimationEnd(Animator animation) {
                    viewToHide.setVisibility(View.GONE);
                }
            })
            .start();
    }
} 