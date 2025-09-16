package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatButton
import androidx.core.view.WindowCompat
import androidx.core.view.isVisible
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

class OtpVerifyActivity : AppCompatActivity() {

    private val ioScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private lateinit var otp1: EditText
    private lateinit var otp2: EditText
    private lateinit var otp3: EditText
    private lateinit var otp4: EditText
    private lateinit var otp5: EditText
    private lateinit var otp6: EditText

    private lateinit var submitButton: AppCompatButton
    private lateinit var resendButton: AppCompatButton
    private lateinit var progressBar: ProgressBar
    private lateinit var tvCountdown: TextView

    private var email: String? = null
    private var requestId: String? = null

    private var isVerifying = false
    private var resendTimer: CountDownTimer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        // ใช้ layout ที่คุณวางไว้ (มีไอดีตามนี้)
        setContentView(R.layout.activity_otp_verify)

        // รับค่า intent
        email = intent.getStringExtra("email")
        requestId = intent.getStringExtra("requestId")

        if (email.isNullOrBlank()) {
            Toast.makeText(this, "ไม่มีอีเมลสำหรับยืนยัน", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // bind views
        otp1 = findViewById(R.id.otp1)
        otp2 = findViewById(R.id.otp2)
        otp3 = findViewById(R.id.otp3)
        otp4 = findViewById(R.id.otp4)
        otp5 = findViewById(R.id.otp5)
        otp6 = findViewById(R.id.otp6)

        submitButton = findViewById(R.id.submitButton)
        resendButton = findViewById(R.id.resendButton)
        progressBar = findViewById(R.id.progressBar)
        tvCountdown = findViewById(R.id.tvCountdown)

        // ตั้งค่าช่อง OTP ให้รับได้ทีละ 1 ตัว + โฟกัสอัตโนมัติ
        setupOtpInputs()

        // กดยืนยัน
        submitButton.setOnClickListener {
            val code = getOtpCode()
            if (code.length != 6) {
                Toast.makeText(this, "กรุณากรอกรหัสให้ครบ 6 หลัก", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            if (!isVerifying) verifyOtp(code)
        }

        // กดส่งใหม่
        resendButton.setOnClickListener {
            resendOtp()
        }

        // เปิดหน้ามาให้โฟกัสช่องแรก
        otp1.requestFocus()
        // เริ่มนับถอยหลังปุ่มส่งใหม่ (กันกดสแปม) ถ้าต้องการเริ่มทันที
        startResendCountdown()
    }

    override fun onDestroy() {
        super.onDestroy()
        resendTimer?.cancel()
    }

    // ---------------------- OTP UI helpers ----------------------

    private fun setupOtpInputs() {
        val boxes = arrayOf(otp1, otp2, otp3, otp4, otp5, otp6)

        // จำกัดความยาว 1 ตัว และ IME ให้จบที่ช่องสุดท้าย
        boxes.forEachIndexed { idx, et ->
            et.filters = arrayOf(InputFilter.LengthFilter(1))
            et.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

                override fun afterTextChanged(s: Editable?) {
                    if (s?.length == 1) {
                        // เมื่อพิมพ์ครบ 1 ตัว ไปช่องถัดไป
                        if (idx < boxes.lastIndex) {
                            boxes[idx + 1].requestFocus()
                        } else {
                            // ช่องสุดท้าย: ซ่อนคีย์บอร์ดอัตโนมัติได้ถ้าอยากทำ
                            // currentFocus?.hideKeyboard()
                        }
                    }
                }
            })

            // กด backspace บนช่องว่าง → ย้อนกลับช่องก่อนหน้า
            et.setOnKeyListener { _, keyCode, event ->
                val isDel = keyCode == android.view.KeyEvent.KEYCODE_DEL &&
                        event.action == android.view.KeyEvent.ACTION_DOWN
                if (isDel && et.text.isNullOrEmpty() && idx > 0) {
                    boxes[idx - 1].apply {
                        requestFocus()
                        setSelection(text?.length ?: 0)
                    }
                    return@setOnKeyListener true
                }
                false
            }

            // กด Done ที่คีย์บอร์ดบนช่องสุดท้าย = กดยืนยัน
            if (idx == boxes.lastIndex) {
                et.imeOptions = EditorInfo.IME_ACTION_DONE
                et.setOnEditorActionListener { _, actionId, _ ->
                    if (actionId == EditorInfo.IME_ACTION_DONE) {
                        submitButton.performClick()
                        true
                    } else false
                }
            }
        }
    }

    private fun getOtpCode(): String =
        "${otp1.text}${otp2.text}${otp3.text}${otp4.text}${otp5.text}${otp6.text}"

    private fun clearOtp() {
        otp1.text?.clear()
        otp2.text?.clear()
        otp3.text?.clear()
        otp4.text?.clear()
        otp5.text?.clear()
        otp6.text?.clear()
        otp1.requestFocus()
    }

    // ---------------------- API calls ----------------------

    private fun verifyOtp(code: String) {
        val email = this.email ?: return
        isVerifying = true
        setLoading(true)

        ioScope.launch {
            try {
                val client = OkHttpClient()
                val rootUrl = getString(R.string.root_url)
                val vUrl = "$rootUrl/api_v2/verify-otp"

                val form = FormBody.Builder()
                    .add("email", email)
                    .add("otp", code)
                    .build()

                val req = Request.Builder().url(vUrl).post(form).build()
                val res = client.newCall(req).execute()

                withContext(Dispatchers.Main) {
                    setLoading(false)
                    isVerifying = false

                    if (res.isSuccessful) {
                        Toast.makeText(this@OtpVerifyActivity, "ยืนยันสำเร็จ", Toast.LENGTH_SHORT).show()
                        // ไปหน้าแรก/หน้าถัดไป
                        val i = Intent(this@OtpVerifyActivity, FirstPageActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        }
                        startActivity(i)
                    } else {
                        Toast.makeText(this@OtpVerifyActivity, "รหัสไม่ถูกต้องหรือหมดอายุ", Toast.LENGTH_LONG).show()
                        clearOtp()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    setLoading(false)
                    isVerifying = false
                    Toast.makeText(this@OtpVerifyActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun resendOtp() {
        val email = this.email ?: return
        // disable ปุ่มและเริ่มนับถอยหลัง
        startResendCountdown()

        ioScope.launch {
            try {
                val client = OkHttpClient()
                val rootUrl = getString(R.string.root_url)
                val rUrl = "$rootUrl/api_v2/request-otp"

                val formBuilder = FormBody.Builder()
                    .add("email", email)
                // ถ้า backend รองรับ requestId ให้ส่งไปด้วย
                requestId?.let { formBuilder.add("requestId", it) }

                val req = Request.Builder().url(rUrl).post(formBuilder.build()).build()
                client.newCall(req).execute() // ไม่จำเป็นต้องอ่าน response body เสมอไป

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OtpVerifyActivity, "ส่งรหัสใหม่แล้ว", Toast.LENGTH_SHORT).show()
                }
            } catch (_: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OtpVerifyActivity, "ส่งรหัสใหม่ไม่สำเร็จ", Toast.LENGTH_LONG).show()
                    // ยกเลิก countdown เพื่อให้กดใหม่ได้ทันทีถ้าล้มเหลว
                    stopResendCountdown()
                }
            }
        }
    }

    // ---------------------- UI state helpers ----------------------

    private fun setLoading(loading: Boolean) {
        progressBar.isVisible = loading
        submitButton.isEnabled = !loading
        resendButton.isEnabled = !loading && resendButton.isEnabled // คงสถานะจาก countdown
        // ปิด/เปิดกล่อง OTP ขณะโหลด
        val enabled = !loading
        listOf(otp1, otp2, otp3, otp4, otp5, otp6).forEach { it.isEnabled = enabled }
    }

    private fun startResendCountdown(totalMillis: Long = 60_000L) {
        resendButton.isEnabled = false
        tvCountdown.isVisible = true
        resendTimer?.cancel()
        resendTimer = object : CountDownTimer(totalMillis, 1_000L) {
            override fun onTick(millisUntilFinished: Long) {
                val s = (millisUntilFinished / 1000L).toInt()
                tvCountdown.text = "ขอรหัสใหม่ได้ใน ${s} วินาที"
            }

            override fun onFinish() {
                tvCountdown.text = ""
                tvCountdown.isVisible = false
                resendButton.isEnabled = true
            }
        }.start()
    }

    private fun stopResendCountdown() {
        resendTimer?.cancel()
        tvCountdown.text = ""
        tvCountdown.isVisible = false
        resendButton.isEnabled = true
    }
}
