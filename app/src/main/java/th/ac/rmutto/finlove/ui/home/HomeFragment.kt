package th.ac.rmutto.finlove.ui.home

import android.app.AlertDialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.FormBody
import org.json.JSONArray
import th.ac.rmutto.finlove.R
import th.ac.rmutto.finlove.databinding.FragmentHomeBinding
import java.io.IOException
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import java.text.SimpleDateFormat
import java.util.*
import android.Manifest
import android.content.IntentSender
import android.location.Location
import com.google.android.gms.location.*
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.tasks.Task
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import androidx.activity.result.IntentSenderRequest
import th.ac.rmutto.finlove.utils.AnimationHelper


class HomeFragment : Fragment() {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!
    private var userID: Int = -1
    private var selectedUserID: Int = -1
    private val client = OkHttpClient()

    private var users = listOf<User>() // เก็บรายการผู้ใช้ทั้งหมด
    private var currentIndex = 0 // ตัวนับสำหรับผู้ใช้ปัจจุบัน

    private val gpsResolutionLauncher = registerForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            getCurrentLocation()
        } else {
            Toast.makeText(requireContext(), "กรุณาเปิด GPS เพื่อใช้งาน", Toast.LENGTH_SHORT).show()
        }
    }

    private lateinit var fusedLocationClient: FusedLocationProviderClient

    private val locationPermissionRequest =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
            if (permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true) {
                turnOnGPS()
            } else {
                Toast.makeText(requireContext(), "กรุณาอนุญาตการเข้าถึงตำแหน่ง", Toast.LENGTH_SHORT)
                    .show()
            }
        }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        val root: View = binding.root

        // กำหนดค่า fusedLocationClient ที่นี่เลย
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())

        // รับ userID ที่ถูกส่งมาจาก MainActivity
        userID = arguments?.getInt("userID", -1) ?: -1

        checkAndRequestLocationPermission()

        // กู้คืน currentIndex หากมีการบันทึกไว้
        if (savedInstanceState != null) {
            currentIndex = savedInstanceState.getInt("currentIndex", 0)
        }

        // ถ้า userID ถูกส่งมาแล้ว ให้ดึงข้อมูลผู้ใช้
        if (userID != -1) {
            fetchRecommendedUsers { fetchedUsers ->
                if (fetchedUsers.isNotEmpty()) {
                    users = fetchedUsers
                    displayUser(currentIndex) // แสดงผู้ใช้จาก currentIndex
                } else {
                    Toast.makeText(requireContext(), "ไม่พบผู้ใช้ที่แนะนำ", Toast.LENGTH_SHORT).show()
                }
            }
        }

        return root
    }

    // ฟังก์ชันบันทึกสถานะ currentIndex ก่อนที่ fragment จะถูกทำลาย
    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putInt("currentIndex", currentIndex) // บันทึก currentIndex
    }


    fun calculateAge(dateString: String?): Int {
        if (dateString.isNullOrEmpty()) {
            Log.d("HomeFragment", "dateString is null or empty")
            return -1
        }
        return try {
            val sdf = SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.ENGLISH)
            val birthDate = sdf.parse(dateString.trim())

            val today = Calendar.getInstance()
            val dob = Calendar.getInstance()
            dob.time = birthDate!!

            var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
            if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                age--
            }

            Log.d("HomeFragment", "Parsed birthDate: $birthDate")
            age
        } catch (e: Exception) {
            e.printStackTrace()
            Log.e("HomeFragment", "Failed to parse date: ${e.message}")
            -1
        }
    }

    private fun checkAndRequestLocationPermission() {
        locationPermissionRequest.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
    }

    private fun turnOnGPS() {
        val locationRequest = LocationRequest.create()
            .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)

        val builder = LocationSettingsRequest.Builder()
            .addLocationRequest(locationRequest)
            .setAlwaysShow(true)

        val settingsClient = LocationServices.getSettingsClient(requireActivity())
        val task = settingsClient.checkLocationSettings(builder.build())

        task.addOnSuccessListener {
            getCurrentLocation()
        }

        task.addOnFailureListener { exception ->
            if (exception is ResolvableApiException) {
                val intentSenderRequest = IntentSenderRequest.Builder(exception.resolution).build()
                gpsResolutionLauncher.launch(intentSenderRequest)
            }
        }
    }

    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            requireContext(),
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun sendLocationToServer(latitude: Double, longitude: Double) {
        val url = getString(R.string.root_url) + "/api_v2/update_location"  // สมมติ endpoint นี้
        val formBody = FormBody.Builder()
            .add("userID", userID.toString())
            .add("latitude", latitude.toString())
            .add("longitude", longitude.toString())
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to update location", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        Log.d("HomeFragment", "Location updated successfully")
                    } else {
                        Toast.makeText(requireContext(), "Error updating location: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }



    private fun getCurrentLocation() {
        if (!hasLocationPermission()) {
            checkAndRequestLocationPermission()
            return
        }

        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    val lat = location.latitude
                    val lng = location.longitude
                    Log.d("GPS", "📍 พิกัด Latitude: $lat, Longitude: $lng")
                    sendLocationToServer(lat, lng) // <-- เพิ่มตรงนี้ เพื่อส่งพิกัดไปเซิร์ฟเวอร์
                } else {
                    Toast.makeText(requireContext(), "ไม่สามารถดึงพิกัดได้", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener {
                Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการดึงตำแหน่ง", Toast.LENGTH_SHORT).show()
            }
    }



    // ฟังก์ชันแสดงผู้ใช้จากตำแหน่ง currentIndex
    private fun displayUser(index: Int) {
        if (index >= users.size) {
            Toast.makeText(requireContext(), "ไม่มีผู้ใช้อีกแล้ว", Toast.LENGTH_SHORT).show()
            return
        }

        val user = users[index]

        val userListLayout: LinearLayout = binding.userListLayout
        userListLayout.removeAllViews() // ลบรายการผู้ใช้ก่อนหน้าออก
        Log.d("ImageURL", "URL: ${user.profilePicture}")
        val userView = LayoutInflater.from(requireContext()).inflate(R.layout.item_user, userListLayout, false)

        // กำหนดข้อมูลผู้ใช้ใน View
        val nickname: TextView = userView.findViewById(R.id.textNickname)
        val profileImage: ImageView = userView.findViewById(R.id.imageProfile)
        val ageTextView: TextView = userView.findViewById(R.id.textAge)
        val verifiedIcon: ImageView = userView.findViewById(R.id.imageVerified) // ไอคอนเครื่องหมายถูก
        val age = calculateAge(user.dateBirth)
        Log.d("HomeFragment", "📌 User: ${user.nickname}, dateBirth: '${user.dateBirth}', age: $age")
        ageTextView.text = if (age >= 0) "$age ปี" else "ไม่ทราบอายุ"
        val likeButton: ImageButton = userView.findViewById(R.id.buttonLike)
        val dislikeButton: ImageButton = userView.findViewById(R.id.buttonDislike)


        nickname.text = user.nickname
        Glide.with(requireContext()).load(user.profilePicture).into(profileImage)

        // ตรวจสอบสถานะ verify และแสดงไอคอนเครื่องหมายถูกหาก verify == 1
        if (user.verify == 1) {
            verifiedIcon.visibility = View.VISIBLE
        } else {
            verifiedIcon.visibility = View.GONE
        }

        // เมื่อกดปุ่ม "Like"
        likeButton.setOnClickListener {
            AnimationHelper.animateButtonPressBounceRotate(it as ImageButton){
            likeUser(user.userID)
        }
        }

        // เมื่อกดปุ่ม "Dislike"
        dislikeButton.setOnClickListener {
            AnimationHelper.animateButtonPressBounceRotate(it as ImageButton) {
                    dislikeUser(user.userID)
            }
        }

        // เพิ่ม View ที่สร้างขึ้นใหม่ไปยัง LinearLayout
        userListLayout.addView(userView)
        // *** เรียกใช้ AnimationHelper สำหรับการแสดงการ์ดผู้ใช้ ***
    }


    // ฟังก์ชันแสดง Dialog สำหรับการรายงานผู้ใช้
    private fun showReportDialog(reportedID: Int) {
        val reportOptions = arrayOf("ก่อกวน/ปั่นป่วน", "ไม่ตอบสนอง", "ข้อมูลเท็จ")
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("เลือกประเภทการรายงาน")
        builder.setSingleChoiceItems(reportOptions, -1) { dialog, which ->
            val reportType = reportOptions[which]
            dialog.dismiss()
            confirmReport(reportedID, reportType)
        }
        builder.create().show()
    }

    // ยืนยันการรายงานผู้ใช้
    private fun confirmReport(reportedID: Int, reportType: String) {
        val builder = AlertDialog.Builder(requireContext())
        builder.setTitle("ยืนยันการรายงาน")
        builder.setMessage("คุณต้องการรายงานผู้ใช้ด้วยเหตุผล '$reportType' หรือไม่?")
        builder.setPositiveButton("ยืนยัน") { _, _ ->
            reportUser(reportedID, reportType)
        }
        builder.setNegativeButton("ยกเลิก", null)
        builder.create().show()
    }

    // ส่งข้อมูลรายงานผู้ใช้ไปยัง api_v2
    private fun reportUser(reportedID: Int, reportType: String) {
        val url = getString(R.string.root_url) + "/api_v2/report"
        val formBody = FormBody.Builder()
            .add("reporterID", userID.toString())
            .add("reportedID", reportedID.toString())
            .add("reportType", reportType)
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to report", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        Toast.makeText(requireContext(), "Report sent successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // ฟังก์ชันไปยังผู้ใช้คนถัดไป
    private fun nextUser() {
        currentIndex++
        if (currentIndex >= users.size) {
            currentIndex = 0 // วนกลับไปผู้ใช้คนแรก
        }
        displayUser(currentIndex)
    }

    // ฟังก์ชันสำหรับการกด "Like"
    private fun likeUser(likedID: Int) {
        val url = getString(R.string.root_url) + "/api_v2/like"
        val formBody = FormBody.Builder()
            .add("likerID", userID.toString())
            .add("likedID", likedID.toString())
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to like user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        checkMatch(likedID)
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // ฟังก์ชันตรวจสอบการ Match
    private fun checkMatch(likedID: Int) {
        val url = getString(R.string.root_url) + "/api_v2/check_match"
        val formBody = FormBody.Builder()
            .add("userID", userID.toString())
            .add("likedID", likedID.toString())
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to check match", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                requireActivity().runOnUiThread {
                    val responseBody = response.body?.string()
                    val isMatch = responseBody?.contains("\"match\":true") == true

                    if (isMatch) {
                        showMatchPopup()
                    } else {
                        nextUser()
                    }
                }
            }
        })
    }

    private fun showMatchPopup() {
        val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_match_popup, null)
        val dialog = android.app.Dialog(requireContext())
        dialog.setContentView(dialogView)

        val btnOk = dialogView.findViewById<Button>(R.id.btnOk)
        btnOk.setOnClickListener {
            dialog.dismiss()
            nextUser()
        }

        dialog.show()
    }


    // ฟังก์ชันสำหรับการกด "Dislike"
    private fun dislikeUser(dislikedID: Int) {
        val url = getString(R.string.root_url) + "/api_v2/dislike"
        val formBody = FormBody.Builder()
            .add("dislikerID", userID.toString())
            .add("dislikedID", dislikedID.toString())
            .build()

        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                requireActivity().runOnUiThread {
                    Toast.makeText(requireContext(), "Failed to dislike user", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                requireActivity().runOnUiThread {
                    if (response.isSuccessful) {
                        nextUser() // เรียก nextUser หลังจากการดำเนินการสำเร็จ
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    private fun fetchUserByID(targetUserID: Int, callback: (User?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/user/$targetUserID"
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val jsonObject = JSONArray(responseBody).getJSONObject(0) // หรือ JSONObject(responseBody) ถ้าเป็น Object
                    val user = User(
                        jsonObject.getInt("UserID"),
                        jsonObject.getString("nickname"),
                        jsonObject.getString("imageFile"),
                        jsonObject.optString("dateBirth", ""),
                        jsonObject.getInt("verify")
                    )
                    withContext(Dispatchers.Main) {
                        callback(user)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        callback(null)
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback(null)
                }
            }
        }
    }

    // ดึงข้อมูลผู้ใช้ที่แนะนำ
    private fun fetchRecommendedUsers(callback: (List<User>) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url2) + "/ai_v2/recommend/$userID"
            val request = Request.Builder().url(url).build()
            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d("API Response", responseBody ?: "No response")
                    val recommendedUsers = parseUsers(responseBody)
                    withContext(Dispatchers.Main) {
                        callback(recommendedUsers)
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "ไม่สามารถดึงข้อมูลผู้ใช้ได้ ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(requireContext(), "Error: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    // แปลงข้อมูล JSON ที่ได้จาก API เป็นรายการผู้ใช้
    private fun parseUsers(responseBody: String?): List<User> {
        val users = mutableListOf<User>()
        responseBody?.let {
            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val user = User(
                    jsonObject.getInt("UserID"),
                    jsonObject.getString("nickname"),
                    jsonObject.getString("imageFile"),
                    jsonObject.optString("dateBirth", ""),
                    jsonObject.getInt("verify")
                )
                users.add(user)
            }
        }
        return users
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

// คลาสสำหรับเก็บข้อมูลผู้ใช้
data class User(
    val userID: Int,
    val nickname: String,
    val profilePicture: String,
    val dateBirth: String ,// รูปแบบ yyyy-MM-dd
    val verify: Int // ฟิลด์สำหรับสถานะการยืนยัน
)

