package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class RegisterActivity5 : AppCompatActivity() {

    private lateinit var selectedPreferences: MutableList<Int>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register5)

        selectedPreferences = mutableListOf()

        val buttonOption1 = findViewById<Button>(R.id.buttonOption1)
        val buttonOption2 = findViewById<Button>(R.id.buttonOption2)
        val buttonOption3 = findViewById<Button>(R.id.buttonOption3)
        val buttonOption4 = findViewById<Button>(R.id.buttonOption4)
        val buttonOption5 = findViewById<Button>(R.id.buttonOption5)
        val buttonOption6 = findViewById<Button>(R.id.buttonOption6)
        val buttonOption7 = findViewById<Button>(R.id.buttonOption7)
        val buttonOption8 = findViewById<Button>(R.id.buttonOption8)
        val buttonOption9 = findViewById<Button>(R.id.buttonOption9)
        val buttonOption10 = findViewById<Button>(R.id.buttonOption10)
        val buttonOption11 = findViewById<Button>(R.id.buttonOption11)
        val buttonOption12 = findViewById<Button>(R.id.buttonOption12)
        val buttonOption13 = findViewById<Button>(R.id.buttonOption13)
        val buttonOption14 = findViewById<Button>(R.id.buttonOption14)
        val buttonOption15 = findViewById<Button>(R.id.buttonOption15)
        val buttonOption16 = findViewById<Button>(R.id.buttonOption16)
        val buttonNextStep5 = findViewById<ImageButton>(R.id.buttonNextStep5)

        // เรียกใช้ setupButton เพื่อจัดการแต่ละปุ่ม
        setupButton(buttonOption1, "ฟุตบอล")
        setupButton(buttonOption2, "ภาพยนตร์")
        setupButton(buttonOption3, "ท่องเที่ยว")
        setupButton(buttonOption4, "อนิเมชั่น")
        setupButton(buttonOption5, "ช็อปปิ้ง")
        setupButton(buttonOption6, "เล่นดนตรี")
        setupButton(buttonOption7, "เล่นกีฬา")
        setupButton(buttonOption8, "เล่นเกม")
        setupButton(buttonOption9, "อ่านหนังสือ")
        setupButton(buttonOption10, "ปาร์ตี้")
        setupButton(buttonOption11, "สายควัน")
        setupButton(buttonOption12, "ออกกำลังกาย")
        setupButton(buttonOption13, "ตกปลา")
        setupButton(buttonOption14, "รักสัตว์")
        setupButton(buttonOption15, "ของหวาน")
        setupButton(buttonOption16, "ถ่ายรูป")

        buttonNextStep5.setOnClickListener {
            if (selectedPreferences.isEmpty()) {
                Toast.makeText(this, "กรุณาเลือกอย่างน้อยหนึ่งตัวเลือก", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val preferencesString = selectedPreferences.joinToString(",")
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

            if (educationID == -1) {
                Toast.makeText(this, "ไม่พบข้อมูล EducationID", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ส่งข้อมูลไปยัง RegisterActivity6
            val intent = Intent(this@RegisterActivity5, RegisterActivity6::class.java)
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
            intent.putExtra("preferences", preferencesString)
            startActivity(intent)
        }
    }

    private fun setupButton(button: Button, preference: String) {
        button.setOnClickListener {
            val preferenceID = getPreferenceID(preference)

            if (button.isSelected) {
                button.isSelected = false
                selectedPreferences.remove(preferenceID)
            } else {
                if (selectedPreferences.size < 3) {
                    button.isSelected = true
                    selectedPreferences.add(preferenceID)
                } else {
                    Toast.makeText(this, "เลือกได้ไม่เกิน 3 ตัวเลือก", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // เพิ่มตัวเลือกทั้งหมดใน getPreferenceID
    private fun getPreferenceID(preference: String): Int {
        return when (preference) {
            "ฟุตบอล" -> 1
            "ภาพยนตร์" -> 2
            "ท่องเที่ยว" -> 3
            "อนิเมชั่น" -> 4
            "ช็อปปิ้ง" -> 5
            "เล่นดนตรี" -> 6
            "เล่นกีฬา" -> 7
            "เล่นเกม" -> 8
            "อ่านหนังสือ" -> 9
            "ปาร์ตี้" -> 10
            "สายควัน" -> 11
            "ออกกำลังกาย" -> 12
            "ตกปลา" -> 13
            "รักสัตว์" -> 14
            "ของหวาน" -> 15
            "ถ่ายรูป" -> 16
            else -> -1
        }
    }
}
