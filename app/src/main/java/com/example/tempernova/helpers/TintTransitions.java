package com.example.tempernova.helpers;

import android.animation.Animator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.util.AttributeSet;
import android.view.ViewGroup;

import androidx.appcompat.widget.AppCompatImageView;
import androidx.transition.Transition;
import androidx.transition.TransitionValues;
import androidx.vectordrawable.graphics.drawable.ArgbEvaluator;

public class TintTransitions extends Transition {

    private static final String PROPNAME_TINT = "com.example:TintTransition:tint";

    public TintTransitions(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    private void captureValues(TransitionValues values) {

        if (values.view instanceof AppCompatImageView) {
            values.values.put(PROPNAME_TINT, ((AppCompatImageView) values.view).getImageTintList());
        }

    }

    @Override
    public void captureStartValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }

    @Override
    public void captureEndValues(TransitionValues transitionValues) {
        captureValues(transitionValues);
    }


    @Override
    public Animator createAnimator(ViewGroup sceneRoot, final TransitionValues startValues, final TransitionValues endValues) {


        if (endValues == null) {
            return null;
        }

        if (!(endValues.view instanceof AppCompatImageView)) {
            return null;
        }


        ColorStateList startColorStateList = (ColorStateList) startValues.values.get(PROPNAME_TINT);
        ColorStateList endColorStateList = (ColorStateList) endValues.values.get(PROPNAME_TINT);

        final int endColor = endColorStateList.getDefaultColor();
        final int startColor = startColorStateList == null
                ? Color.TRANSPARENT
                : startColorStateList.getDefaultColor();

        ValueAnimator animator = ValueAnimator.ofObject(new ArgbEvaluator(), startColor, endColor);

        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                Integer color = (Integer) animation.getAnimatedValue();
                if (color != null) {
                    ((AppCompatImageView) endValues.view).setImageTintList(ColorStateList.valueOf(color));

                }
            }
        });


        return animator;

    }
}