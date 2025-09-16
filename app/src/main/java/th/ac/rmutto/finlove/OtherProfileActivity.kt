package th.ac.rmutto.finlove

import android.app.AlertDialog
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.io.IOException
import androidx.lifecycle.lifecycleScope
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowCompat

class OtherProfileActivity : AppCompatActivity() {

    private lateinit var profileImageView: ImageView
    private lateinit var firstNameTextView: TextView
    private lateinit var lastNameTextView: TextView
    private lateinit var nicknameTextView: TextView
    private lateinit var genderTextView: TextView
    private lateinit var weightTextView: TextView
    private lateinit var heightTextView: TextView
    private lateinit var provinceTextView: TextView
    private lateinit var careerTextView: TextView
    private lateinit var preferencesContainer: LinearLayout
    private lateinit var reportButton: Button
    private lateinit var verifiedIcon: ImageView
    private val client = OkHttpClient()
    private var userID: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_other_profile)

        // Initialize views
        profileImageView = findViewById(R.id.profile_image)
        firstNameTextView = findViewById(R.id.textViewFirstName)
        lastNameTextView = findViewById(R.id.textViewLastName)
        nicknameTextView = findViewById(R.id.textViewNickname)
        weightTextView = findViewById(R.id.textViewWeight)
        heightTextView = findViewById(R.id.textViewHeight)
        provinceTextView = findViewById(R.id.textViewProvince)
        careerTextView = findViewById(R.id.textViewCareer)
        genderTextView = findViewById(R.id.textViewGender)
        preferencesContainer = findViewById(R.id.preferenceContainer)
        reportButton = findViewById(R.id.buttonReportUser)
        verifiedIcon = findViewById(R.id.verifiedIcon)

        // Get the userID from intent
        userID = intent.getIntExtra("userID", -1)

        // If userID is valid, fetch user profile
        if (userID != -1) {
            fetchUserProfile(userID)
        } else {
            Toast.makeText(this, "ไม่พบข้อมูลผู้ใช้", Toast.LENGTH_SHORT).show()
        }

        // Report button listener
        reportButton.setOnClickListener {
            showReportDialog(userID)
        }
    }

    // Function to fetch user profile using api_v2
    private fun fetchUserProfile(userID: Int) {
        GlobalScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/profile/$userID"
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONObject(responseBody ?: "{}")
                    val gender = jsonObject.optString("gender")

                    // แปลงค่าจากฐานข้อมูล (male, female, other) เป็นคำที่เข้าใจได้
                    val displayGender = when (gender.toLowerCase()) {
                        "male" -> "ชาย"
                        "female" -> "หญิง"
                        "other" -> "อื่นๆ"
                        else -> "ไม่ระบุ"
                    }

                    // ✅ เพิ่มบรรทัดนี้เพื่อรองรับทั้งกรณีมี "data" และไม่มี
                    val obj = if (jsonObject.has("data") && jsonObject.opt("data") is JSONObject)
                        jsonObject.getJSONObject("data") else jsonObject
                    // Extract data from JSON response
                    val firstName = jsonObject.optString("firstname")
                    val lastName = jsonObject.optString("lastname")
                    val nickname = jsonObject.optString("nickname")
                    val height = obj.optDouble("height", Double.NaN)     // ✅ แก้จาก jsonObject → obj
                    val weight = obj.optDouble("weight", Double.NaN)
                    val province = obj.optString("province", "")  // ดึงข้อมูลจังหวัดจาก JSON
                    val careerId    = if (obj.has("careerId") && !obj.isNull("careerId")) obj.optInt("careerId") else null
                    val careerName  = obj.optString("careerName", "")
                    val preferences = jsonObject.optString("preferences")
                    var profileImage = jsonObject.optString("imageFile")
                    val isVerified = jsonObject.optInt("verify", 0) == 1

                    // เติม URL แบบเต็มถ้า profileImage ไม่ได้เป็น URL แบบเต็ม
                    if (!profileImage.startsWith("http")) {
                        profileImage = getString(R.string.root_url) + "/api_v2/user/image/" + profileImage
                    }

                    Log.d("API Response", "Province: $province")
                    Log.d("API Response", "career: $careerId")
                    // Log the profileImage URL to ensure it's correct
                    Log.d("OtherProfileActivity", "Profile Image URL: $profileImage")

                    // Update UI on the main thread
                    withContext(Dispatchers.Main) {
                        // province (แก้เช็คให้ถูก: optString ไม่มีวันเป็น null)
                        val displayProvince = if (province.isBlank()) "ไม่ทราบจังหวัด" else province
                        provinceTextView.text = "จังหวัด: $displayProvince"

                        // ✅ career
                        val displayCareer = when {
                            careerName.isNotBlank() -> careerName
                            else                    -> "ไม่ทราบอาชีพ"
                        }
                        careerTextView.text = "อาชีพ: $displayCareer"
                        // อัปเดต TextView
                        firstNameTextView.text = "ชื่อจริง: $firstName"
                        lastNameTextView.text = "นามสกุล: $lastName"
                        nicknameTextView.text = "ชื่อเล่น: $nickname"
                        genderTextView.text = "เพศ: $displayGender"

                        // ✅ เติมการแสดงผลส่วนสูง/น้ำหนักแบบกัน NaN
                        heightTextView.text = "ส่วนสูง: " + if (height.isNaN()) "—" else "${height.toInt()} ซม."
                        weightTextView.text = "น้ำหนัก: " + if (weight.isNaN()) "—" else "${weight.toInt()} กก."

                        // Load profile image using Glide
                        Glide.with(this@OtherProfileActivity)
                            .load(profileImage)
                            .placeholder(R.drawable.img_1)  // แสดงภาพ placeholder ขณะกำลังโหลด
                            .error(R.drawable.error)        // แสดงภาพ error ถ้าโหลดไม่ได้
                            .into(profileImageView)

                        // Show verified icon if user is verified
                        verifiedIcon.visibility = if (isVerified) View.VISIBLE else View.GONE

                        // Update preferences
                        updateUserPreferences(preferences)

                        // Set nickname to the toolbar dynamically
                        val toolbarTitle = findViewById<TextView>(R.id.toolbarTitle)
                        toolbarTitle.text = nickname
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@OtherProfileActivity, "ไม่สามารถดึงข้อมูลผู้ใช้ได้", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@OtherProfileActivity, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
                    Log.e("OtherProfileActivity", "Error fetching profile", e)
                }
            }
        }
    }

    // Show the report dialog
    private fun showReportDialog(reportedID: Int) {
        val reportOptions = arrayOf("ก่อกวน/ปั่นป่วน", "ไม่ตอบสนอง", "ข้อมูลเท็จ")
        val builder = AlertDialog.Builder(this)
        builder.setTitle("เลือกประเภทการรายงาน")
        builder.setSingleChoiceItems(reportOptions, -1) { dialog, which ->
            val reportType = reportOptions[which]
            dialog.dismiss()
            confirmReport(reportedID, reportType)
        }
        builder.create().show()
    }

    // Confirm the report action
    private fun confirmReport(reportedID: Int, reportType: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle("ยืนยันการรายงาน")
        builder.setMessage("คุณต้องการรายงานผู้ใช้ด้วยเหตุผล '$reportType' หรือไม่?")
        builder.setPositiveButton("ยืนยัน") { _, _ ->
            reportUser(reportedID, reportType)
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.create().show()
    }

    // Report user to the server
    private fun reportUser(reportedID: Int, reportType: String) {
        val url = getString(R.string.root_url) + "/api_v2/report"
        val formBody = FormBody.Builder()
            .add("reporterID", userID.toString())
            .add("reportedID", reportedID.toString())
            .add("reportType", reportType)
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@OtherProfileActivity, "Failed to report", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(this@OtherProfileActivity, "Report sent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(this@OtherProfileActivity, "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun updateUserPreferences(preferences: String?) {
        preferencesContainer.removeAllViews()

        val preferencesArray = preferences?.split(",") ?: listOf()
        for (preference in preferencesArray) {
            val preferenceTextView = TextView(this)
            preferenceTextView.text = preference
            preferenceTextView.setBackgroundResource(R.drawable.show_preference)
            preferenceTextView.setPadding(16, 16, 16, 16)
            preferenceTextView.textSize = 14f
            preferenceTextView.setTypeface(null, android.graphics.Typeface.BOLD)
            preferenceTextView.gravity = Gravity.CENTER
            preferenceTextView.setTextColor(resources.getColor(R.color.white))

            val layoutParams = LinearLayout.LayoutParams(165, 100)
            layoutParams.setMargins(16, 16, 16, 16)
            preferenceTextView.layoutParams = layoutParams

            preferencesContainer.addView(preferenceTextView)
        }
    }
}
