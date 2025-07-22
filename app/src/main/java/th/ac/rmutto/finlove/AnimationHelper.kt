package th.ac.rmutto.finlove.utils // หรือแพ็กเกจที่เหมาะสม

import android.animation.Animator
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.ImageButton
import th.ac.rmutto.finlove.R // ตรวจสอบให้แน่ใจว่า import R ถูกต้อง
import android.view.animation.BounceInterpolator

object AnimationHelper { // ใช้ object เพื่อให้เรียกใช้ได้โดยตรง ไม่ต้องสร้าง instance

    /**
     * Animates the pressed button with a scale effect.
     * @param button The ImageButton that was clicked (Like or Dislike).
     * @param onAnimationEnd Callback to execute after the button animation finishes.
     */
    fun animateButtonPress(button: ImageButton, onAnimationEnd: () -> Unit) {
        val buttonScaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.8f, 1f)
        val buttonScaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.8f, 1f)

        AnimatorSet().apply {
            playTogether(buttonScaleX, buttonScaleY)
            duration = 150
            interpolator = AccelerateDecelerateInterpolator()
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    button.isEnabled = false // ปิดการใช้งานปุ่มที่กด

                    // ปิดการใช้งานปุ่มอื่นๆ บนการ์ดเดียวกัน (ถ้ามี)
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = false
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    button.isEnabled = true // เปิดการใช้งานปุ่มที่กด

                    // เปิดการใช้งานปุ่มอื่นๆ บนการ์ดเดียวกัน
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = true
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = true

                    onAnimationEnd.invoke() // เรียก callback เมื่อแอนิเมชันปุ่มจบ
                }

                override fun onAnimationCancel(animation: Animator) {
                    button.isEnabled = true
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = true
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = true
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    fun animateButtonPressBounceRotate(button: ImageButton, onAnimationEnd: () -> Unit) {
        val buttonScaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.8f, 1.1f, 1f) // Scale in and slightly overshoot
        val buttonScaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.8f, 1.1f, 1f)
        val buttonRotation = ObjectAnimator.ofFloat(button, "rotation", 0f, -10f, 10f, 0f) // Rotate slightly

        AnimatorSet().apply {
            playTogether(buttonScaleX, buttonScaleY, buttonRotation)
            duration = 300 // เพิ่มระยะเวลาเล็กน้อยเพื่อให้เห็นเอฟเฟกต์
            interpolator = BounceInterpolator() // ใช้ BounceInterpolator เพื่อให้ดูเด้ง
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    button.isEnabled = false
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = false
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    button.isEnabled = true
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = true
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = true
                    onAnimationEnd.invoke()
                }

                override fun onAnimationCancel(animation: Animator) {
                    button.isEnabled = true
                    val parentView = button.parent as? ViewGroup
                    parentView?.findViewById<ImageButton>(R.id.buttonDislike)?.isEnabled = true
                    parentView?.findViewById<ImageButton>(R.id.buttonLike)?.isEnabled = true
                }
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /**
     * Animates a View to fade out quickly.
     * @param view The View to animate.
     * @param onAnimationEnd Callback to execute after the fade out animation finishes.
     */
    fun animateViewFadeOut(view: View, onAnimationEnd: () -> Unit) {
        ObjectAnimator.ofFloat(view, "alpha", 1f, 0f).apply {
            duration = 100 // ให้ Fade-out เร็วๆ
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {}
                override fun onAnimationEnd(animation: Animator) {
                    onAnimationEnd.invoke() // เรียก callback เมื่อแอนิเมชันจบ
                }
                override fun onAnimationCancel(animation: Animator) {}
                override fun onAnimationRepeat(animation: Animator) {}
            })
            start()
        }
    }

    /**
     * Animates a View to fade in and scale up from a smaller size.
     * @param view The View to animate.
     */
    fun animateViewAppear(view: View) {
        // ตั้งค่าเริ่มต้น (ซ่อนและขนาดเล็ก)
        view.alpha = 0f
        view.scaleX = 0.8f
        view.scaleY = 0.8f

        val fadeIn = ObjectAnimator.ofFloat(view, "alpha", 0f, 1f)
        val scaleX = ObjectAnimator.ofFloat(view, "scaleX", 0.8f, 1f)
        val scaleY = ObjectAnimator.ofFloat(view, "scaleY", 0.8f, 1f)

        AnimatorSet().apply {
            playTogether(fadeIn, scaleX, scaleY)
            duration = 300 // ความเร็วของแอนิเมชัน
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }
}