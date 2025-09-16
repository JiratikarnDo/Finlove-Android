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
import org.json.JSONObject
import java.io.File

class RegisterActivity8 : AppCompatActivity() {

    private var selectedImageUri: Uri? = null
    private var isSubmitting = false  // <-- เพิ่มตัวนี้กันกดซ้ำ

    override fun onCreate(savedInstanceState: Bundle?) {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register8)

        val imageView = findViewById<ImageView>(R.id.imageView)
        val buttonSelectImage = findViewById<Button>(R.id.buttonSelectImage)
        val buttonUploadImage = findViewById<Button>(R.id.buttonUploadImage)

        // เลือกรูป
        buttonSelectImage.setOnClickListener {
            val pick = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
            startActivityForResult(pick, 100)
        }

        // อัปโหลด + สมัคร → ถ้าสำเร็จพาไปหน้า OTP
        buttonUploadImage.setOnClickListener {
            if (selectedImageUri == null) {
                Toast.makeText(this@RegisterActivity8, "กรุณาเลือกภาพ", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }
            if (isSubmitting) return@setOnClickListener

            // --- รับค่าจาก Intent ---
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
            val careerId = intent.getIntExtra("careerId", 0)
            val career = intent.getStringExtra("career")
            val home = intent.getStringExtra("home")
            val preferences = intent.getStringExtra("preferences")
            val province = intent.getStringExtra("province") ?: intent.getStringExtra("selectedProvince")
            val goalID = intent.getIntExtra("goalID", -1)
            val interestGenderID = intent.getIntExtra("interestGenderID", -1)

            if (educationID == -1 || email.isNullOrEmpty() || username.isNullOrEmpty()
                || password.isNullOrEmpty() || firstname.isNullOrEmpty() || lastname.isNullOrEmpty()
                || gender.isNullOrEmpty() || dateOfBirth.isNullOrEmpty()
                || goalID == -1 || interestGenderID == -1
            ) {
                Toast.makeText(this@RegisterActivity8, "ข้อมูลไม่ครบถ้วน", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // --- แปลง Uri → File ---
            val file = getFileFromUri(selectedImageUri!!)
            if (file == null) {
                Toast.makeText(this@RegisterActivity8, "ไม่สามารถอ่านไฟล์รูปได้", Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }

            // --- multipart แนบรูป + ฟอร์ม ---
            val requestFile = RequestBody.create("image/jpeg".toMediaTypeOrNull(), file)
            val imagePart = MultipartBody.Part.createFormData("imageFile", file.name, requestFile)

            val jsonBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("email", email!!)
                .addFormDataPart("username", username!!)
                .addFormDataPart("password", password!!)
                .addFormDataPart("firstname", firstname!!)
                .addFormDataPart("lastname", lastname!!)
                .addFormDataPart("nickname", nickname ?: "")
                .addFormDataPart("gender", gender!!)
                .addFormDataPart("height", height ?: "")
                .addFormDataPart("phonenumber", phonenumber ?: "")
                .addFormDataPart("dateOfBirth", dateOfBirth!!)
                .addFormDataPart("educationID", educationID.toString())
                .addFormDataPart("home", home ?: "")
                .addFormDataPart("preferences", preferences ?: "")
                .addFormDataPart("goalID", goalID.toString())
                .addFormDataPart("interestGenderID", interestGenderID.toString())
                .addFormDataPart("weight", (weight ?: "").trim())
                .addFormDataPart("status", career ?: "ไม่ระบุ")
                .addFormDataPart("career_id", careerId.toString())
                .addFormDataPart("province", province ?: "")
                .addPart(imagePart)
                .build()

            // --- ยิง register แล้วพาไปหน้า OTP ---
            isSubmitting = true
            buttonUploadImage.isEnabled = false

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
                        isSubmitting = false
                        buttonUploadImage.isEnabled = true

                        if (response.isSuccessful) {
                            Toast.makeText(this@RegisterActivity8, "ส่งรหัสแล้ว กรุณาตรวจอีเมล", Toast.LENGTH_SHORT).show()

                            val requestId = try {
                                JSONObject(bodyStr).optString("requestId", null)?.takeIf { it.isNotBlank() }
                            } catch (_: Exception) { null }

                            val i = Intent(this@RegisterActivity8, OtpVerifyActivity::class.java).apply {
                                putExtra("email", email)
                                if (requestId != null) putExtra("requestId", requestId)
                            }
                            startActivity(i)

                            // ถ้าอยากกันย้อนกลับมาส่งซ้ำ ให้เปิดบรรทัดนี้
                            // finish()
                        } else {
                            Toast.makeText(this@RegisterActivity8, "สมัครไม่สำเร็จ ($code): $bodyStr", Toast.LENGTH_LONG).show()
                        }
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        isSubmitting = false
                        buttonUploadImage.isEnabled = true
                        Toast.makeText(this@RegisterActivity8, "เกิดข้อผิดพลาดเครือข่าย: ${e.message}", Toast.LENGTH_LONG).show()
                    }
                } finally {
                    // ถ้าต้องการลบไฟล์ชั่วคราวหลังอัปโหลดเสร็จ:
                    // file.delete()
                }
            }
        } // <-- ปิด setOnClickListener
    } // <-- ปิด onCreate

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
            tempFile.outputStream().use { outputStream -> inputStream.copyTo(outputStream) }
            tempFile
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
