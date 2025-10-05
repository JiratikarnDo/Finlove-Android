package th.ac.rmutto.finlove

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.asRequestBody
import org.json.JSONObject
import th.ac.rmutto.finlove.databinding.ActivityAddphotoBinding
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AlertDialog  // <-- เพิ่มบรรทัดนี้

class AddphotoActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAddphotoBinding
    private val CAMERA_REQUEST = 2
    private val client = OkHttpClient.Builder().connectTimeout(10, TimeUnit.SECONDS).build()
    private var userID: Int = -1  // รับ userID ที่ส่งมาจาก ProfileFragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityAddphotoBinding.inflate(layoutInflater)
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.addphoto)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // รับ userID จาก Intent
        userID = intent.getIntExtra("userID", -1)

        checkPermissions()

        // เมื่อคลิกที่ปุ่ม imageView จะเรียกกล้องโดยตรง
        binding.imageView.setOnClickListener {
            openCamera()
        }

        binding.confirmButton.setOnClickListener {
            val imageUri = (binding.imageView.tag as? Uri) ?: return@setOnClickListener
            sendImageForVerification(imageUri)
        }
    }

    // ฟังก์ชันเพื่อเปิดกล้องโดยตรง
    private fun openCamera() {
        val cameraIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        startActivityForResult(cameraIntent, CAMERA_REQUEST)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.CAMERA, Manifest.permission.READ_EXTERNAL_STORAGE), 101)
        }
    }

    private fun onNext(selectedImageUri: Uri) {
        binding.confirmButton.visibility = View.VISIBLE
        binding.imageView.tag = selectedImageUri
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && requestCode == CAMERA_REQUEST) {
            val photo: Bitmap = data?.extras?.get("data") as Bitmap
            binding.imageViewshow.setImageBitmap(photo)
            binding.imageView.visibility = View.GONE
            binding.textViewFile.text = "Captured Image"
            binding.textViewFile.visibility = View.VISIBLE
            binding.textView.visibility = View.GONE
            val placeholderUri = saveImageToExternalStorage(photo)
            onNext(placeholderUri)
        }
    }

    private fun saveImageToExternalStorage(bitmap: Bitmap): Uri {
        val imagesFolder = File(getExternalFilesDir(Environment.DIRECTORY_PICTURES), "YourImages")
        if (!imagesFolder.exists()) {
            imagesFolder.mkdirs()
        }

        val file = File(imagesFolder, "${System.currentTimeMillis()}.jpg")
        val outputStream = FileOutputStream(file)
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
        outputStream.flush()
        outputStream.close()
        return Uri.fromFile(file)
    }
    // ✅ เพิ่ม: ฟังก์ชันแสดง Popup มาตรฐาน
    private fun showResultDialog(title: String, message: String, onOk: (() -> Unit)? = null) {
        if (isFinishing) return
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("ตกลง") { dlg, _ ->
                dlg.dismiss()
                onOk?.invoke()
            }
            .setCancelable(false)
            .show()
    }

    // ✅ เพิ่ม: ฟังก์ชันแสดง Popup สำหรับข้อผิดพลาด
    private fun showErrorDialog(message: String, onOk: (() -> Unit)? = null) {
        showResultDialog("แจ้งเตือน", message, onOk)
    }
    // ✅ เพิ่ม: รวม logic แปลงข้อความจาก API → ภาษาผู้ใช้ (ไม่โชว์คำว่า SPOOF/score)
    private fun makeFriendlyMessage(isLive: Boolean, detail: String?): Pair<String, String> {
        val d = (detail ?: "").lowercase()
        val noFace = d.contains("ไม่พบใบหน้า") || d.contains("no face")
        return when {
            isLive -> "ยืนยันสำเร็จ" to "ยืนยันตัวตนเรียบร้อย"
            noFace -> "ไม่พบใบหน้า" to "กรุณามองกล้องโดยตรง จัดใบหน้าให้อยู่กึ่งกลาง และถ่ายในที่ที่มีแสงเพียงพอ"
            else   -> "ยืนยันไม่สำเร็จ" to "กรุณาลองใหม่ โดยถือเครื่องให้นิ่ง ไม่ใส่หน้ากาก/หมวก/แว่นสะท้อนแสง และถ่ายในที่สว่างเพียงพอ"
        }
    }

    private fun sendImageForVerification(imageUri: Uri) {
        // ถ้า root_url3 คือ http://<IP>:8000
        val url = getString(R.string.root_url4) + "/api/v1/liveness_check"

        val imagePath = imageUri.path ?: ""
        val imageFile = File(imagePath)
        if (!imageFile.exists()) {
            showErrorDialog("ไม่พบไฟล์ภาพที่: $imagePath")
            return
        }

        // ⬅️ เปลี่ยนชื่อฟิลด์เป็น "file" ตาม FastAPI
        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart(
                "file",
                imageFile.name,
                imageFile.asRequestBody("image/jpeg".toMediaTypeOrNull())
            )
            .addFormDataPart("user_id", userID.toString())   // ✅ เพิ่มบรรทัดนี้
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                val bodyText = response.body?.string().orEmpty()

                if (response.isSuccessful) {
                    val json = JSONObject(bodyText)
                    val isLive = json.optBoolean("is_live", false)
                    // 🧹 ไม่ใช้งาน score/ข้อความโปรแกรมมิง
                    // val score  = json.optDouble("score", Double.NaN)
                    // val msg    = json.optString("message")
                    val detail = json.optString("detail")

                    // ✅ เปลี่ยน: ใช้ภาษาผู้ใช้เสมอ
                    val (title, message) = makeFriendlyMessage(isLive, detail)

                    runOnUiThread {
                        // ✅ เปลี่ยน: Popup + กดตกลงค่อยกลับหน้า Main
                        showResultDialog(title, message) {
                            val intent = Intent(this@AddphotoActivity, MainActivity::class.java)
                            intent.putExtra("userID", userID)
                            intent.putExtra("navigateToProfile", true)
                            startActivity(intent)
                            finish()
                        }
                    }
                } else {
                    // ✅ เปลี่ยน: ไม่โชว์โค้ด/ดีเทลเทคนิคกับผู้ใช้
                    runOnUiThread {
                        showErrorDialog("ไม่สามารถยืนยันตัวตนได้ในขณะนี้ กรุณาลองใหม่อีกครั้ง")
                    }
                }
            }
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    // ❌ เดิม: Toast → ✅ ใหม่: Popup
                    showErrorDialog("เชื่อมต่อกับเซิร์ฟเวอร์ไม่ได้ กรุณาตรวจสอบอินเทอร์เน็ต/เครือข่าย แล้วลองใหม่อีกครั้ง")
                }
            }
        })
    }
}
