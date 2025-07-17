package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import android.text.InputFilter
import android.text.Spanned
import java.util.regex.Pattern

class RegisterActivity3 : AppCompatActivity() {

    private var selectedGender: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register3)

        val buttonMale = findViewById<Button>(R.id.buttonMale)
        val buttonFemale = findViewById<Button>(R.id.buttonFemale)
        val buttonOther = findViewById<Button>(R.id.buttonOther)
        val editTextHeight = findViewById<EditText>(R.id.editTextHeight)
        val editTextPhoneNumber = findViewById<EditText>(R.id.editTextPhoneNumber)
        val buttonNextStep3 = findViewById<ImageButton>(R.id.buttonNextStep3)

        setupGenderButton(buttonMale, "Male")
        setupGenderButton(buttonFemale, "Female")
        setupGenderButton(buttonOther, "Other")

        // กำหนดข้อจำกัดจำนวนตัวอักษรสำหรับหมายเลขโทรศัพท์
        editTextPhoneNumber.filters = arrayOf(InputFilter.LengthFilter(10))  // จำกัดเบอร์โทรไม่เกิน 10 ตัวอักษร

        // กำหนดข้อจำกัดรูปแบบของส่วนสูง ไม่เกิน 3 หลักเฉพาะตัวเลขก่อนทศนิยม
        editTextHeight.filters = arrayOf(ThreeDigitInputFilter())

        // รับข้อมูลจากหน้า RegisterActivity2
        val email = intent.getStringExtra("email")
        val username = intent.getStringExtra("username")
        val password = intent.getStringExtra("password")
        val firstname = intent.getStringExtra("firstname")
        val lastname = intent.getStringExtra("lastname")
        val nickname = intent.getStringExtra("nickname")

        buttonNextStep3.setOnClickListener {
            val height = editTextHeight.text.toString()
            val phoneNumber = editTextPhoneNumber.text.toString()

            if (selectedGender == null) {
                Toast.makeText(this, "กรุณาเลือกเพศก่อน", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (height.isEmpty()) {
                editTextHeight.error = "กรุณาระบุส่วนสูง"
                return@setOnClickListener
            }

            // ตรวจสอบว่าความสูงเป็นตัวเลขหรือไม่
            if (height.toIntOrNull() == null) {
                editTextHeight.error = "กรุณาระบุส่วนสูงที่เป็นตัวเลข"
                return@setOnClickListener
            }

            if (phoneNumber.isEmpty()) {
                editTextPhoneNumber.error = "กรุณาระบุเบอร์โทร"
                return@setOnClickListener
            }

            // ตรวจสอบความยาวของเบอร์โทรศัพท์
            if (phoneNumber.length != 10) {
                editTextPhoneNumber.error = "เบอร์โทรต้องมี 10 หลัก"
                return@setOnClickListener
            }

            // ส่งข้อมูลไปที่ RegisterActivity4
            val intent = Intent(this@RegisterActivity3, RegisterActivity4::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("password", password)
            intent.putExtra("firstname", firstname)
            intent.putExtra("lastname", lastname)
            intent.putExtra("nickname", nickname)
            intent.putExtra("gender", selectedGender)
            intent.putExtra("height", height)
            intent.putExtra("phonenumber", phoneNumber)
            startActivity(intent)
        }
    }

    private fun setupGenderButton(button: Button, gender: String) {
        button.setOnClickListener {
            findViewById<Button>(R.id.buttonMale).isSelected = false
            findViewById<Button>(R.id.buttonFemale).isSelected = false
            findViewById<Button>(R.id.buttonOther).isSelected = false
            button.isSelected = true
            selectedGender = gender
        }
    }

    // ฟิลเตอร์สำหรับกำหนดให้ป้อนตัวเลขได้ไม่เกิน 3 หลักก่อนทศนิยม และไม่ให้ป้อนทศนิยม
    class ThreeDigitInputFilter : InputFilter {

        private val pattern: Pattern = Pattern.compile("[0-9]{0,3}")

        override fun filter(
            source: CharSequence?,
            start: Int,
            end: Int,
            dest: Spanned?,
            dstart: Int,
            dend: Int
        ): CharSequence? {
            val matcher = pattern.matcher(dest.toString() + source.toString())
            return if (!matcher.matches()) "" else null
        }
    }
}
