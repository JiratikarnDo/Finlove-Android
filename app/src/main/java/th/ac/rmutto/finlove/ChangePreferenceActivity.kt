package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody

class ChangePreferenceActivity : AppCompatActivity() {

    private lateinit var selectedPreferences: MutableList<Int> // เก็บ PreferenceID ที่เลือก

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_preference)

        selectedPreferences = mutableListOf()

        // Find all buttons
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
        val buttonSave = findViewById<ImageButton>(R.id.buttonsave)

        // Set up buttons with preferences
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

        // Save button click listener
        buttonSave.setOnClickListener {
            if (selectedPreferences.size < 1) {
                Toast.makeText(this, "กรุณาเลือกอย่างน้อยหนึ่งตัวเลือก", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val preferencesString = selectedPreferences.joinToString(",") // แปลงเป็น comma-separated ID string
            Log.d("ChangePreferenceActivity", "Selected preferences: $preferencesString")

            val userID = intent.getIntExtra("userID", -1)
            if (userID == -1) {
                Toast.makeText(this@ChangePreferenceActivity, "ไม่พบ userID", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            Log.d("ChangePreferenceActivity", "UserID: $userID")

            val url = getString(R.string.root_url) + "/api_v2/user/update_preferences/$userID"
            Log.d("ChangePreferenceActivity", "API URL: $url")

            val formBody: RequestBody = FormBody.Builder()
                .add("preferences", preferencesString)
                .build()

            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val request: Request = Request.Builder()
                        .url(url)
                        .post(formBody)
                        .build()

                    val response = OkHttpClient().newCall(request).execute()

                    withContext(Dispatchers.Main) {
                        if (response.isSuccessful) {
                            Log.d("ChangePreferenceActivity", "Response successful")
                            // ส่งข้อมูลกลับไปที่ ProfileActivity หลังจากบันทึก Preferences
                            val resultIntent = Intent()
                            resultIntent.putExtra("preferences", preferencesString)
                            setResult(RESULT_OK, resultIntent)
                            finish() // กลับไปที่หน้า ProfileActivity
                        } else {
                            Log.e("ChangePreferenceActivity", "Response error: ${response.code}")
                            Toast.makeText(this@ChangePreferenceActivity, "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Log.e("ChangePreferenceActivity", "Error: ${e.message}")
                        Toast.makeText(this@ChangePreferenceActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    // ฟังก์ชันสำหรับแปลง preference เป็น preferenceID ที่ถูกต้อง
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

    private fun setupButton(button: Button, preference: String) {
        button.setOnClickListener {
            val preferenceID = getPreferenceID(preference)

            if (preferenceID == -1) {
                Toast.makeText(this, "ไม่พบ preference ที่ถูกต้อง", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            if (button.isSelected) {
                // เช็คว่าจำนวนที่เหลือจะเป็น 0 หรือไม่
                if (selectedPreferences.size <= 1) {
                    Toast.makeText(this, "กรุณาเลือกอย่างน้อยหนึ่งตัวเลือก", Toast.LENGTH_LONG).show()
                } else {
                    button.isSelected = false
                    selectedPreferences.remove(preferenceID) // เอา PreferenceID ออกจาก List
                }
            } else {
                // เช็คว่ามีการเลือกครบ 3 อย่างหรือยัง
                if (selectedPreferences.size >= 3) {
                    Toast.makeText(this, "คุณสามารถเลือกได้สูงสุด 3 ตัวเลือก", Toast.LENGTH_LONG).show()
                } else {
                    button.isSelected = true
                    selectedPreferences.add(preferenceID) // เพิ่ม PreferenceID ลงใน List
                }
            }
        }
    }
}
