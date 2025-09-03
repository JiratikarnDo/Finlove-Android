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
import android.animation.ValueAnimator
import android.view.animation.LinearInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.ImageView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner

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

    fun animateButtonPressSubtle(button: View, onAnimationEnd: () -> Unit) {
        val buttonScaleX = ObjectAnimator.ofFloat(button, "scaleX", 1f, 0.95f, 1f)
        val buttonScaleY = ObjectAnimator.ofFloat(button, "scaleY", 1f, 0.95f, 1f)

        AnimatorSet().apply {
            playTogether(buttonScaleX, buttonScaleY)
            duration = 200 // การกดที่ไม่เร็วเกินไป
            interpolator = AccelerateDecelerateInterpolator() // สร้างการเคลื่อนไหวที่ราบรื่น
            addListener(object : Animator.AnimatorListener {
                override fun onAnimationStart(animation: Animator) {
                    button.isEnabled = false
                }

                override fun onAnimationEnd(animation: Animator) {
                    button.isEnabled = true
                    onAnimationEnd.invoke() // เรียก callback เมื่อแอนิเมชันจบ
                }

                override fun onAnimationCancel(animation: Animator) {
                    button.isEnabled = true
                }

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
            duration = 350 // ความเร็วของแอนิเมชัน
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    /**
     * อนิเมทเฉพาะ ImageView ใหม่ สไลด์เข้าจากขวา + เฟดอิน
     * ปุ่มและข้อความรอบ ๆ จะไม่ขยับ
     */
    fun animateImageSlideInFromRight(
        imageView: ImageView,
        duration: Long = 360L,
        onLoadNew: () -> Unit
    ) {
        // ยกเลิกอนิเมชันค้าง
        imageView.animate().cancel()

        // โหลดรูปใหม่เข้ามาก่อน
        onLoadNew()

        val screenW = imageView.resources.displayMetrics.widthPixels.toFloat()

        // จุดเริ่ม: อยู่นอกจอขวา + จาง + scale เล็กนิด
        imageView.translationX = screenW * 0.85f
        imageView.alpha = 0f
        imageView.scaleX = 0.97f
        imageView.scaleY = 0.97f

        val slideIn = ObjectAnimator.ofFloat(imageView, View.TRANSLATION_X, imageView.translationX, 0f)
        val fadeIn  = ObjectAnimator.ofFloat(imageView, View.ALPHA, 0f, 1f)
        val scaleX  = ObjectAnimator.ofFloat(imageView, View.SCALE_X, 0.97f, 1f)
        val scaleY  = ObjectAnimator.ofFloat(imageView, View.SCALE_Y, 0.97f, 1f)

        AnimatorSet().apply {
            playTogether(slideIn, fadeIn, scaleX, scaleY)
            this.duration = duration
            interpolator = OvershootInterpolator(1.04f) // เด้งนุ่มๆ เล็กน้อย
            start()
        }
    }
    /** สร้างชุดอนิเมชัน “ขยับเบา ๆ” สำหรับมาสคอต แต่ยังไม่ start */
    fun mascotIdle(
        target: View,
        amplitudeY: Float = 16f,      // เด้งขึ้นลงแค่ไหน (dp → px ถ้าต้องการละเอียดค่อยแปลง)
        rotationDeg: Float = 2.5f,    // แกว่งซ้ายขวา
        scaleMax: Float = 1.03f       // หายใจ นิด ๆ
    ): AnimatorSet {
        val bob = ObjectAnimator.ofFloat(target, View.TRANSLATION_Y, 0f, -amplitudeY).apply {
            duration = 1400
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val sway = ObjectAnimator.ofFloat(target, View.ROTATION, -rotationDeg, rotationDeg).apply {
            duration = 1600
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
        }
        val breatheX = ObjectAnimator.ofFloat(target, View.SCALE_X, 1f, scaleMax, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        val breatheY = ObjectAnimator.ofFloat(target, View.SCALE_Y, 1f, scaleMax, 1f).apply {
            duration = 2400
            repeatCount = ValueAnimator.INFINITE
            interpolator = LinearInterpolator()
        }
        return AnimatorSet().apply { playTogether(bob, sway, breatheX, breatheY) }
    }

    /** แนบอนิเมชันกับ lifecycle ของ Fragment/Activity ให้ start/stop อัตโนมัติ */
    fun attachMascotIdle(owner: LifecycleOwner, target: ImageView): AnimatorSet {
        val set = mascotIdle(target)
        owner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) { set.start() }
            override fun onStop(owner: LifecycleOwner) { stopAndReset(target, set) }
        })
        return set
    }

    /** ปุ่มเด้งสั้น ๆ เวลาแตะ */
    fun boing(target: View, scale: Float = 1.08f, upMs: Long = 160, downMs: Long = 180) {
        target.animate()
            .setDuration(upMs)
            .scaleX(scale).scaleY(scale)
            .withEndAction {
                target.animate().setDuration(downMs).scaleX(1f).scaleY(1f).start()
            }.start()
    }

    /** ยกเลิกและรีเซ็ตค่าทรานส์ฟอร์ม */
    fun stopAndReset(target: View, animator: Animator?) {
        animator?.cancel()
        target.apply {
            translationX = 0f; translationY = 0f
            rotation = 0f; scaleX = 1f; scaleY = 1f
        }
    }

}
