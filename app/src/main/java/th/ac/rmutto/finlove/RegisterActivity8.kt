package th.ac.rmutto.finlove

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import java.io.File
import android.view.LayoutInflater
import android.widget.EditText
import androidx.appcompat.app.AlertDialog
import okhttp3.FormBody

class RegisterActivity8 : AppCompatActivity() {
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register8)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonUploadImage = findViewById<Button>(R.id.buttonUploadImage)

        buttonSelectImage.setOnClickListener {
            val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(intent, 100)
        }

        buttonUploadImage.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this@RegisterActivity8, "กรุณาเลือกภาพ", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // รับข้อมูลทั้งหมดจาก Intent
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
            val dateOfBirth = intent.getStringExtra("dateOfBirth")
            val educationID = intent.getIntExtra("educationID", -1)
            val careerId = intent.getIntExtra("careerId", 0)       // << เพิ่มบรรทัดนี้
            val career   = intent.getStringExtra("career")         // ชื่อ (ถ้าต้องใช้แสดงผล)
            val home = intent.getStringExtra("home")
            val preferences = intent.getStringExtra("preferences")
            val province = intent.getStringExtra("province")
                ?: intent.getStringExtra("selectedProvince")
            val goalID = intent.getIntExtra("goalID", -1) // ดึง goalID จาก Intent
            val interestGenderID = intent.getIntExtra("interestGenderID", -1) // ดึง interestGenderID

            if (educationID == -1 || email.isNullOrEmpty() || username.isNullOrEmpty() || goalID == -1 || interestGenderID == -1) {
                Toast.makeText(this@RegisterActivity8, "ข้อมูลไม่ครบถ้วน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            val file = getFileFromUri(selectedImageUri!!)
            if (file != null) {
                val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
                val body = MultipartBody.Part.createFormData("imageFile", file.name, requestFile)

                // สร้าง JSON Object สำหรับข้อมูลทั้งหมด
                val jsonBody = MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("email", email!!)
                    .addFormDataPart("username", username!!)
                    .addFormDataPart("password", password!!)
                    .addFormDataPart("firstname", firstname!!)
                    .addFormDataPart("lastname", lastname!!)
                    .addFormDataPart("nickname", nickname!!)
                    .addFormDataPart("gender", gender!!)
                    .addFormDataPart("height", height!!)
                    .addFormDataPart("phonenumber", phonenumber!!)
                    .addFormDataPart("dateOfBirth", dateOfBirth!!)
                    .addFormDataPart("educationID", educationID.toString())
                    .addFormDataPart("home", home!!)
                    .addFormDataPart("preferences", preferences!!)
                    .addFormDataPart("goalID", goalID.toString())
                    .addFormDataPart("interestGenderID", interestGenderID.toString()) // เพิ่ม interestGenderID
                    // ----- ฟิลด์ใหม่ -----
                    .addFormDataPart("weight",   (weight ?: "").trim())
                    .addFormDataPart("status",   career ?: "ไม่ระบุ")  // ถ้า backend ไม่ใช้ status จะลบทิ้งก็ได้
                    .addFormDataPart("career_id", careerId.toString())  // << เพิ่มบรรทัดนี้ สำคัญ
                    .addFormDataPart("province", province ?: "")
                    .addPart(body)
                    .build()

// เปิด Popup OTP จากไฟล์ dialog_otp.xml
                val view = LayoutInflater.from(this).inflate(R.layout.dialog_otp, null, false)
                val edtOtp = view.findViewById<EditText>(R.id.edtOtp)
                val btnVerify = view.findViewById<Button>(R.id.btnVerify)
// ถ้ามีปุ่มส่งใหม่ใน XML
                val btnResend = view.findViewById<Button>(R.id.btnResend)

                val dialog = AlertDialog.Builder(this)
                    .setView(view)
                    .setCancelable(false)
                    .create()

// 1) ตอนเปิด dialog → ยิง register เพื่อ "insert + ส่ง OTP"
                dialog.setOnShowListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = OkHttpClient()
                            val rootUrl = getString(R.string.root_url)
                            val url = "$rootUrl/api_v2/register8"
                            val request = Request.Builder().url(url).post(jsonBody).build()
                            val response = client.newCall(request).execute()
                            val code = response.code
                            val bodyStr = response.body?.string() ?: ""

                            withContext(Dispatchers.Main) {
                                if (response.isSuccessful) {
                                    Toast.makeText(this@RegisterActivity8, "ส่งรหัสแล้ว กรุณาตรวจอีเมล", Toast.LENGTH_SHORT).show()
                                    // ปล่อยให้ผู้ใช้กรอก OTP ต่อได้ตามปกติ (อย่า dismiss ตรงนี้)
                                } else {
                                    // โชว์รายละเอียดให้รู้ชัด ๆ ว่าพลาดตรงไหน
                                    Toast.makeText(
                                        this@RegisterActivity8,
                                        "สมัครไม่สำเร็จ ($code): $bodyStr",
                                        Toast.LENGTH_LONG
                                    ).show()

                                    // เคสที่แก้ในแอปไม่ได้ (เช่น 409/404) ค่อยปิด dialog
                                    if (code == 409 || code == 404) {
                                        // 409 = อีเมล/ชื่อผู้ใช้ซ้ำ, 404 = เพศไม่พบ
                                        // คุณอาจจะเปิดหน้าก่อนหน้ากลับไปแก้ค่า หรือปิด popup เฉย ๆ
                                        dialog.dismiss()
                                    }
                                    // สำหรับ 400 (ฟิลด์ไม่ครบ), 413 (ไฟล์ใหญ่), 422 ฯลฯ
                                    // คง dialog ไว้เพื่อให้ผู้ใช้แก้ได้ (หรือคุณจะปิดก็ได้ถ้าต้องการ)
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    this@RegisterActivity8,
                                    "เกิดข้อผิดพลาดเครือข่าย: ${e.message}",
                                    Toast.LENGTH_LONG
                                ).show()
                                dialog.dismiss()
                            }
                        }
                    }
                }

// 2) ผู้ใช้กด “ยืนยัน” → เรียก verify-otp
                btnVerify.setOnClickListener {
                    val code = edtOtp.text?.toString()?.trim().orEmpty()
                    if (code.length != 6) {
                        Toast.makeText(this, "กรุณาใส่รหัสให้ครบ 6 หลัก", Toast.LENGTH_SHORT).show()
                        return@setOnClickListener
                    }

                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = OkHttpClient()
                            val rootUrl = getString(R.string.root_url)
                            val vUrl = "$rootUrl/auth/verify-otp"   // ถ้า path ของคุณต่างไป แก้ตรงนี้
                            val form = FormBody.Builder()
                                .add("email", email!!)   // ใช้ email เดิมที่อ่านมาด้านบน
                                .add("otp", code)
                                .build()
                            val req = Request.Builder().url(vUrl).post(form).build()
                            val res = client.newCall(req).execute()

                            withContext(Dispatchers.Main) {
                                if (res.isSuccessful) {
                                    Toast.makeText(this@RegisterActivity8, "ยืนยันสำเร็จ", Toast.LENGTH_SHORT).show()
                                    dialog.dismiss()

                                    // ไปหน้าแรกหลังยืนยันสำเร็จ
                                    val intent = Intent(this@RegisterActivity8, FirstPageActivity::class.java)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                    startActivity(intent)
                                } else {
                                    Toast.makeText(this@RegisterActivity8, "รหัสไม่ถูกต้องหรือหมดอายุ", Toast.LENGTH_LONG).show()
                                }
                            }
                        } catch (e: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegisterActivity8, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

// (ตัวเลือก) 3) ปุ่ม “ส่งใหม่” ถ้าคุณมี endpoint แยก /auth/send-otp
                btnResend?.setOnClickListener {
                    CoroutineScope(Dispatchers.IO).launch {
                        try {
                            val client = OkHttpClient()
                            val rootUrl = getString(R.string.root_url)
                            val rUrl = "$rootUrl/auth/send-otp"  // ปรับตามของคุณ
                            val form = FormBody.Builder().add("email", email!!).build()
                            val req = Request.Builder().url(rUrl).post(form).build()
                            client.newCall(req).execute()
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegisterActivity8, "ส่งรหัสใหม่แล้ว", Toast.LENGTH_SHORT).show()
                            }
                        } catch (_: Exception) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(this@RegisterActivity8, "ส่งรหัสใหม่ไม่สำเร็จ", Toast.LENGTH_LONG).show()
                            }
                        }
                    }
                }

                dialog.show()
            }
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 100 && resultCode == RESULT_OK) {
            selectedImageUri = data?.data
            val imageView = findViewById<ImageView>(R.id.imageView)
            imageView.setImageURI(selectedImageUri)
        }
    }

    private fun getFileFromUri(uri: Uri): File? {
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File.createTempFile("image", ".jpg", cacheDir)
            tempFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
