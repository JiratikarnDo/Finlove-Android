package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity7 : AppCompatActivity() {

    // ตัวแปรเก็บค่าเพศที่สนใจ
    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register7)

        // เชื่อมต่อปุ่มจาก XML
        val buttonMale = findViewById<Button>(R.id.buttonMale)
        val buttonFemale = findViewById<Button>(R.id.buttonFemale)
        val buttonOther = findViewById<Button>(R.id.buttonOther)
        val buttonNextStep7 = findViewById<ImageButton>(R.id.buttonNextStep7)

        // กำหนดการทำงานของปุ่มเลือกเพศ
        setupGenderButton(buttonMale, "ชาย")
        setupGenderButton(buttonFemale, "หญิง")
        setupGenderButton(buttonOther, "อื่นๆ")

        // เมื่อกดปุ่มถัดไป
        buttonNextStep7.setOnClickListener {
            if (selectedGender.isNullOrEmpty()) {
                Toast.makeText(this@RegisterActivity7, "กรุณาเลือกเพศที่คุณสนใจ", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val interestGenderID = getInterestGenderID(selectedGender)

            // รับข้อมูลทั้งหมดจาก RegisterActivity6 ที่ส่งผ่าน Intent
            val email = intent.getStringExtra("email")
            val username = intent.getStringExtra("username")
            val password = intent.getStringExtra("password")
            val firstname = intent.getStringExtra("firstname")
            val lastname = intent.getStringExtra("lastname")
            val nickname = intent.getStringExtra("nickname")
            val gender = intent.getStringExtra("gender")
            val height = intent.getStringExtra("height")
            val phonenumber = intent.getStringExtra("phonenumber")
            val dateOfBirth = intent.getStringExtra("dateOfBirth")
            val educationID = intent.getIntExtra("educationID", -1)
            val home = intent.getStringExtra("home")
            val preferences = intent.getStringExtra("preferences")
            val goalID = intent.getIntExtra("goalID", -1)

            // Log ข้อมูลที่รับเข้ามา
            Log.d("RegisterActivity7", "Received data: email: $email, username: $username, password: $password, firstname: $firstname, lastname: $lastname, nickname: $nickname, gender: $gender, height: $height, phonenumber: $phonenumber, dateOfBirth: $dateOfBirth, educationID: $educationID, home: $home, preferences: $preferences, goalID: $goalID, interestGenderID: $interestGenderID")

            if (educationID == -1 || email.isNullOrEmpty() || username.isNullOrEmpty() || goalID == -1) {
                Toast.makeText(this@RegisterActivity7, "ข้อมูลไม่ครบถ้วน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // สร้าง Intent เพื่อส่งข้อมูลไปยัง RegisterActivity8
            val intent = Intent(this@RegisterActivity7, RegisterActivity8::class.java).apply {
                putExtra("email", email)
                putExtra("username", username)
                putExtra("password", password)
                putExtra("firstname", firstname)
                putExtra("lastname", lastname)
                putExtra("nickname", nickname)
                putExtra("gender", gender)
                putExtra("height", height)
                putExtra("phonenumber", phonenumber)
                putExtra("dateOfBirth", dateOfBirth)
                putExtra("educationID", educationID)
                putExtra("home", home)
                putExtra("preferences", preferences)
                putExtra("interestGenderID", interestGenderID)
                putExtra("goalID", goalID)
            }


            startActivity(intent)
        }
    }

    // ฟังก์ชันสำหรับแปลงชื่อเพศเป็น interestGenderID
    private fun getInterestGenderID(gender: String?): Int {
        return when (gender) {
            "ชาย" -> 1
            "หญิง" -> 2
            "อื่นๆ" -> 3
            else -> -1
        }
    }

    // ฟังก์ชันสำหรับตั้งค่าการเลือกเพศ
    private fun setupGenderButton(button: Button, gender: String) {
        button.setOnClickListener {
            // ทำให้ปุ่มที่ถูกเลือก active
            findViewById<Button>(R.id.buttonMale).isSelected = false
            findViewById<Button>(R.id.buttonFemale).isSelected = false
            findViewById<Button>(R.id.buttonOther).isSelected = false
            button.isSelected = true

            // เก็บค่าเพศที่ถูกเลือก
            selectedGender = gender
        }
    }
}