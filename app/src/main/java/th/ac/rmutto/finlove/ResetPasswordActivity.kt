package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

class ResetPasswordActivity : AppCompatActivity() {

    private lateinit var newPasswordEditText: EditText
    private lateinit var confirmPasswordEditText: EditText
    private lateinit var resetButton: Button
    private lateinit var progressBar: ProgressBar
    private val client = OkHttpClient()

    private lateinit var email: String
    private lateinit var pin: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        newPasswordEditText = findViewById(R.id.newPasswordEditText)
        confirmPasswordEditText = findViewById(R.id.confirmPasswordEditText)
        resetButton = findViewById(R.id.resetButton)
        progressBar = findViewById(R.id.progressBar)

        // รับ email และ PIN จาก Intent
        email = intent.getStringExtra("email") ?: ""
        pin = intent.getStringExtra("pin") ?: "" // รับค่า PIN ที่ส่งมาจาก GetPINActivity

        resetButton.setOnClickListener {
            resetPassword()
        }
    }

    private fun resetPassword() {
        val newPassword = newPasswordEditText.text.toString().trim()
        val confirmPassword = confirmPasswordEditText.text.toString().trim()

        if (TextUtils.isEmpty(newPassword) || TextUtils.isEmpty(confirmPassword)) {
            Toast.makeText(this, "กรุณากรอกรหัสผ่านใหม่", Toast.LENGTH_LONG).show()
            return
        }

        if (newPassword != confirmPassword) {
            Toast.makeText(this, "รหัสผ่านไม่ตรงกัน", Toast.LENGTH_LONG).show()
            return
        }

        if (TextUtils.isEmpty(pin)) {  // เพิ่มการตรวจสอบว่ามีค่า PIN หรือไม่
            Toast.makeText(this, "ไม่พบ PIN", Toast.LENGTH_LONG).show()
            return
        }

        progressBar.visibility = View.VISIBLE
        resetButton.isEnabled = false

        // สร้าง JSON object สำหรับส่งไปยัง api_v2
        val json = JSONObject()
        json.put("email", email)
        json.put("pin", pin)  // ส่ง PIN ที่ได้รับจากหน้าอื่น
        json.put("newPassword", newPassword)

        Log.d("ResetPassword", "Request JSON: $json") // Log ข้อมูล JSON ที่จะส่งไปยัง api_v2

        val requestBody = json.toString().toRequestBody("application/json; charset=utf-8".toMediaType())

        val rootUrl = getString(R.string.root_url) // ดึงค่า root_url จาก strings.xml
        val url = "$rootUrl/api_v2/reset-password"

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resetButton.isEnabled = true
                    Toast.makeText(this@ResetPasswordActivity, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    resetButton.isEnabled = true
                    if (response.isSuccessful) {
                        Toast.makeText(this@ResetPasswordActivity, "รีเซ็ตรหัสผ่านสำเร็จ", Toast.LENGTH_LONG).show()

                        // นำไปหน้า LoginActivity
                        val intent = Intent(this@ResetPasswordActivity, LoginActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                        startActivity(intent)
                        finish() // ปิดหน้า ResetPasswordActivity
                    } else {
                        // ตรวจสอบ response body เมื่อเกิด error
                        response.body?.string()?.let { errorBody ->
                            Toast.makeText(this@ResetPasswordActivity, "Error: $errorBody", Toast.LENGTH_LONG).show()
                        } ?: run {
                            Toast.makeText(this@ResetPasswordActivity, "Unknown Error", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
        })
    }
}
