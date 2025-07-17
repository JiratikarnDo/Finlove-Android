package th.ac.rmutto.finlove

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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

class RegisterActivity8 : AppCompatActivity() {
    private var selectedImageUri: Uri? = null

    override fun onCreate(savedInstanceState: Bundle?) {
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
            val phonenumber = intent.getStringExtra("phonenumber")
            val dateOfBirth = intent.getStringExtra("dateOfBirth")
            val educationID = intent.getIntExtra("educationID", -1)
            val home = intent.getStringExtra("home")
            val preferences = intent.getStringExtra("preferences")
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
                    .addPart(body) // เพิ่มไฟล์ภาพใน Body
                    .build()

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val client = OkHttpClient()
                        val rootUrl = getString(R.string.root_url) // ดึงค่า root_url จาก strings.xml
                        val url = "$rootUrl/api_v2/register8" // ประกอบ URL กับ path ที่ต้องการ
                        val request = Request.Builder()
                            .url(url)
                            .post(jsonBody)
                            .build()

                        val response = client.newCall(request).execute()

                        withContext(Dispatchers.Main) {
                            if (response.isSuccessful) {
                                Toast.makeText(this@RegisterActivity8, "ข้อมูลถูกบันทึกแล้ว", Toast.LENGTH_LONG).show()

                                // ดีเลย์ 1 วินาทีก่อนเปลี่ยนหน้า
                                kotlinx.coroutines.delay(500)

                                val intent = Intent(this@RegisterActivity8, FirstPageActivity::class.java)
                                intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                                startActivity(intent)
                            } else {
                                Toast.makeText(this@RegisterActivity8, "ไม่สามารถเชื่อมต่อกับเซิร์ฟเวอร์ได้", Toast.LENGTH_LONG).show()
                            }
                        }

                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(this@RegisterActivity8, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
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
