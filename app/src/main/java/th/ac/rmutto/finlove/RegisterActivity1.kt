package th.ac.rmutto.finlove

import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import android.content.Intent
import android.text.InputFilter
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import android.util.Log

class RegisterActivity1 : AppCompatActivity() {

    private val client = OkHttpClient()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register1)

        val editTextEmail = findViewById<EditText>(R.id.editTextEmail)
        val editTextUsername = findViewById<EditText>(R.id.editTextUsername)
        val editTextPassword = findViewById<EditText>(R.id.editTextPassword)
        val buttonNext = findViewById<ImageButton>(R.id.buttonNext)

        // กำหนดข้อจำกัดจำนวนตัวอักษรตามฐานข้อมูล
        editTextUsername.filters = arrayOf(InputFilter.LengthFilter(20))
        editTextEmail.filters = arrayOf(InputFilter.LengthFilter(30))
        editTextPassword.filters = arrayOf(InputFilter.LengthFilter(20))

        buttonNext.setOnClickListener {
            val email = editTextEmail.text.toString()
            val username = editTextUsername.text.toString()
            val password = editTextPassword.text.toString()

            if (email.length > 40) {
                editTextEmail.error = "อีเมลต้องไม่เกิน 40 ตัวอักษร"
                return@setOnClickListener
            }

            if (username.length > 20) {
                editTextUsername.error = "ชื่อผู้ใช้ต้องไม่เกิน 20 ตัวอักษร"
                return@setOnClickListener
            }

            if (password.length > 20) {
                editTextPassword.error = "รหัสผ่านต้องไม่เกิน 20 ตัวอักษร"
                return@setOnClickListener
            }

            if (email.isEmpty()) {
                editTextEmail.error = "กรุณาระบุอีเมล"
                return@setOnClickListener
            }

            if (username.isEmpty()) {
                editTextUsername.error = "กรุณาระบุชื่อผู้ใช้"
                return@setOnClickListener
            }

            if (password.isEmpty()) {
                editTextPassword.error = "กรุณาระบุรหัสผ่าน"
                return@setOnClickListener
            }

            // Log ข้อมูลก่อนเรียก API
            Log.d("RegisterActivity1", "Calling checkUsernameEmail with username: $username, email: $email")

            checkUsernameEmail(username, email) { isAvailable, message ->
                if (isAvailable) {
                    val intent = Intent(this@RegisterActivity1, RegisterActivity2::class.java)
                    intent.putExtra("email", email)
                    intent.putExtra("username", username)
                    intent.putExtra("password", password)
                    startActivity(intent)
                } else {
                    Toast.makeText(this@RegisterActivity1, message, Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun checkUsernameEmail(username: String, email: String, callback: (Boolean, String) -> Unit) {
        val jsonObject = JSONObject().apply {
            put("username", username)
            put("email", email)
        }

        val mediaType = "application/json; charset=utf-8".toMediaTypeOrNull()
        val requestBody = jsonObject.toString().toRequestBody(mediaType)

        val url = getString(R.string.root_url) + "/api_v2/checkUsernameEmail"
        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        Log.d("RegisterActivity1", "Sending request to URL: $url with body: $jsonObject")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e("RegisterActivity1", "API call failed: ${e.message}")
                runOnUiThread {
                    Toast.makeText(this@RegisterActivity1, "เกิดข้อผิดพลาดในระบบ", Toast.LENGTH_SHORT).show()
                }
                callback(false, "เกิดข้อผิดพลาดในระบบ")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()
                Log.d("RegisterActivity1", "Response received with code: ${response.code}")

                runOnUiThread {
                    try {
                        if (!response.isSuccessful) {
                            Log.e("RegisterActivity1", "Server error: ${response.code}")
                            Toast.makeText(this@RegisterActivity1, "ชื่อผู้ใช้หรือีเมลนี้ถูกใช้แล้ว: ${response.code}", Toast.LENGTH_SHORT).show()
                            callback(false, "ชื่อผู้ใช้หรือีเมลนี้ถูกใช้แล้ว์: ${response.code}")
                            return@runOnUiThread
                        }

                        if (responseBody.isNullOrEmpty()) {
                            Log.e("RegisterActivity1", "Empty response from server")
                            Toast.makeText(this@RegisterActivity1, "ไม่ได้รับข้อมูลจากเซิร์ฟเวอร์", Toast.LENGTH_SHORT).show()
                            callback(false, "ไม่ได้รับข้อมูลจากเซิร์ฟเวอร์")
                            return@runOnUiThread
                        }

                        Log.d("RegisterActivity1", "Response body: $responseBody")

                        val jsonResponse = JSONObject(responseBody)
                        val isAvailable = jsonResponse.optBoolean("status", false)
                        val message = jsonResponse.optString("message", "เกิดข้อผิดพลาด")

                        Log.d("RegisterActivity1", "Parsed JSON - isAvailable: $isAvailable, message: $message")

                        callback(isAvailable, message)
                    } catch (e: Exception) {
                        Log.e("RegisterActivity1", "Error parsing JSON response: ${e.message}")
                        Toast.makeText(this@RegisterActivity1, "เกิดข้อผิดพลาดในการแปลงข้อมูล", Toast.LENGTH_SHORT).show()
                        callback(false, "เกิดข้อผิดพลาดในการแปลงข้อมูล")
                    }
                }
            }
        })
    }
}
