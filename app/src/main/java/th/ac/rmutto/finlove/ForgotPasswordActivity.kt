package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.TextUtils
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException

class ForgotPasswordActivity : AppCompatActivity() {

    private lateinit var emailEditText: EditText
    private lateinit var sendButton: Button

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        emailEditText = findViewById(R.id.emailEditText)
        sendButton = findViewById(R.id.sendButton)

        sendButton.setOnClickListener {
            sendResetPasswordRequest()
        }
    }

    private fun sendResetPasswordRequest() {
        val email = emailEditText.text.toString().trim()

        if (TextUtils.isEmpty(email)) {
            emailEditText.error = "Please enter your email"
            return
        }

        val requestBody = FormBody.Builder()
            .add("email", email)
            .build()
        val rootUrl = getString(R.string.root_url) // ดึงค่า root_url จาก strings.xml
        val url = "$rootUrl/api_v2/request-pin" // ประกอบ URL กับ path ที่ต้องการ
        val request = Request.Builder()
            .url(url) // เปลี่ยน URL ตามต้องการ
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // แสดงข้อความเมื่อเกิดข้อผิดพลาดในการเชื่อมต่อ
                    Toast.makeText(this@ForgotPasswordActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        val message = response.body?.string()
                        Toast.makeText(this@ForgotPasswordActivity, message, Toast.LENGTH_LONG).show()

                        // เรียกหน้า LoadingActivity
                        val loadingIntent = Intent(this@ForgotPasswordActivity, LoadingActivity::class.java)
                        startActivity(loadingIntent)

                        // ใช้ Handler เพื่อทำการหน่วงเวลา ก่อนเปลี่ยนไปหน้า GetPINActivity
                        Handler(Looper.getMainLooper()).postDelayed({
                            val intent = Intent(this@ForgotPasswordActivity, GetPINActivity::class.java)
                            intent.putExtra("email", email)  // ส่ง email ไปด้วย
                            startActivity(intent)
                            finish() // ปิด ForgotPasswordActivity เพื่อไม่ให้ย้อนกลับมา
                        }, 1500) // หน่วงเวลา 1.5 วินาที
                    } else {
                        // แสดงข้อความเมื่ออีเมลไม่ถูกต้อง โดยไม่ต้องแสดงหน้า LoadingActivity
                        response.body?.string()?.let { errorBody ->
                            Toast.makeText(this@ForgotPasswordActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                        } ?: run {
                            Toast.makeText(this@ForgotPasswordActivity, "Unknown Error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}
