package th.ac.rmutto.finlove

import android.app.DatePickerDialog
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import java.util.Calendar
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.AdapterView
import android.widget.Spinner
import com.google.android.material.textfield.MaterialAutoCompleteTextView

class RegisterActivity4 : AppCompatActivity() {

    private var selectedEducation: String? = null
    private var selectedCareerName: String? = null
    private var selectedDateOfBirth: String? = null
    private var selectedCareerId: Int? = null
    private var selectedProvince: String? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register4)

        val buttonHighSchool = findViewById<Button>(R.id.buttonHighSchool)
        val buttonBachelor = findViewById<Button>(R.id.buttonBachelor)
        val buttonMaster = findViewById<Button>(R.id.buttonMaster)
        val buttonPhd = findViewById<Button>(R.id.buttonPhd)
        val editTextHome = findViewById<EditText>(R.id.editTextHome)
        val buttonSelectDate = findViewById<Button>(R.id.buttonSelectDate)
        val buttonNextStep4 = findViewById<ImageButton>(R.id.buttonNextStep4)
        val careerDropdown = findViewById<AutoCompleteTextView>(R.id.dropdowncareer)

        // รายการต้อง “ตรงกับตาราง careerdetail” ของคุณ
        val careerItems = listOf(
            0 to "ไม่ระบุ",
            1 to "นักเรียน",
            2 to "นักศึกษา",
            3 to "พนักงาน",
            4 to "ข้าราชการ"
            // ถ้ามี "ไม่ระบุ" = 0 ก็เพิ่มได้: 0 to "ไม่ระบุ"
        )
        val careerNames = careerItems.map { it.second }

        careerDropdown.setAdapter(
            ArrayAdapter(this, android.R.layout.simple_list_item_1, careerNames)
        )

        careerDropdown.setOnItemClickListener { _, _, position, _ ->
            val (id, name) = careerItems[position]
            selectedCareerId = id
            selectedCareerName = name
        }

// ให้แตะแล้วลิสต์เด้งทันที
        careerDropdown.setOnClickListener { careerDropdown.showDropDown() }
        careerDropdown.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) careerDropdown.showDropDown()
        }

        // ---------- [บล็อก A] ใส่ตรงนี้: findViewById + adapter + listener ----------
        // --- Dropdown จังหวัด ---
        val provinceDropdown = findViewById<AutoCompleteTextView>(R.id.dropdownProvince)

        val provinces = resources.getStringArray(R.array.province_display_array)
        val provinceAdapter = ArrayAdapter(this, android.R.layout.simple_list_item_1, provinces)
        provinceDropdown.setAdapter(provinceAdapter)

        provinceDropdown.setOnItemClickListener { parent, _, position, _ ->
            selectedProvince = parent.getItemAtPosition(position)?.toString()
        }

// แตะแล้วให้แสดงลิสต์ทันที (สะดวกขึ้น)
        provinceDropdown.setOnClickListener { provinceDropdown.showDropDown() }
        provinceDropdown.setOnFocusChangeListener { _, hasFocus -> if (hasFocus) provinceDropdown.showDropDown() }
        // ---------------------------------------------------------------------------

        // ฟังก์ชันสำหรับจัดการการคลิกปุ่มระดับการศึกษา
        setupEducationButton(buttonHighSchool, "มัธยมศึกษา")
        setupEducationButton(buttonBachelor, "ปริญญาตรี")
        setupEducationButton(buttonMaster, "ปริญญาโท")
        setupEducationButton(buttonPhd, "ปริญญาเอก")

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

            if (selectedCareerId == null) {
                Toast.makeText(this, "กรุณาเลือกอาชีพก่อน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ตรวจสอบว่าอายุถึงเกณฑ์ 18 ปีหรือไม่
            if (!isAgeAboveOrEqual18(selectedDateOfBirth!!)) {
                Toast.makeText(this, "อายุของคุณต้องมากกว่าหรือเท่ากับ 18 ปี", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // ---------- [บล็อก B] ใส่เช็คจังหวัดก่อนส่งต่อ ----------
            if (selectedProvince.isNullOrEmpty()) {
                Toast.makeText(this, "กรุณาเลือกจังหวัด", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            // -------------------------------------------------------

            // รับข้อมูลจากหน้า RegisterActivity3
            val email = intent.getStringExtra("email")
            val username = intent.getStringExtra("username")
            val password = intent.getStringExtra("password")
            val firstname = intent.getStringExtra("firstname")
            val lastname = intent.getStringExtra("lastname")
            val nickname = intent.getStringExtra("nickname")
            val gender = intent.getStringExtra("gender")
            val height = intent.getStringExtra("height")
            val weight = intent.getStringExtra("weight")
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
            intent.putExtra("weight", weight)
            intent.putExtra("phonenumber", phonenumber)
            intent.putExtra("dateOfBirth", selectedDateOfBirth)
            intent.putExtra("educationID", getEducationID(selectedEducation!!))
            intent.putExtra("careerId", selectedCareerId ?: 0)
            intent.putExtra("career", selectedCareerName ?: "")
            intent.putExtra("home", home)
            intent.putExtra("province", selectedProvince)
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
            else -> -1
        }
    }

    private fun setupEducationButton(button: Button, education: String) {
        button.setOnClickListener {
            findViewById<Button>(R.id.buttonHighSchool).isSelected = false
            findViewById<Button>(R.id.buttonBachelor).isSelected = false
            findViewById<Button>(R.id.buttonMaster).isSelected = false
            findViewById<Button>(R.id.buttonPhd).isSelected = false

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
