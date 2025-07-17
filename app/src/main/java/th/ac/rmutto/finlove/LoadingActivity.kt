package th.ac.rmutto.finlove

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import th.ac.rmutto.finlove.databinding.ActivityLoadingBinding

class LoadingActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoadingBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // เริ่มต้น View Binding
        binding = ActivityLoadingBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // รับข้อมูลจาก Intent เพื่อตรวจสอบว่าจะไปหน้า Login หรือ Register
        val nextActivity = intent.getStringExtra("nextActivity")

        // เริ่มการ Animation ของจุด
        startDotAnimation()

        Handler(Looper.getMainLooper()).postDelayed({
            val intent = when (nextActivity) {
                "Login" -> Intent(this, LoginActivity::class.java)
                "Register" -> Intent(this, RegisterActivity::class.java)
                else -> null
            }
            intent?.let {
                startActivity(it)
                finish()
            }
        }, 1200) // เพิ่มเวลาการโหลดเป็น 3 วินาที
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)

        for ((index, dot) in dots.withIndex()) {
            // ใช้ PropertyValuesHolder เพื่อปรับขนาดตามแกน X และ Y
            ObjectAnimator.ofPropertyValuesHolder(
                dot,
                PropertyValuesHolder.ofFloat("scaleX", 1.5f, 0.5f, 1.5f), // ขยายและหดกลับในแนว X
                PropertyValuesHolder.ofFloat("scaleY", 1.5f, 0.5f, 1.5f)  // ขยายและหดกลับในแนว Y
            ).apply {
                duration = 500
                startDelay = (index * 150).toLong() // เว้นระยะการขยับของแต่ละจุด
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }
}
