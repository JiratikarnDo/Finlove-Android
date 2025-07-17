package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity6 : AppCompatActivity() {

    private var selectedGoalID: Int? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register6)

        // เชื่อมต่อปุ่มต่างๆ กับ XML layout
        val buttonGoal1 = findViewById<Button>(R.id.buttonGoal1)
        val buttonGoal2 = findViewById<Button>(R.id.buttonGoal2)
        val buttonGoal3 = findViewById<Button>(R.id.buttonGoal3)
        val buttonGoal4 = findViewById<Button>(R.id.buttonGoal4)
        val buttonNextStep6 = findViewById<ImageButton>(R.id.buttonNextStep6)

        // ตั้งค่าปุ่ม goal เพื่อเลือกเป้าหมาย
        setupGoalButton(buttonGoal1, 1)
        setupGoalButton(buttonGoal2, 2)
        setupGoalButton(buttonGoal3, 3)
        setupGoalButton(buttonGoal4, 4)

        // เมื่อกดปุ่ม "ถัดไป"
        buttonNextStep6.setOnClickListener {
            if (selectedGoalID == null) {
                Toast.makeText(this, "กรุณาเลือกหนึ่งเป้าหมาย", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // รับข้อมูลจาก Intent ที่ส่งมาจาก RegisterActivity5
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

            // ตรวจสอบว่าข้อมูลทั้งหมดครบถ้วนหรือไม่
            if (email.isNullOrEmpty() || username.isNullOrEmpty() || password.isNullOrEmpty() || firstname.isNullOrEmpty()
                || lastname.isNullOrEmpty() || nickname.isNullOrEmpty() || gender.isNullOrEmpty() || height.isNullOrEmpty()
                || phonenumber.isNullOrEmpty() || dateOfBirth.isNullOrEmpty() || educationID == -1 || home.isNullOrEmpty() || preferences.isNullOrEmpty()) {
                Toast.makeText(this, "ข้อมูลไม่ครบถ้วน กรุณาตรวจสอบข้อมูลอีกครั้ง", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ส่งข้อมูลไปยัง RegisterActivity7 โดยไม่บันทึกข้อมูลใน RegisterActivity6
            val intent = Intent(this@RegisterActivity6, RegisterActivity7::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("password", password)
            intent.putExtra("firstname", firstname)
            intent.putExtra("lastname", lastname)
            intent.putExtra("nickname", nickname)
            intent.putExtra("gender", gender)
            intent.putExtra("height", height)
            intent.putExtra("phonenumber", phonenumber)
            intent.putExtra("dateOfBirth", dateOfBirth)
            intent.putExtra("educationID", educationID)
            intent.putExtra("home", home)
            intent.putExtra("preferences", preferences)
            intent.putExtra("goalID", selectedGoalID)  // เพิ่ม goalID
            startActivity(intent)

        }
    }

    // ฟังก์ชันสำหรับตั้งค่าการทำงานของปุ่ม goal แต่ละปุ่ม
    private fun setupGoalButton(button: Button, goalID: Int) {
        button.setOnClickListener {
            // ทำให้ปุ่มที่ถูกเลือก active
            findViewById<Button>(R.id.buttonGoal1).isSelected = false
            findViewById<Button>(R.id.buttonGoal2).isSelected = false
            findViewById<Button>(R.id.buttonGoal3).isSelected = false
            findViewById<Button>(R.id.buttonGoal4).isSelected = false
            button.isSelected = true

            // เก็บค่า goalID
            selectedGoalID = goalID
        }
    }
}
