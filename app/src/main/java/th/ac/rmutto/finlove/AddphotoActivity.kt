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

    private fun sendImageForVerification(imageUri: Uri) {
        val url = getString(R.string.root_url3) + "/ai/predict"  // ใช้ URL จาก resource string

        val imagePath = imageUri.path ?: ""
        val imageFile = File(imagePath)

        if (!imageFile.exists()) {
            Toast.makeText(this, "File not found at: $imagePath", Toast.LENGTH_SHORT).show()
            return
        }

        val requestBody = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("image", imageFile.name, imageFile.asRequestBody("image/*".toMediaTypeOrNull()))
            .addFormDataPart("UserID", userID.toString())  // ใช้ "UserID" ให้ตรงกับฝั่ง server
            .build()

        val request = Request.Builder().url(url).post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonResponse = JSONObject(response.body?.string())
                    val isHuman = jsonResponse.getBoolean("is_human")
                    val confidenceScore = jsonResponse.getDouble("confidence_score")

                    runOnUiThread {
                        Toast.makeText(
                            this@AddphotoActivity,
                            if (isHuman) "ยืนยันตัวตนเรียบร้อย" else "ยืนยันตัวตนไม่สำเร็จกรุณาลองใหม่อีกครั้ง",
                            Toast.LENGTH_SHORT
                        ).show()

                        val intent = Intent(this@AddphotoActivity, MainActivity::class.java)
                        intent.putExtra("userID", userID)
                        intent.putExtra("navigateToProfile", true)
                        startActivity(intent)
                        finish()
                    }
                } else {
                    runOnUiThread {
                        Toast.makeText(this@AddphotoActivity, "ยืนยันตัวตนไม่สำเร็จกรุณาลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
                    }
                }
            }

            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread {
                    Toast.makeText(this@AddphotoActivity, "Network request failed", Toast.LENGTH_SHORT).show()
                }
            }
        })
    }
}
