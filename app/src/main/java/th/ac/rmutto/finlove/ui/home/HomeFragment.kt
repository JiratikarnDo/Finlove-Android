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
import android.content.Context
import android.location.Location
import com.google.android.gms.location.*
import androidx.activity.result.contract.ActivityResultContracts
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.common.api.ResolvableApiException
import androidx.activity.result.IntentSenderRequest
import th.ac.rmutto.finlove.utils.AnimationHelper
import org.json.JSONObject
import com.bumptech.glide.load.engine.DiskCacheStrategy
import android.graphics.Color
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.request.RequestOptions
import androidx.navigation.fragment.findNavController
import android.widget.ImageView
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import th.ac.rmutto.finlove.BioBottomSheetFragment
import th.ac.rmutto.finlove.utils.AnimationHelper.animateImageSlideInFromRight
import th.ac.rmutto.finlove.LoadingDialogFragment
import android.graphics.drawable.Drawable
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.signature.ObjectKey

private fun optDoubleOrNull(obj: org.json.JSONObject, key: String): Double? {
    val v = obj.opt(key)
    return when (v) {
        is Number -> v.toDouble().let { d -> if (d.isFinite()) d else null }
        is String -> v.toDoubleOrNull()?.let { d -> if (d.isFinite()) d else null }
        else -> null
    }
}
private fun sanitizeJsonNumbers(raw: String?): String? {
    if (raw.isNullOrEmpty()) return raw
    return raw
        .replace(Regex("""\bNaN\b"""), "null")
        .replace(Regex("""\bInfinity\b"""), "null")
        .replace(Regex("""\b-Infinity\b"""), "null")
}
class HomeFragment : Fragment() {

    // ===== Loading dialog helpers =====
    private var loadingDialog: LoadingDialogFragment? = null
    private fun showLoading() {
        if (loadingDialog?.isAdded != true) {
            loadingDialog = LoadingDialogFragment.show(childFragmentManager)
        }
    }
    private fun hideLoading() {
        loadingDialog?.dismissAllowingStateLoss()
        loadingDialog = null
    }

    /** (ทางเลือก) พรีโหลดรูปทั้งหมดให้เข้าดิสก์แคชก่อนขึ้นจอ */
    private suspend fun preloadAllImages(urls: List<String>) = withContext(Dispatchers.IO) {
        val app = requireContext().applicationContext
        val targets = urls.filter { it.isNotBlank() }.map { u ->
            Glide.with(app).downloadOnly().load(u).submit()
        }
        targets.forEach { t -> try { t.get() } catch (_: Exception) {} ; Glide.with(app).clear(t) }
    }

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
            val fine = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true
            val coarse = permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
            if (fine || coarse) {      // ✅ ยอมรับอย่างใดอย่างหนึ่ง
                turnOnGPS()
            } else {
                Toast.makeText(requireContext(), "กรุณาอนุญาตการเข้าถึงตำแหน่ง", Toast.LENGTH_SHORT).show()
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

        // ✅ ใส่ default ถ้ายังไม่เคยมี
        val prefs = requireContext().getSharedPreferences("FinLovePrefs", Context.MODE_PRIVATE)
        if (!prefs.contains("age_min") || !prefs.contains("age_max") || !prefs.contains("max_distance_km")) {
            prefs.edit()
                .putInt("age_min", 18)
                .putInt("age_max", 60)
                .putFloat("max_distance_km", 50f) // 50 กม.
                .apply()
        }

        checkAndRequestLocationPermission()
        recentlyDisliked.clear()

        // กู้คืน currentIndex หากมีการบันทึกไว้
        showLoading()

        if (selectedUserID != -1) {
            // เปิดจาก WhoLike → ดึงคนเดียว
            fetchUserByID(selectedUserID) { user ->
                lifecycleScope.launch {
                    if (user != null) {
                        users = listOf(user)
                        currentIndex = 0

                        // (ทางเลือก) พรีโหลดรูปก่อน
                        val urls = users.map { it.profilePicture.trim() }.filter { it.isNotBlank() }
                        preloadAllImages(urls)

                        hideLoading()
                        displayUser(currentIndex)
                    } else {
                        hideLoading()
                        Toast.makeText(requireContext(), "ไม่พบข้อมูลผู้ใช้ที่เลือก", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        } else {
            // ปกติ → ดึงลิสต์แนะนำ
            if (userID != -1) {
                fetchRecommendedUsers { fetchedUsers ->
                    lifecycleScope.launch {
                        if (fetchedUsers.isNotEmpty()) {
                            users = fetchedUsers
                            currentIndex = 0

                            // (ทางเลือก) พรีโหลดรูปก่อน
                            val urls = users.map { it.profilePicture.trim() }.filter { it.isNotBlank() }
                            preloadAllImages(urls)

                            hideLoading()
                            displayUser(currentIndex)
                        } else {
                            hideLoading()
                            Toast.makeText(requireContext(), "ไม่พบผู้ใช้ที่แนะนำ", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                hideLoading()
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

    // ===== helper กรองตามช่วงอายุที่ผู้ใช้ตั้งไว้ =====
    private fun filterByAge(users: List<User>): List<User> {
        val prefs = requireContext().getSharedPreferences("FinLovePrefs", android.content.Context.MODE_PRIVATE)
        val minAge = prefs.getInt("age_min", 18)
        val maxAge = prefs.getInt("age_max", 60)
        return users.filter { u ->
            val a = calculateAge(u.dateBirth)
            a in minAge..maxAge
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
        val fine = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        val coarse = ActivityCompat.checkSelfPermission(
            requireContext(), Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
        return fine || coarse
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
                    // 🔥 ขอพิกัดสดแบบ one-shot แทน
                    val hasFine = ActivityCompat.checkSelfPermission(
                        requireContext(), Manifest.permission.ACCESS_FINE_LOCATION
                    ) == PackageManager.PERMISSION_GRANTED

                    val priority = if (hasFine)
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY
                    else
                        com.google.android.gms.location.Priority.PRIORITY_BALANCED_POWER_ACCURACY

                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(priority, cts.token)
                        .addOnSuccessListener { loc ->
                            if (loc != null) {
                                myLat = loc.latitude
                                myLng = loc.longitude
                                Log.d("GPS", "📍 currentLocation: ${loc.latitude}, ${loc.longitude}")
                                sendLocationToServer(loc.latitude, loc.longitude)
                            } else {
                                Toast.makeText(requireContext(), "ไม่สามารถดึงพิกัดได้", Toast.LENGTH_SHORT).show()
                            }
                        }
                        .addOnFailureListener {
                            Toast.makeText(requireContext(), "เกิดข้อผิดพลาดในการดึงตำแหน่ง", Toast.LENGTH_SHORT).show()
                        }
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
        Log.d("HomeFragment", "📌 Displaying user: ${user.nickname}, UserID=${user.userID}")
        Log.d("HomeFragment", "ProfilePicture URL: ${user.profilePicture}")
        Log.d("HomeFragment", "ImageView tag before load: ${profileImage.tag}")
        nickname.text = user.nickname
// ผูก tag กับ URL ที่ "normalize แล้ว"
        val expectedTag = user.profilePicture
        profileImage.tag = expectedTag

        val imageView = userView.findViewById<ImageView>(R.id.imageProfile)// ล้างภาพเก่า
// เรียก animation
        animateImageSlideInFromRight(imageView) {
            val rawUrl = user.profilePicture.trim()
            if (rawUrl.isBlank()) {
                profileImage.setImageResource(R.drawable.ic_user)
            } else {
                val finalUrl = rawUrl + (if (rawUrl.contains("?")) "&" else "?") + "u=${user.userID}"
                Log.d("GLIDE_URL", "load -> $finalUrl")
                Glide.with(this@HomeFragment)
                    .load(finalUrl)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.error)
                    .centerCrop()
                    .signature(ObjectKey(finalUrl)) // กันแคชชน
                    .into(profileImage)
            }
        }

        // ตรวจสอบสถานะ verify และแสดงไอคอนเครื่องหมายถูกหาก verify == 1
        if (user.verify == 1) {
            verifiedIcon.visibility = View.VISIBLE
        } else {
            verifiedIcon.visibility = View.GONE
        }

        val buttonBio: ImageButton = userView.findViewById(R.id.buttonBio)

// เมื่อกดปุ่ม "ดูข้อมูลเพิ่มเติม"
        buttonBio.setOnClickListener {
            lifecycleScope.launch(Dispatchers.IO) {
                try {
                    // ✅ ใช้เส้นทาง /api_v2/user/:id
                    val url = getString(R.string.root_url) + "/api_v2/user/${user.userID}"

                    val request = Request.Builder()
                        .url(url)
                        .get() // <-- ใช้ GET
                        .build()

                    val response = client.newCall(request).execute()
                    val responseBody = response.body?.string()
                    val json = JSONObject(responseBody ?: "{}")

                    // ✅ ดึง bio
                    val bio = json.optString("bio", "ไม่มีข้อมูล Bio")

                    withContext(Dispatchers.Main) {
                        val bioBottomSheet = BioBottomSheetFragment.newInstance(
                            userID = user.userID,
                            nickname = user.nickname,
                            profilePicture = user.profilePicture,
                            dateBirth = user.dateBirth,
                            verify = user.verify,
                            preferences = user.preferences,
                            bio = bio
                        )
                        Log.d("ShowBioSheet", "🔍 user=${user.nickname}, bio=${bio}")
                        bioBottomSheet.show(childFragmentManager, "BioBottomSheet")
                    }
                } catch (e: Exception) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(
                            requireContext(),
                            "โหลดข้อมูลไม่สำเร็จ: ${e.message}",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }

        val blockIds = listOf(R.id.block1, R.id.block2, /* ... */ R.id.block3)

        // สร้างลิสต์สำหรับแสดง: ให้ shared มาก่อน แล้วตามด้วยที่เหลือจาก all
        val shared = user.sharedPreferences.filter { it.isNotBlank() }
        var all = user.allPreferences.filter { it.isNotBlank() }
// ถ้า all ว่าง แต่ preferences มี ให้ fallback:
        if (all.isEmpty() && user.preferences.isNotEmpty()) {
            all = user.preferences.filter { it.isNotBlank() }
        }
        val nonShared = all.filterNot { it in shared }
        val displayList = shared + nonShared

        for ((i, id) in blockIds.withIndex()) {
            val tv = userView.findViewById<TextView>(id)

            when (i) {
                0, 1 -> {
                    val text = displayList.getOrNull(i)
                    if (text.isNullOrBlank()) {
                        tv.text = ""
                        tv.visibility = View.GONE
                    } else {
                        tv.text = text
                        tv.visibility = View.VISIBLE
                        val isShared = text in shared
                        tv.setBackgroundResource(
                            if (isShared) R.drawable.button_pressed else R.drawable.button_normal
                        )
                        tv.setTextColor(
                            if (isShared) Color.WHITE else Color.BLACK
                        )
                        Log.d("BindPref", "block $i = $text (shared=$isShared)")
                    }
                }
                2 -> {
                    val text = displayList.getOrNull(2)
                    if (text.isNullOrBlank()) {
                        tv.text = ""
                        tv.visibility = View.GONE
                    } else {
                        val remain = (displayList.size - 3).coerceAtLeast(0)
                        tv.text = if (remain > 0) "$text (+$remain)" else text
                        tv.visibility = View.VISIBLE
                        val isShared = text in shared
                        tv.setBackgroundResource(
                            if (isShared) R.drawable.button_pressed else R.drawable.button_normal
                        )
                        tv.setTextColor(
                            if (isShared) Color.WHITE else Color.BLACK
                        )
                        Log.d("BindPref", "block $i = ${tv.text} (shared=$isShared)")
                    }
                }
            }
            if (index >= users.size || users.isEmpty()) {
                binding.userListLayout.removeAllViews()
                labellist.text = "ไม่มีผู้ใช้ที่แนะนำ"
                Toast.makeText(requireContext(), "ไม่มีผู้ใช้อีกแล้ว", Toast.LENGTH_SHORT).show()
                return
            }
           labellist.text = "มีคนเหมาะกับคุณ: ${users.size}"

            val titledistance: TextView = userView.findViewById(R.id.titledistance)

            // 1) ใช้ค่าที่ backend ส่งมาก่อน (หน่วย: เมตร)
            if (user.distance != null) {
                if (user.distance < 1.0) {
                    titledistance.text = "ห่างจากคุณ: น้อยกว่า 1 กม."
                } else {
                    titledistance.text = String.format("ห่างจากคุณ: %.1f กม.", user.distance)
                }
                titledistance.visibility = View.VISIBLE

// 2) ถ้าไม่มี distance ใน JSON ค่อย fallback คำนวณจากพิกัด
            } else if (myLat != null && myLng != null && user.latitude != null && user.longitude != null) {
                val km = calculateDistance(myLat!!, myLng!!, user.latitude!!, user.longitude!!)
                titledistance.text = String.format("ห่างจากคุณ: %.1f กม.", km)
                titledistance.visibility = View.VISIBLE

// 3) ไม่งั้นไม่ทราบ
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

    private fun getMaxDistanceKm(): Double {
        val prefs = requireContext().getSharedPreferences("FinLovePrefs", android.content.Context.MODE_PRIVATE)
        return prefs.getFloat("max_distance_km", 50f).toDouble()
    }

    private fun filterByDistance(users: List<User>): List<User> {
        val maxKm = getMaxDistanceKm()   // ✅ backend ส่ง km อยู่แล้ว
        val myLat_ = myLat
        val myLng_ = myLng

        return users.filter { u ->
            u.distance?.let { km ->
                // ✅ ใช้ตรง ๆ เพราะ backend ส่งเป็น km แล้ว
                return@filter km >= 0.0 && km <= maxKm
            }
            if (myLat_ != null && myLng_ != null && u.latitude != null && u.longitude != null) {
                val meters = calculateDistance(myLat_, myLng_, u.latitude!!, u.longitude!!) // คืน "เมตร"
                val km = meters / 1000.0   // ✅ แปลงเป็น km ก่อนเทียบ
                return@filter km >= 0.0 && km <= maxKm
            }
            // ✅ soft-block: ถ้ายังไม่มีข้อมูลพอจะวัด → ปล่อยผ่าน
            true
        }
    }

    private fun formatDistanceFromMeters(meters: Double?): String {
        val safe = (meters ?: return "ไม่ทราบระยะทาง").coerceAtLeast(0.0)
        return if (safe < 1.0) {
            // ถ้าน้อยกว่า 1 กม.
            "น้อยกว่า 1 กม."
        } else {
            val km = safe / 1000.0
            String.format(Locale("th","TH"), "%.1f กม.", km)
        }
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
                // (ทางเลือก) log ไว้ดีบักถ้ามีปัญหา
                val bodyStr = response.body?.string()
                android.util.Log.d("DISLIKE", "code=${response.code} body=$bodyStr")
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

    private fun jsonOptIntAny(obj: JSONObject, vararg keys: String, fallback: Int = -1): Int {
        for (k in keys) if (obj.has(k)) return obj.optInt(k, fallback)
        return fallback
    }

    private fun normalizeImageUrl(imageFile: String?): String {
        val s = imageFile?.trim().orEmpty()            // << trim() กัน space/บรรทัดใหม่
        if (s.isBlank()) return ""
        return if (s.startsWith("http")) s
        else getString(R.string.root_url2) + "/ai_v2/user/" + s.trimStart('/')
    }

    private fun fetchUserByID(targetUserID: Int, callback: (User?) -> Unit) {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/user/detail"

            val prefs = requireContext().getSharedPreferences("FinLovePrefs", Context.MODE_PRIVATE)
            val token = prefs.getString("jwt_token", null)

            if (token.isNullOrBlank()) {
                Log.w("fetchUserByID", "token is null/blank → จะโดน missing token แน่นอน")
                withContext(Dispatchers.Main) { callback(null) }
                return@launch
            }

            val formBody = FormBody.Builder()
                .add("userID", targetUserID.toString())
                .build()

            val request = Request.Builder()
                .url(url)
                .post(formBody)
                .addHeader("Authorization", "Bearer $token")
                // .addHeader("Cookie", "token=$token") // ถ้า backend อ่านจากคุกกี้ ให้ใช้บรรทัดนี้แทน/ร่วมด้วย
                .build()

            try {
                val response = client.newCall(request).execute()
                val responseBody = response.body?.string()
                Log.d("fetchUserByID", "code=${response.code} body=$responseBody")

                // ถ้า token หมดอายุ/ผิด → 401
                if (response.code == 401) {
                    withContext(Dispatchers.Main) { callback(null) }
                    return@launch
                }

                if (response.isSuccessful && !responseBody.isNullOrEmpty()) {
                    val jsonObject = JSONObject(responseBody)

                    // ★ รองรับทั้ง "UserID" และ "userID"
                    val parsedId = jsonOptIntAny(jsonObject, "UserID", "userID", fallback = -1)
                    if (parsedId <= 0) {
                        Log.w("fetchUserByID", "invalid user id in payload: $jsonObject")
                        withContext(Dispatchers.Main) { callback(null) }
                        return@launch
                    }

                    val imageFile = jsonObject.optString("imageFile")
                    // ใหม่
                    val profileUrl = normalizeImageUrl(jsonObject.optString("imageFile"))

                    val prefsList = mutableListOf<String>().apply {
                        jsonObject.optJSONArray("preferences")?.let { arr ->
                            for (j in 0 until arr.length()) add(arr.optString(j))
                        }
                    }

                    val allPrefs = mutableListOf<String>().apply {
                        jsonObject.optJSONArray("allPreferences")?.let { arr ->
                            for (j in 0 until arr.length()) add(arr.optString(j))
                        }
                        if (isEmpty()) addAll(prefsList)
                    }

                    val sharedPrefs = mutableListOf<String>().apply {
                        // ★ ฝั่งเซิร์ฟเวอร์บางที่ส่ง "shared_preferences"
                        (jsonObject.optJSONArray("sharedPreferences")
                            ?: jsonObject.optJSONArray("shared_preferences"))?.let { arr ->
                            for (j in 0 until arr.length()) add(arr.optString(j))
                        }
                    }

                    val user = User(
                        parsedId, // ★ ใช้ parsedId ที่เชื่อถือได้
                        jsonObject.optString("nickname"),
                        profileUrl, // << ใช้ URL ที่ normalize แล้ว
                        jsonObject.optString("DateBirth", ""),
                        jsonObject.optInt("verify"),
                        prefsList,
                        optDoubleOrNull(jsonObject, "latitude"),
                        optDoubleOrNull(jsonObject, "longitude"),
                        allPreferences = allPrefs,
                        sharedPreferences = sharedPrefs,
                        // ⬇️ แก้ตรงนี้: รับ distance_km (กม.) และ fallback ไปที่ distance
                        distance = optDoubleOrNull(jsonObject, "distance_km")
                            ?: optDoubleOrNull(jsonObject, "distance"),
                        bio = jsonObject.optString("bio", null)   // 👈 เพิ่มตรงนี้
                    )

                    withContext(Dispatchers.Main) { callback(user) }
                } else {
                    withContext(Dispatchers.Main) { callback(null) }
                }
            } catch (e: Exception) {
                Log.e("fetchUserByID", "Exception: ${e.message}", e)
                withContext(Dispatchers.Main) { callback(null) }
            }
        }
    }

    private fun NoUsersDialog() {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("ไม่พบผู้ใช้")
            .setMessage("กรุณาปรับช่วงอายุ และ กำหนดขอบเขตระยะทางเพิ่ม")
            .setPositiveButton("ไปตั้งค่า") { d, _ ->
                d.dismiss()
                // ใช้ id ของหน้าตั้งค่าจริงใน nav_graph ของคุณ
                findNavController().navigate(R.id.settingsFragment)
                // หรือถ้ามี action ระหว่างหน้าปัจจุบัน → settings:
                // findNavController().navigate(R.id.action_homeFragment_to_settingsFragment)
            }
            .show()
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
                    val safeBody = sanitizeJsonNumbers(responseBody)  // ← เพิ่มบรรทัดนี้
                    val recommendedUsers = parseUsers(safeBody)
                    // ✨ กรองด้วยช่วงอายุที่ผู้ใช้ตั้งไว้
                    val visibleByAge = filterByAge(recommendedUsers)

                    // ✅ ใหม่: ตัดคนที่ "ไม่ทราบระยะทาง" ออก
                    // เงื่อนไขยอมรับ 2 แบบ:
                    // 1) backend ใส่ distance มาแล้ว (เป็นกม.)
                    // 2) ไม่มี distance แต่เรามีพิกัดเรา + พิกัดเขา → คำนวณเองได้
                    val knownDistanceOnly = visibleByAge.filter { u ->
                        (u.distance != null) ||
                                (myLat != null && myLng != null && u.latitude != null && u.longitude != null)
                    }
                    // 2) กรองด้วยระยะทาง (ดึง max_distance_km จาก SharedPreferences)
                    val visible = filterByDistance(knownDistanceOnly)

                    withContext(Dispatchers.Main) {
                        if (visible.isEmpty()) {
                            NoUsersDialog()   // ← เด้ง popup + ไปหน้า Settings
                        } else {
                            callback(visible)
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(requireContext(), "ไม่สามารถดึงข้อมูลผู้ใช้ได้ ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main
                ) {
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

    private fun optDoubleOrNull(obj: org.json.JSONObject, key: String): Double? {
        val v = obj.optDouble(key, Double.NaN)
        return if (v.isNaN()) null else v
    }

    // แปลงข้อมูล JSON ที่ได้จาก API เป็นรายการผู้ใช้
    private fun parseUsers(responseBody: String?): List<User> {
        val users = mutableListOf<User>()
        responseBody?.let {
            val jsonArray = JSONArray(it)
            for (i in 0 until jsonArray.length()) {
                val jsonObject = jsonArray.getJSONObject(i)
                // ---- รูป ----
                val imageFileRaw = jsonObject.optString("imageFile", "")
                val profileUrl = normalizeImageUrl(imageFileRaw)
                Log.d("RECO_RAW", "uid=${jsonObject.optInt("UserID")} file=$imageFileRaw url=$profileUrl")
                val prefsJsonArray = jsonObject.optJSONArray("preferences")
                // preferences (ของเดิม)
                val prefsList = mutableListOf<String>()
                jsonObject.optJSONArray("preferences")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        prefsList.add(arr.getString(j))
                    }
                }

                // ---- เพิ่มใหม่: allPreferences ----
                val allPrefs = mutableListOf<String>().apply {
                    jsonObject.optJSONArray("allPreferences")?.let { arr ->
                        for (j in 0 until arr.length()) {
                            add(arr.optString(j))
                        }
                    }
                    if (isEmpty()) addAll(prefsList)   // ✅ เรียก isEmpty() ของ list ถูกต้องแล้ว
                }

                // ---- เพิ่มใหม่: sharedPreferences ----
                val sharedPrefs = mutableListOf<String>()
                jsonObject.optJSONArray("sharedPreferences")?.let { arr ->
                    for (j in 0 until arr.length()) {
                        sharedPrefs.add(arr.getString(j))
                    }
                }
                if (prefsJsonArray != null) {
                    for (j in 0 until prefsJsonArray.length()) {
                        prefsList.add(prefsJsonArray.getString(j))
                    }
                }

                Log.d("parseUsers", "User $i distance(raw)=${jsonObject.opt("distance")}")
                Log.d("parseUsers", "User $i bio=${jsonObject.optString("bio")}")

                val user = User(
                    jsonObject.getInt("UserID"),
                    jsonObject.getString("nickname"),
                    profileUrl, // << ใช้ URL ที่ normalize แล้ว
                    jsonObject.optString("dateBirth", ""),
                    jsonObject.getInt("verify"),
                    preferences = prefsList, // เพิ่มตรงนี้
                    optDoubleOrNull(jsonObject, "latitude"),     // ✅ กลายเป็น null ถ้า NaN/ไม่มีค่า
                    optDoubleOrNull(jsonObject, "longitude"),    // ✅,
                    allPreferences = allPrefs,           // << ใส่ค่าเพิ่ม
                    sharedPreferences = sharedPrefs,      // << ใส่ค่าเพิ่ม
                    distance = optDoubleOrNull(jsonObject, "distance"),   // ✅ ใช้ helper
                    bio = jsonObject.optString("bio", null)
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
    val longitude: Double? = null,        // เพิ่มตรงนี้
    val distance: Double? = null,
    val allPreferences: List<String> = emptyList(),
    val sharedPreferences: List<String> = emptyList(),
    val bio: String? = null, // เพิ่มฟิลด์ Bio
)

