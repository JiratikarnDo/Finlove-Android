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
import okhttp3.Call
import okhttp3.Callback
import com.google.gson.Gson
import okhttp3.Response
import org.json.JSONObject
import com.bumptech.glide.load.engine.DiskCacheStrategy



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
        selectedUserID = arguments?.getInt("selectedUserID", -1) ?: -1
        Log.d("HomeFragment", "selectedUserID = $selectedUserID")

        checkAndRequestLocationPermission()
        recentlyDisliked.clear()

        // กู้คืน currentIndex หากมีการบันทึกไว้
        if (selectedUserID != -1) {
            // ถ้ามี selectedUserID แสดงว่าเข้ามาจาก WhoLikeFragment
            fetchUserByID(selectedUserID) { user ->
                if (user != null) {
                    users = listOf(user)  // แสดงเฉพาะคนนี้
                    currentIndex = 0
                    displayUser(currentIndex)
                } else {
                    Toast.makeText(requireContext(), "ไม่พบข้อมูลผู้ใช้ที่เลือก", Toast.LENGTH_SHORT).show()
                }
            }
        } else {
            // ถ้าไม่มี selectedUserID แสดงรายชื่อผู้ใช้แนะนำตามปกติ
            if (userID != -1) {
                fetchRecommendedUsers { fetchedUsers ->
                    if (fetchedUsers.isNotEmpty()) {
                        users = fetchedUsers
                        displayUser(currentIndex)
                    } else {
                        Toast.makeText(requireContext(), "ไม่พบผู้ใช้ที่แนะนำ", Toast.LENGTH_SHORT).show()
                    }
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
            Log.d("HomeFragment", "⛔ dateString is null or empty")
            return -1
        }

        val patterns = listOf(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            "EEE, dd MMM yyyy HH:mm:ss z"
        )

        var birthDate: Date? = null

        for (pattern in patterns) {
            try {
                val sdf = SimpleDateFormat(
                    pattern,
                    if (pattern == "EEE, dd MMM yyyy HH:mm:ss z") Locale.ENGLISH else Locale.getDefault()
                )
                if (pattern.contains("Z")) {
                    sdf.timeZone = TimeZone.getTimeZone("UTC")
                }
                birthDate = sdf.parse(dateString.trim())
                if (birthDate != null) break
            } catch (e: Exception) {
                // ลอง pattern ถัดไป
            }
        }

        if (birthDate == null) {
            Log.e("HomeFragment", "❌ Failed to parse date: ไม่ตรงกับทุก pattern")
            return -1
        }

        val today = Calendar.getInstance()
        val dob = Calendar.getInstance()
        dob.time = birthDate

        var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
        if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
            age--
        }

        Log.d("HomeFragment", "✅ Parsed birthDate: $birthDate")
        return age
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
        Log.d("sendLocationToServer", "Sending location: userID=$userID, lat=$latitude, lng=$longitude")
        val url = getString(R.string.root_url) + "/api_v2/add-location"
        val formBody = FormBody.Builder()
            .add("latitude", latitude.toString())
            .add("longitude", longitude.toString())
            .build()

        // ดึง token จาก SharedPreferences
        val sharedPref = requireContext().getSharedPreferences("FinLovePrefs", android.content.Context.MODE_PRIVATE)
        val token = sharedPref.getString("jwt_token", "") ?: ""

        val request = Request.Builder()
            .url(url)
            .post(formBody)
            .addHeader("Authorization", "Bearer $token")
            .build()

        client.newCall(request).enqueue(object : okhttp3.Callback {
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

    private var myLat: Double? = null
    private var myLng: Double? = null

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
                    // เพิ่มตรงนี้!
                    myLat = lat
                    myLng = lng
                    // -----
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
        if (index >= users.size || users.isEmpty()) {
            binding.userListLayout.removeAllViews() // <-- เพิ่มบรรทัดนี้!
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
        val labellist: TextView = userView.findViewById((R.id.labellist))


        // ดึงขนาดจริงของ ImageView (ถ้ายังไม่ได้ layout ให้ใช้ fallback เป็นขนาดหน้าจอ)
        val width = if (profileImage.width > 0) profileImage.width else resources.displayMetrics.widthPixels
        val height = if (profileImage.height > 0) profileImage.height else resources.displayMetrics.heightPixels


        nickname.text = user.nickname
        // ปรับ performance ให้โหลดภาพไวขึ้น
        val thumbRequest = Glide.with(requireContext())
            .load(user.profilePicture)
            .override(width / 5, height / 5) // ปรับขนาด thumbnail ให้เล็กลง

        Glide.with(requireContext())
            .load(user.profilePicture)
            .override(width / 2, height / 2)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
            .thumbnail(thumbRequest)
            .into(profileImage)


        // ตรวจสอบสถานะ verify และแสดงไอคอนเครื่องหมายถูกหาก verify == 1
        if (user.verify == 1) {
            verifiedIcon.visibility = View.VISIBLE
        } else {
            verifiedIcon.visibility = View.GONE
        }

        val blockIds = listOf(R.id.block1, R.id.block2, /* ... */ R.id.block3)
        for ((i, id) in blockIds.withIndex()) {
            val tv = userView.findViewById<TextView>(id)
            if (i < user.preferences.size) {
                Log.d("TestPref", "preferences size = ${user.preferences.size}, data = ${user.preferences}")
                Log.d("BindPref", "block $i = ${user.preferences[i]}")
                tv.text = user.preferences[i]
                tv.visibility = View.VISIBLE
            } else {
                tv.visibility = View.GONE
            }
            if (index >= users.size || users.isEmpty()) {
                binding.userListLayout.removeAllViews()
                labellist.text = "ไม่มีผู้ใช้ที่แนะนำ"
                Toast.makeText(requireContext(), "ไม่มีผู้ใช้อีกแล้ว", Toast.LENGTH_SHORT).show()
                return
            }
           labellist.text = "มีคนที่เหมาะกับคุณ: ${users.size} คน"

            val titledistance: TextView = userView.findViewById(R.id.titledistance)

            if (myLat != null && myLng != null && user.latitude != null && user.longitude != null) {
                val distanceKm = calculateDistance(myLat!!, myLng!!, user.latitude!!, user.longitude!!) / 1000
                titledistance.text = String.format("ห่าง %.2f กม.", distanceKm)
                titledistance.visibility = View.VISIBLE
            } else {
                titledistance.text = "ไม่ทราบระยะทาง"
                titledistance.visibility = View.VISIBLE
            }
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

    private fun calculateDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double): Float {
        val results = FloatArray(1)
        Location.distanceBetween(lat1, lng1, lat2, lng2, results)
        return results[0] // หน่วย: เมตร
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
                        // ไม่ต้องเช็ค selectedUserID ตรงนี้
                        checkMatch(likedID)
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }

    // ฟังก์ชันตรวจสอบการ Match
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
                        // ถ้ามาจาก selectedUserID (กรณีเปิดจาก WhoLike)
                        if (selectedUserID != -1) {
                            selectedUserID = -1
                            fetchRecommendedUsers { fetchedUsers ->
                                users = fetchedUsers
                                currentIndex = 0
                                displayUser(currentIndex)
                            }
                        } else {
                            removeAndShowNextUser(currentIndex)
                        }
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

    private val recentlyDisliked = mutableSetOf<Int>()

    // ฟังก์ชันสำหรับการกด "Dislike"
//    private fun dislikeUser(dislikedID: Int) {
//        val url = getString(R.string.root_url) + "/api_v2/dislike"
//        val formBody = FormBody.Builder()
//            .add("dislikerID", userID.toString())
//            .add("dislikedID", dislikedID.toString())
//            .build()
//
//        client.newCall(Request.Builder().url(url).post(formBody).build()).enqueue(object : okhttp3.Callback {
//            override fun onFailure(call: okhttp3.Call, e: IOException) {
//                requireActivity().runOnUiThread {
//                    Toast.makeText(requireContext(), "Failed to dislike user", Toast.LENGTH_SHORT).show()
//                }
//            }
//
//            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
//                requireActivity().runOnUiThread {
//                    if (response.isSuccessful) {
//                        if (selectedUserID != -1) {
//                            selectedUserID = -1
//                            fetchRecommendedUsers { fetchedUsers ->
//                                users = fetchedUsers
//                                currentIndex = 0
//                                displayUser(currentIndex)
//                            }
//                        } else {
//                            // <-- ปรับมาใช้แบบนี้ -->
//                            removeAndShowNextUser(currentIndex)
//                        }
//                    } else {
//                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
//                    }
//                }
//            }
//        })
//    }

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
                        removeAndShowNextUser(currentIndex)
                    } else {
                        Toast.makeText(requireContext(), "Error: ${response.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        })
    }


    private fun fetchUserByID(targetUserID: Int, callback: (User?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/user/detail"
            val formBody = FormBody.Builder()
                .add("userID", targetUserID.toString())
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("fetchUserByID", "Response body: $responseBody")

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    // แก้ตรงนี้ด้วย: ถ้า response เป็น object → ใช้ JSONObject()
                    val jsonObject = JSONObject(responseBody)

                    val imageFile = jsonObject.getString("imageFile")

                    val profilePicture = if (imageFile.startsWith("http")) {
                        imageFile  // เป็น full URL อยู่แล้ว
                    } else {
                        val baseImageUrl = getString(R.string.root_url2) + "/ai_v2/user/"
                        baseImageUrl + imageFile
                    }

                    // 👉 เพิ่มส่วนนี้เพื่ออ่าน preferences (array)
                    val prefsJsonArray = jsonObject.optJSONArray("preferences")
                    val prefsList = mutableListOf<String>()
                    if (prefsJsonArray != null) {
                        for (j in 0 until prefsJsonArray.length()) {
                            prefsList.add(prefsJsonArray.getString(j))
                        }
                    }


                    val user = User(
                        jsonObject.getInt("userID"),
                        jsonObject.getString("nickname"),
                        profilePicture,
                        jsonObject.optString("DateBirth", ""),
                        jsonObject.getInt("verify"),
                        prefsList,  // <-- ส่ง prefs เข้า data class ด้วย
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
                Log.e("fetchUserByID", "Exception: ${e.message}")
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

    private fun removeAndShowNextUser(removeIndex: Int) {
        val userList = users.toMutableList()
        Log.d("REMOVE", "ก่อน remove: users=${userList.map { it.userID }}, currentIndex=$currentIndex, removeIndex=$removeIndex")
        if (removeIndex in userList.indices) {
            recentlyDisliked.add(userList[removeIndex].userID)
            Log.d("REMOVE", "กำลัง remove userID=${userList[removeIndex].userID}")
            userList.removeAt(removeIndex)
        }
        users = userList
        Log.d("REMOVE", "หลัง remove: users=${users.map { it.userID }}, currentIndex=$currentIndex")
        if (currentIndex >= users.size) {
            currentIndex = 0
            Log.d("REMOVE", "currentIndex reset to 0")
        }
        if (users.isEmpty()) {
            Log.d("REMOVE", "users ว่าง! fetchRecommendedUsers ใหม่")
            fetchRecommendedUsers { fetchedUsers ->
                var filteredUsers = fetchedUsers.filter { it.userID !in recentlyDisliked }
                Log.d("REMOVE", "หลัง fetch: users=${filteredUsers.map { it.userID }}")
                if (filteredUsers.isEmpty()) {
                    Log.d("REMOVE", "No user left after filter")
                     showNoMoreUsersDialog()
                    return@fetchRecommendedUsers
                }
                users = filteredUsers
                currentIndex = 0
                displayUser(currentIndex)
            }
        } else {
            displayUser(currentIndex)
        }
    }



    // แปลงข้อมูล JSON ที่ได้จาก API เป็นรายการผู้ใช้
    private fun parseUsers(responseBody: String?): List<User> {
        val users = mutableListOf<User>()
        responseBody?.let {
            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                val imageFile = jsonObject.getString("imageFile")
                val prefsJsonArray = jsonObject.optJSONArray("preferences")
                val prefsList = mutableListOf<String>()
                if (prefsJsonArray != null) {
                    for (j in 0 until prefsJsonArray.length()) {
                        prefsList.add(prefsJsonArray.getString(j))
                    }
                }
                Log.d("parseUsers", "User $i imageFile: $imageFile")  // เพิ่มบรรทัดนี้

                val user = User(
                    jsonObject.getInt("UserID"),
                    jsonObject.getString("nickname"),
                    jsonObject.getString("imageFile"),
                    jsonObject.optString("dateBirth", ""),
                    jsonObject.getInt("verify"),
                    prefsList, // เพิ่มตรงนี้
                    jsonObject.optDouble("latitude"),     // <-- เพิ่ม!
                    jsonObject.optDouble("longitude")


                )
                users.add(user)
            }
        }
        return users
    }

    private var isDialogShowing = false

    private fun showNoMoreUsersDialog() {
        if (isDialogShowing) return
        isDialogShowing = true
        AlertDialog.Builder(requireContext())
            .setTitle("ผู้ใช้ที่เหมาะกับคุณหมดแล้ว")
            .setMessage("คุณสามารถเพิ่มขอบเขตระยะทางการค้นหา เพื่อหาคนใหม่ๆได้")
            .setPositiveButton("ตกลง") { dialog, _ ->
                dialog.dismiss()
                isDialogShowing = false
            }
            .show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        recentlyDisliked.clear() // ลืม user ที่ dislike ไป
    }
}

// คลาสสำหรับเก็บข้อมูลผู้ใช้
data class User(
    val userID: Int,
    val nickname: String,
    val profilePicture: String,
    val dateBirth: String,
    val verify: Int,
    val preferences: List<String> = emptyList(), // ← เพิ่มตรงนี้
    val latitude: Double? = null,        // เพิ่มตรงนี้
    val longitude: Double? = null        // เพิ่มตรงนี้
)

