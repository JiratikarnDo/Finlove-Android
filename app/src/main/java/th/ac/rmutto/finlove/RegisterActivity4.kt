package th.ac.rmutto.finlove

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import java.util.Calendar

class RegisterActivity4 : AppCompatActivity() {

    private var selectedEducation: String? = null
    private var selectedDateOfBirth: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register4)

        val buttonHighSchool = findViewById<Button>(R.id.buttonHighSchool)
        val buttonBachelor = findViewById<Button>(R.id.buttonBachelor)
        val buttonMaster = findViewById<Button>(R.id.buttonMaster)
        val buttonPhd = findViewById<Button>(R.id.buttonPhd)
        val buttonWorking = findViewById<Button>(R.id.buttonWorking)
        val editTextHome = findViewById<EditText>(R.id.editTextHome)
        val buttonSelectDate = findViewById<Button>(R.id.buttonSelectDate)
        val buttonNextStep4 = findViewById<ImageButton>(R.id.buttonNextStep4)

        // ฟังก์ชันสำหรับจัดการการคลิกปุ่มระดับการศึกษา
        setupEducationButton(buttonHighSchool, "มัธยมศึกษา")
        setupEducationButton(buttonBachelor, "ปริญญาตรี")
        setupEducationButton(buttonMaster, "ปริญญาโท")
        setupEducationButton(buttonPhd, "ปริญญาเอก")
        setupEducationButton(buttonWorking, "กำลังทำงาน")

        buttonSelectDate.setOnClickListener {
            val calendar = Calendar.getInstance()
            val year = calendar.get(Calendar.YEAR)
            val month = calendar.get(Calendar.MONTH)
            val day = calendar.get(Calendar.DAY_OF_MONTH)

            val datePickerDialog = DatePickerDialog(this, { _, selectedYear, selectedMonth, selectedDay ->
                selectedDateOfBirth = "$selectedYear-${String.format("%02d", selectedMonth + 1)}-${String.format("%02d", selectedDay)}"
                buttonSelectDate.text = selectedDateOfBirth
            }, year, month, day)

            // กำหนดให้เลือกวันเกิดได้ไม่เกินวันที่ปัจจุบัน
            datePickerDialog.datePicker.maxDate = calendar.timeInMillis

            datePickerDialog.show()
        }

        buttonNextStep4.setOnClickListener {
            val home = editTextHome.text.toString()

            if (home.isEmpty()) {
                editTextHome.error = "กรุณาระบุที่อยู่"
                return@setOnClickListener
            }

            if (selectedDateOfBirth.isNullOrEmpty()) {
                Toast.makeText(this, "กรุณาระบุวันเกิด", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (selectedEducation == null) {
                Toast.makeText(this, "กรุณาเลือกระดับการศึกษาก่อน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบว่าอายุถึงเกณฑ์ 18 ปีหรือไม่
            if (!isAgeAboveOrEqual18(selectedDateOfBirth!!)) {
                Toast.makeText(this, "อายุของคุณต้องมากกว่าหรือเท่ากับ 18 ปี", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // รับข้อมูลจากหน้า RegisterActivity3
            val email = intent.getStringExtra("email")
            val username = intent.getStringExtra("username")
            val password = intent.getStringExtra("password")
            val firstname = intent.getStringExtra("firstname")
            val lastname = intent.getStringExtra("lastname")
            val nickname = intent.getStringExtra("nickname")
            val gender = intent.getStringExtra("gender")
            val height = intent.getStringExtra("height")
            val phonenumber = intent.getStringExtra("phonenumber")

            // ส่งข้อมูลไปยัง RegisterActivity5
            val intent = Intent(this@RegisterActivity4, RegisterActivity5::class.java)
            intent.putExtra("email", email)
            intent.putExtra("username", username)
            intent.putExtra("password", password)
            intent.putExtra("firstname", firstname)
            intent.putExtra("lastname", lastname)
            intent.putExtra("nickname", nickname)
            intent.putExtra("gender", gender)
            intent.putExtra("height", height)
            intent.putExtra("phonenumber", phonenumber)
            intent.putExtra("dateOfBirth", selectedDateOfBirth)
            intent.putExtra("educationID", getEducationID(selectedEducation!!))
            intent.putExtra("home", home)
            startActivity(intent)
        }
    }

    // ฟังก์ชันสำหรับแปลง education เป็น educationID
    private fun getEducationID(education: String): Int {
        return when (education) {
            "มัธยมศึกษา" -> 1
            "ปริญญาตรี" -> 2
            "ปริญญาโท" -> 3
            "ปริญญาเอก" -> 4
            "กำลังทำงาน" -> 5
            else -> -1
        }
    }

    private fun setupEducationButton(button: Button, education: String) {
        button.setOnClickListener {
            findViewById<Button>(R.id.buttonHighSchool).isSelected = false
            findViewById<Button>(R.id.buttonBachelor).isSelected = false
            findViewById<Button>(R.id.buttonMaster).isSelected = false
            findViewById<Button>(R.id.buttonPhd).isSelected = false
            findViewById<Button>(R.id.buttonWorking).isSelected = false

            // ตั้งค่าสำหรับปุ่มที่ถูกเลือก
            button.isSelected = true
            selectedEducation = education
        }
    }

    // ฟังก์ชันตรวจสอบอายุ
    private fun isAgeAboveOrEqual18(birthDate: String): Boolean {
        val parts = birthDate.split("-")
        val birthYear = parts[0].toInt()
        val birthMonth = parts[1].toInt()
        val birthDay = parts[2].toInt()

        val today = Calendar.getInstance()
        var age = today.get(Calendar.YEAR) - birthYear

        // ลดอายุลง 1 ปีหากวันเกิดยังไม่ถึงในปีนี้
        if (today.get(Calendar.MONTH) + 1 < birthMonth ||
            (today.get(Calendar.MONTH) + 1 == birthMonth && today.get(Calendar.DAY_OF_MONTH) < birthDay)) {
            age -= 1
        }

        // ตรวจสอบว่าอายุมากกว่าหรือเท่ากับ 18 ปี
        return age >= 18
    }
}
