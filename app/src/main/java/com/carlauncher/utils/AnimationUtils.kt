package com.carlauncher.utils

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.AnticipateOvershootInterpolator
import android.view.animation.DecelerateInterpolator
import android.view.animation.OvershootInterpolator
import androidx.core.animation.doOnEnd
import androidx.core.view.isVisible

object AnimationUtils {

    private const val DEFAULT_DURATION = 300L
    private const val SPRING_DAMPING = 0.7f
    private const val MACOS_EASE_OUT = 1.5f

    // macOS 风格的弹簧缩放进入动画
    fun scaleInAnimation(
        view: View,
        duration: Long = DEFAULT_DURATION,
        onEnd: (() -> Unit)? = null
    ) {
        view.alpha = 0f
        view.scaleX = 0.85f
        view.scaleY = 0.85f
        view.isVisible = true

        val animatorSet = AnimatorSet()
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.85f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.85f, 1f)

        alphaAnimator.duration = duration
        scaleXAnimator.duration = duration
        scaleYAnimator.duration = duration

        val interpolator = OvershootInterpolator(SPRING_DAMPING)
        scaleXAnimator.interpolator = interpolator
        scaleYAnimator.interpolator = interpolator
        alphaAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.doOnEnd {
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 风格的缩放退出动画
    fun scaleOutAnimation(
        view: View,
        duration: Long = 200,
        onEnd: (() -> Unit)? = null
    ) {
        val animatorSet = AnimatorSet()
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.9f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.9f)

        alphaAnimator.duration = duration
        scaleXAnimator.duration = duration
        scaleYAnimator.duration = duration

        val interpolator = AccelerateDecelerateInterpolator()
        alphaAnimator.interpolator = interpolator
        scaleXAnimator.interpolator = interpolator
        scaleYAnimator.interpolator = interpolator

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.doOnEnd {
            view.isVisible = false
            view.scaleX = 1f
            view.scaleY = 1f
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS Dock 风格的弹入动画
    fun dockBounceAnimation(
        view: View,
        duration: Long = 400,
        onEnd: (() -> Unit)? = null
    ) {
        view.alpha = 0f
        view.scaleX = 0.5f
        view.scaleY = 0.5f
        view.isVisible = true

        val animatorSet = AnimatorSet()
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.5f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.5f, 1.1f, 1f)

        alphaAnimator.duration = duration
        scaleXAnimator.duration = duration
        scaleYAnimator.duration = duration

        val interpolator = AnticipateOvershootInterpolator(1.2f)
        scaleXAnimator.interpolator = interpolator
        scaleYAnimator.interpolator = interpolator

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.doOnEnd {
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS Mission Control 风格的滑入动画（从底部）
    fun slideInFromBottom(
        view: View,
        duration: Long = DEFAULT_DURATION,
        onEnd: (() -> Unit)? = null
    ) {
        view.translationY = view.height.toFloat()
        view.alpha = 0f
        view.isVisible = true

        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationY", view.height.toFloat(), 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        translateAnimator.duration = duration
        alphaAnimator.duration = duration

        translateAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)
        alphaAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)

        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.doOnEnd {
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 风格的滑出动画（向底部）
    fun slideOutToBottom(
        view: View,
        duration: Long = 200,
        onEnd: (() -> Unit)? = null
    ) {
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationY", 0f, view.height.toFloat())
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        translateAnimator.duration = duration
        alphaAnimator.duration = duration

        val interpolator = AccelerateDecelerateInterpolator()
        translateAnimator.interpolator = interpolator
        alphaAnimator.interpolator = interpolator

        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.doOnEnd {
            view.isVisible = false
            view.translationY = 0f
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 风格的淡入动画
    fun fadeIn(
        view: View,
        duration: Long = 200,
        onEnd: (() -> Unit)? = null
    ) {
        view.alpha = 0f
        view.isVisible = true

        ObjectAnimator.ofFloat(view, "alpha", 0f, 1f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(MACOS_EASE_OUT)
            start()
            doOnEnd { onEnd?.invoke() }
        }
    }

    // macOS 风格的淡出动画
    fun fadeOut(
        view: View,
        duration: Long = 150,
        onEnd: (() -> Unit)? = null
    ) {
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
            doOnEnd {
                view.isVisible = false
                onEnd?.invoke()
            }
        }
    }

    // macOS 按钮点击反馈动画（弹性按压）
    fun buttonPressAnimation(view: View) {
        val animatorSet = AnimatorSet()

        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 0.92f),
            PropertyValuesHolder.ofFloat("scaleY", 0.92f)
        )
        scaleDown.duration = 80
        scaleDown.interpolator = DecelerateInterpolator()

        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        )
        scaleUp.duration = 200
        scaleUp.interpolator = OvershootInterpolator(0.6f)

        animatorSet.playSequentially(scaleDown, scaleUp)
        animatorSet.start()
    }

    // macOS Launchpad 风格的图标展开动画
    fun iconSpreadAnimation(
        view: View,
        delay: Long = 0,
        duration: Long = 350,
        onEnd: (() -> Unit)? = null
    ) {
        view.alpha = 0f
        view.scaleX = 0.6f
        view.scaleY = 0.6f
        view.isVisible = true

        val animatorSet = AnimatorSet()
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 0.6f, 1.05f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 0.6f, 1.05f, 1f)

        alphaAnimator.duration = duration
        scaleXAnimator.duration = duration
        scaleYAnimator.duration = duration

        val interpolator = OvershootInterpolator(0.8f)
        scaleXAnimator.interpolator = interpolator
        scaleYAnimator.interpolator = interpolator
        alphaAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)

        animatorSet.playTogether(alphaAnimator, scaleXAnimator, scaleYAnimator)
        animatorSet.startDelay = delay
        animatorSet.doOnEnd {
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 窗口最小化动画（Genie effect 简化版）
    fun genieMinimizeAnimation(
        view: View,
        duration: Long = 300,
        onEnd: (() -> Unit)? = null
    ) {
        val animatorSet = AnimatorSet()
        val scaleXAnimator = ObjectAnimator.ofFloat(view, "scaleX", 1f, 0.1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(view, "scaleY", 1f, 0.1f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        scaleXAnimator.duration = duration
        scaleYAnimator.duration = duration
        alphaAnimator.duration = duration

        val interpolator = AccelerateDecelerateInterpolator()
        scaleXAnimator.interpolator = interpolator
        scaleYAnimator.interpolator = interpolator
        alphaAnimator.interpolator = interpolator

        animatorSet.playTogether(scaleXAnimator, scaleYAnimator, alphaAnimator)
        animatorSet.doOnEnd {
            view.isVisible = false
            view.scaleX = 1f
            view.scaleY = 1f
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 图标旋转动画
    fun rotateAnimation(
        view: View,
        degrees: Float = 180f,
        duration: Long = 300
    ): ObjectAnimator {
        return ObjectAnimator.ofFloat(view, "rotation", view.rotation, view.rotation + degrees).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator(MACOS_EASE_OUT)
            start()
        }
    }

    // macOS 脉冲动画（用于提示）
    fun pulseAnimation(view: View, duration: Long = 1200) {
        val scaleUp = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1.08f),
            PropertyValuesHolder.ofFloat("scaleY", 1.08f)
        )
        scaleUp.duration = duration / 2
        scaleUp.interpolator = DecelerateInterpolator()

        val scaleDown = ObjectAnimator.ofPropertyValuesHolder(
            view,
            PropertyValuesHolder.ofFloat("scaleX", 1f),
            PropertyValuesHolder.ofFloat("scaleY", 1f)
        )
        scaleDown.duration = duration / 2
        scaleDown.interpolator = DecelerateInterpolator()

        scaleUp.start()
        scaleUp.doOnEnd {
            scaleDown.start()
            scaleDown.doOnEnd {
                pulseAnimation(view, duration)
            }
        }
    }

    // macOS 侧边栏滑入动画
    fun slideInFromLeft(
        view: View,
        duration: Long = DEFAULT_DURATION,
        onEnd: (() -> Unit)? = null
    ) {
        view.translationX = -view.width.toFloat()
        view.alpha = 0f
        view.isVisible = true

        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationX", -view.width.toFloat(), 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)

        translateAnimator.duration = duration
        alphaAnimator.duration = duration

        translateAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)
        alphaAnimator.interpolator = DecelerateInterpolator(MACOS_EASE_OUT)

        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.doOnEnd {
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // macOS 侧边栏滑出动画
    fun slideOutToLeft(
        view: View,
        duration: Long = 200,
        onEnd: (() -> Unit)? = null
    ) {
        val animatorSet = AnimatorSet()
        val translateAnimator = ObjectAnimator.ofFloat(view, "translationX", 0f, -view.width.toFloat())
        val alphaAnimator = ObjectAnimator.ofFloat(view, "alpha", 1f, 0f)

        translateAnimator.duration = duration
        alphaAnimator.duration = duration

        val interpolator = AccelerateDecelerateInterpolator()
        translateAnimator.interpolator = interpolator
        alphaAnimator.interpolator = interpolator

        animatorSet.playTogether(translateAnimator, alphaAnimator)
        animatorSet.doOnEnd {
            view.isVisible = false
            view.translationX = 0f
            onEnd?.invoke()
        }
        animatorSet.start()
    }

    // 背景模糊淡入（用于面板遮罩）
    fun backdropFadeIn(
        view: View,
        duration: Long = 200,
        onEnd: (() -> Unit)? = null
    ) {
        view.alpha = 0f
        view.isVisible = true

        ObjectAnimator.ofFloat(view, "alpha", 0f, 0.3f).apply {
            this.duration = duration
            interpolator = DecelerateInterpolator()
            start()
            doOnEnd { onEnd?.invoke() }
        }
    }

    // 背景模糊淡出
    fun backdropFadeOut(
        view: View,
        duration: Long = 150,
        onEnd: (() -> Unit)? = null
    ) {
        ObjectAnimator.ofFloat(view, "alpha", 0.3f, 0f).apply {
            this.duration = duration
            interpolator = AccelerateDecelerateInterpolator()
            start()
            doOnEnd {
                view.isVisible = false
                onEnd?.invoke()
            }
        }
    }
}
