package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.ImageButton
import android.text.InputFilter
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity2 : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register2)

        val editTextFirstname = findViewById<EditText>(R.id.editTextFirstname)
        val editTextLastname = findViewById<EditText>(R.id.editTextLastname)
        val editTextNickname = findViewById<EditText>(R.id.editTextNickname)
        val buttonNextStep2 = findViewById<ImageButton>(R.id.buttonNextStep2)

        // รับข้อมูลจาก RegisterActivity1
        val email = intent.getStringExtra("email")
        val username = intent.getStringExtra("username")
        val password = intent.getStringExtra("password")

        // กำหนดข้อจำกัดจำนวนตัวอักษรตามฐานข้อมูล
        editTextFirstname.filters = arrayOf(InputFilter.LengthFilter(20))  // จำกัดชื่อจริงไม่เกิน 20 ตัวอักษร
        editTextLastname.filters = arrayOf(InputFilter.LengthFilter(20))   // จำกัดนามสกุลไม่เกิน 20 ตัวอักษร
        editTextNickname.filters = arrayOf(InputFilter.LengthFilter(15))   // จำกัดชื่อเล่นไม่เกิน 15 ตัวอักษร

        buttonNextStep2.setOnClickListener {
            val firstname = editTextFirstname.text.toString()
            val lastname = editTextLastname.text.toString()
            val nickname = editTextNickname.text.toString()

            // ตรวจสอบความยาวของข้อมูลก่อนส่งไปยังหน้าถัดไป
            if (firstname.length > 20) {
                editTextFirstname.error = "ชื่อจริงต้องไม่เกิน 20 ตัวอักษร"
                return@setOnClickListener
            }

            if (lastname.length > 20) {
                editTextLastname.error = "นามสกุลต้องไม่เกิน 20 ตัวอักษร"
                return@setOnClickListener
            }

            if (nickname.length > 15) {
                editTextNickname.error = "ชื่อเล่นต้องไม่เกิน 15 ตัวอักษร"
                return@setOnClickListener
            }

            if (firstname.isEmpty()) {
                editTextFirstname.error = "กรุณาระบุชื่อจริง"
                return@setOnClickListener
            }

            if (lastname.isEmpty()) {
                editTextLastname.error = "กรุณาระบุนามสกุล"
                return@setOnClickListener
            }

            if (nickname.isEmpty()) {
                editTextNickname.error = "กรุณาระบุชื่อเล่น"
                return@setOnClickListener
            }

            // ส่งข้อมูลทั้งหมดไปยัง RegisterActivity3
            val intent = Intent(this@RegisterActivity2, RegisterActivity3::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("password", password)
            intent.putExtra("firstname", firstname)
            intent.putExtra("lastname", lastname)
            intent.putExtra("nickname", nickname)
            startActivity(intent)
        }
    }
}
