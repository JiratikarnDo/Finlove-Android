package th.ac.rmutto.finlove.ui.Dateing

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.Intent
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.*
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import th.ac.rmutto.finlove.ChatActivity
import th.ac.rmutto.finlove.R
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.appcompat.app.AlertDialog
import java.util.Locale
import kotlin.math.roundToInt
import java.text.NumberFormat
import android.Manifest
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.location.FusedLocationProviderClient

// ================== Data Classes ==================
data class RecommendResponse(
    val users: List<Int>,
    val midpoint: Midpoint,
    val shared_preferences: List<String>,
    val spots: List<Spot>
)

data class Midpoint(
    val lat: Double,
    val lng: Double
)

data class Spot(
    val id: String,
    val name: String,
    val lat: Double,
    val lng: Double,
    val category: String?,
    val photo_url: String?,
    val description: String?,
    val gmaps_url: String
)

data class Place(
    val title: String,
    val description: String,
    val latitude: Double,
    val longitude: Double,
    val photoUrl: String?,
    val gmapsUrl: String   // ✅ ใช้ camelCase ให้ตรงกับ map ด้านล่าง
)

// ================== API Service ==================
interface ApiService {
    @GET("/ai_v2/recommend_places/{match_id}")
    fun getRecommendPlaces(@Path("match_id") matchId: Int): Call<RecommendResponse>
}
class DatingPlaceFragment : Fragment() {
    private lateinit var txtTitle: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtLocation: TextView
    private lateinit var imagePlace: ImageView
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnDate: Button
    private lateinit var btnBackToChat: Button
    private lateinit var dotContainer: LinearLayout
    private lateinit var dot1: View
    private lateinit var dot2: View
    private lateinit var dot3: View
    private lateinit var contentContainer: ViewGroup

    private var currentIndex = 0
    private var placeList: List<Place> = emptyList()

    // ✅ พิกัดของ "คนที่เปิดจอ" (viewer)
    private var userLat: Double? = null
    private var userLng: Double? = null

    // ✅ เพิ่ม: ตัวดึงพิกัด + ตัวขอ permission
    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private val locationPermLauncher =
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
            val ok = (perms[android.Manifest.permission.ACCESS_FINE_LOCATION] == true) ||
                    (perms[android.Manifest.permission.ACCESS_COARSE_LOCATION] == true)
            if (ok) getViewerLocation()
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.item_datingplace, container, false)

        val matchID = arguments?.getInt("matchID") ?: -1
        val senderID = arguments?.getInt("senderID") ?: -1
        val nickname = arguments?.getString("nickname") ?: "ไม่ระบุชื่อ"

        txtTitle = view.findViewById(R.id.txtPlacetitle)
        txtDescription = view.findViewById(R.id.txtplaceDescription)
        txtLocation = view.findViewById(R.id.txtPlacelocat)
        imagePlace = view.findViewById(R.id.imageplaceview)
        btnNext = view.findViewById(R.id.imageButtonnext)
        btnPrev = view.findViewById(R.id.imageButtonback)
        btnDate = view.findViewById(R.id.button)
        btnBackToChat = view.findViewById(R.id.button2)
        dotContainer = view.findViewById(R.id.dot_container)
        dot1 = view.findViewById(R.id.dot1)
        dot2 = view.findViewById(R.id.dot2)
        dot3 = view.findViewById(R.id.dot3)

        // อ่านพิกัดจาก args/SP ถ้ามี (ของเดิม)
        userLat = arguments?.getDouble("viewerLat", Double.NaN)?.takeIf { it.isFinite() }
        userLng = arguments?.getDouble("viewerLng", Double.NaN)?.takeIf { it.isFinite() }
        if (userLat == null || userLng == null) {
            val prefs = requireContext().getSharedPreferences("user_loc", android.content.Context.MODE_PRIVATE)
            userLat = prefs.getString("lat", null)?.toDoubleOrNull()
            userLng = prefs.getString("lng", null)?.toDoubleOrNull()
        }

        // ✅ ใหม่: ดึงพิกัดสดของผู้ใช้ในหน้านี้เอง
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity())
        ensureViewerLocation()

        // โหลดข้อมูลจาก API
        loadPlacesFromApi(matchID)

        btnNext.setOnClickListener {
            if (currentIndex < placeList.size - 1) {
                currentIndex++
                showPlace(currentIndex)
            } else {
                Toast.makeText(requireContext(), "ไม่มีสถานที่ถัดไปแล้ว", Toast.LENGTH_SHORT).show()
            }
        }

        btnPrev.setOnClickListener {
            if (currentIndex > 0) {
                currentIndex--
                showPlace(currentIndex)
            } else {
                Toast.makeText(requireContext(), "นี่คือรายการแรกแล้ว", Toast.LENGTH_SHORT).show()
            }
        }

        btnDate.setOnClickListener {
            if (placeList.isEmpty()) return@setOnClickListener
            val place = placeList[currentIndex]
            val message = "ที่นี่น่าสนใจดีนะ คุณอยากลองเดทกับฉันไหม?\n\n" +
                    "📍 ${place.title}\n" +
                    "📝 ${place.description}\n" +
                    "🌐 ${txtLocation.text}" +
                    "🌐 ${place.gmapsUrl}"

            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_date, null)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)

            val txtMessage = dialogView.findViewById<TextView>(R.id.txtDialogMessage)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            txtMessage.text = "คุณต้องการชวนอีกฝ่ายไปสถานที่นี้หรือไม่?\n\n📍 ${place.title}"
            txtMessage.movementMethod = LinkMovementMethod.getInstance()

            btnConfirm.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("matchID", matchID)
                    putExtra("senderID", senderID)
                    putExtra("nickname", nickname)
                    putExtra("autoMessage", message)
                }
                startActivity(intent)
            }
            btnCancel.setOnClickListener { dialog.dismiss() }
            dialog.show()
        }

        btnBackToChat.setOnClickListener {
            requireActivity().onBackPressed()
        }

        return view
    }

    // ✅ ขอสิทธิ์ → ดึงพิกัด → เซฟ + รีเฟรช UI
    private fun ensureViewerLocation() {
        val fineGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED
        val coarseGranted = androidx.core.app.ActivityCompat.checkSelfPermission(
            requireContext(), android.Manifest.permission.ACCESS_COARSE_LOCATION
        ) == android.content.pm.PackageManager.PERMISSION_GRANTED

        if (!fineGranted && !coarseGranted) {
            locationPermLauncher.launch(
                arrayOf(
                    android.Manifest.permission.ACCESS_FINE_LOCATION,
                    android.Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
            return
        }
        getViewerLocation()
    }

    private fun getViewerLocation() {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { loc: Location? ->
                if (loc != null) {
                    onViewerLocationReady(loc)
                } else {
                    val hasFine = androidx.core.app.ActivityCompat.checkSelfPermission(
                        requireContext(), android.Manifest.permission.ACCESS_FINE_LOCATION
                    ) == android.content.pm.PackageManager.PERMISSION_GRANTED
                    val priority = if (hasFine) Priority.PRIORITY_HIGH_ACCURACY
                    else Priority.PRIORITY_BALANCED_POWER_ACCURACY

                    val cts = com.google.android.gms.tasks.CancellationTokenSource()
                    fusedLocationClient.getCurrentLocation(priority, cts.token)
                        .addOnSuccessListener { now: Location? ->
                            if (now != null) onViewerLocationReady(now)
                        }
                        .addOnFailureListener { e: Exception ->
                            Log.w("DatingPlace", "getCurrentLocation failed: ${e.message}")
                        }
                }
            }
            .addOnFailureListener { e: Exception ->
                Log.w("DatingPlace", "getCurrentLocation failed: ${e.message}")
            }
    }

    private fun onViewerLocationReady(loc: Location) {
        userLat = loc.latitude
        userLng = loc.longitude
        // เก็บลง SharedPreferences ไว้ใช้ครั้งหน้า
        val sp = requireContext().getSharedPreferences("user_loc", android.content.Context.MODE_PRIVATE)
        sp.edit().putString("lat", userLat.toString())
            .putString("lng", userLng.toString())
            .apply()
        // ถ้ามีรายการสถานที่แล้ว → รีเฟรชให้เลิกขึ้น "กำลังระบุตำแหน่ง…"
        if (placeList.isNotEmpty()) showPlace(currentIndex)
    }
    // --- OSRM: ระยะทางตามถนน ---
    private val osrmClient by lazy {
        okhttp3.OkHttpClient.Builder()
            .callTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .build()
    }

    private fun fetchRoadDistanceMetersOSRM(
        startLat: Double, startLng: Double,
        endLat: Double, endLng: Double,
        profile: String = "driving",
        onResult: (Long?) -> Unit
    ) {
        val url = "https://router.project-osrm.org/route/v1/$profile/" +
                "$startLng,$startLat;$endLng,$endLat?overview=false&alternatives=false&steps=false"

        // ใช้ OkHttp ตรง ๆ เพื่อให้แยกจาก Retrofit ปัจจุบัน
        Thread {
            try {
                val req = okhttp3.Request.Builder().url(url).build()
                val resp = osrmClient.newCall(req).execute()
                val body = resp.body?.string()
                val distance = org.json.JSONObject(body ?: "{}")
                    .getJSONArray("routes")
                    .getJSONObject(0)
                    .getLong("distance") // หน่วย: เมตร
                requireActivity().runOnUiThread { onResult(distance) }
            } catch (e: Exception) {
                requireActivity().runOnUiThread { onResult(null) }
            }
        }.start()
    }

    private fun showNoSpotsDialog() {
        if (!isAdded) return

        val title = android.text.SpannableString("ไม่พบสถานที่แนะนำ").apply {
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                0, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        val msg = android.text.SpannableStringBuilder().apply {
            append("ไม่สามารถหาสถานที่ที่เหมาะสมได้ในตอนนี้\n\n")
            val headerStart = length
            append("เนื่องจาก:\n")
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                headerStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            val bulletStart = length
            append("• คุณอาจอยู่ห่างกันเกินไป\n")
            setSpan(
                android.text.style.LeadingMarginSpan.Standard(0, 36),
                bulletStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
            append("กรุณาลองใหม่ภายหลัง")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setIcon(android.R.drawable.ic_dialog_info)
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("กลับไปแชท") { _, _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .show()
    }

    private fun loadPlacesFromApi(matchId: Int) {
        val baseUrl = getString(R.string.root_url2)

        // ✅ แสดง dot loader
        dotContainer.visibility = View.VISIBLE
        startDotAnimation()

        // ✅ ซ่อนคอนเทนต์ระหว่างโหลด
        txtTitle.visibility = View.GONE
        txtDescription.visibility = View.GONE
        txtLocation.visibility = View.GONE
        imagePlace.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnPrev.visibility = View.GONE
        btnDate.visibility = View.GONE
        btnBackToChat.visibility = View.GONE

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)
            .build()

        val api = retrofit.create(ApiService::class.java)

        api.getRecommendPlaces(matchId).enqueue(object : Callback<RecommendResponse> {
            override fun onResponse(call: Call<RecommendResponse>, response: Response<RecommendResponse>) {
                // ✅ ซ่อน dot loader
                dotContainer.visibility = View.GONE

                txtTitle.visibility = View.VISIBLE
                txtDescription.visibility = View.VISIBLE
                txtLocation.visibility = View.VISIBLE
                imagePlace.visibility = View.VISIBLE
                btnNext.visibility = View.VISIBLE
                btnPrev.visibility = View.VISIBLE
                btnDate.visibility = View.VISIBLE
                btnBackToChat.visibility = View.VISIBLE

                if (response.isSuccessful) {
                    val recommend = response.body()
                    Log.d("API_DEBUG", "onResponse called, code=${response.code()}")
                    if (recommend != null) {
                        placeList = recommend.spots.map {
                            Place(
                                title = it.name,
                                description = it.description ?: "ไม่มีคำอธิบาย",
                                latitude = it.lat,
                                longitude = it.lng,
                                photoUrl = it.photo_url,
                                gmapsUrl = it.gmaps_url
                            )
                        }
                        if (placeList.isEmpty()) {
                            showNoSpotsDialog()
                        } else {
                            currentIndex = 0
                            showPlace(currentIndex)
                        }
                    }
                } else {
                    handleHttpError(response.code(), matchId)
                }
            }

            override fun onFailure(call: Call<RecommendResponse>, t: Throwable) {
                // ✅ ซ่อน dot loader
                dotContainer.visibility = View.GONE
                Toast.makeText(requireContext(), "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                Log.e("API_DEBUG", "onFailure: ${t.message}", t)
            }
        })
    }

    // ใช้โลเคลไทย (จะได้รูปแบบเลข/คอมม่าแบบไทย)
    private val TH = Locale("th", "TH")
    private val nf0 = NumberFormat.getNumberInstance(TH).apply {
        maximumFractionDigits = 0; minimumFractionDigits = 0; isGroupingUsed = true
    }
    private val nf1 = NumberFormat.getNumberInstance(TH).apply {
        maximumFractionDigits = 1; minimumFractionDigits = 1; isGroupingUsed = true
    }

    /// แปลงเมตร -> ข้อความอ่านง่าย (ภาษาไทย)
    private fun formatDistance(meters: Float): String {
        if (meters.isNaN() || meters.isInfinite()) return "—"
        val m = meters.coerceAtLeast(0f)
        return when {
            m < 1f        -> "<1 เมตร"
            m < 1000f     -> "${nf0.format(((m / 10f).roundToInt() * 10))} ม."
            m < 100_000f  -> "${nf1.format(m / 1000f)} กม."
            else          -> "${nf0.format(m / 1000f)} กม."
        }
    }

    private fun showPlace(index: Int) {
        if (placeList.isEmpty() || index !in placeList.indices) {
            txtTitle.text = "❌ ไม่มีสถานที่แนะนำ"
            txtDescription.text = "โปรดลองใหม่อีกครั้ง หรือขยายรัศมีค้นหา"
            imagePlace.setImageResource(R.drawable.ic_edit)
            txtLocation.text = "ไม่มีพิกัดให้แสดง"
            return
        }

        val place = placeList[index]
        txtTitle.text = place.title

        val desc = if (place.description.isBlank() || place.description == "ไม่มีคำอธิบาย") {
            "ℹ️ ยังไม่มีรายละเอียดเพิ่มเติมสำหรับสถานที่นี้\n✨ แต่ที่นี่อาจเป็นจุดที่น่าสนใจสำหรับการนัดเดท!"
        } else {
            "${decorateDescription(place.description)}\n${getExtraMessage(place.description)}"
        }
        txtDescription.text = desc

        Glide.with(this)
            .load(place.photoUrl)
            .placeholder(R.drawable.ic_edit)
            .override(600, 400)
            .centerCrop()
            .into(imagePlace)

        // ✅ ข้อความเริ่มต้น ระหว่างคำนวณระยะทาง "ตามถนน"
        val baseText = "📍 พิกัดของสถานที่\nห่างจากคุณประมาณ (…)"
        val baseSpan = android.text.SpannableStringBuilder(baseText).apply {
            val start = baseText.indexOf("ห่างจากคุณ")
            val end = baseText.length
            setSpan(
                android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )
        }

        // ✅ ต้องใส่บรรทัดนี้ก่อน append ลิงก์
        txtLocation.text = baseSpan
        // ✅ ลิงก์ Google Maps
        val linkText = getShortLink(place.gmapsUrl)
        txtLocation.append("  ")
        txtLocation.append(linkText)
        txtLocation.movementMethod = LinkMovementMethod.getInstance()

        // ✅ ดึงระยะทาง "ตามถนน" มาแสดงแทนเลย
        if (userLat != null && userLng != null) {
            fetchRoadDistanceMetersOSRM(
                userLat!!, userLng!!,
                place.latitude, place.longitude,
                profile = "driving" // หรือ "walking"/"cycling"
            ) { roadMeters ->
                // สร้างข้อความใหม่ (ใช้ตามถนนเป็นหลัก, ล้มเหลวค่อย fallback เส้นตรง)
                val distanceText: String = if (roadMeters != null) {
                    "(${formatDistance(roadMeters.toFloat())})"
                } else {
                    // fallback → เส้นตรง
                    val straight = calculateDistance(userLat!!, userLng!!, place.latitude, place.longitude)
                    "(${formatDistance(straight)})"
                }

                val finalText = "📍 พิกัดของสถานที่\nห่างจากคุณประมาณ $distanceText"
                val span = android.text.SpannableStringBuilder(finalText).apply {
                    val s = finalText.indexOf("ห่างจากคุณ")
                    val e = finalText.length
                    setSpan(
                        android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                        s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                    )
                }
                // เขียนทับ + ใส่ลิงก์ใหม่ (เพื่อไม่ให้ซ้ำ)
                txtLocation.text = span
                txtLocation.append("  ")
                txtLocation.append(linkText)
            }
        } else {
            // ไม่มีพิกัดฝั่งผู้ใช้
            val finalText = "📍 พิกัดของสถานที่\nห่างจากคุณประมาณ (กำลังระบุตำแหน่ง…)"
            val span = android.text.SpannableStringBuilder(finalText).apply {
                val s = finalText.indexOf("ห่างจากคุณ")
                val e = finalText.length
                setSpan(
                    android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
                    s, e, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                )
            }
            txtLocation.text = span
            txtLocation.append("  ")
            txtLocation.append(linkText)
        }
    }

    private fun handleHttpError(code: Int, matchId: Int) {
        when (code) {
            404, 502, 503, 504 -> showNoSpotsDialog()
            else -> Toast.makeText(requireContext(), "โหลดข้อมูลไม่สำเร็จ ($code)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun calculateDistance(userLat: Double, userLng: Double, placeLat: Double, placeLng: Double): Float {
        val userLocation = Location("").apply {
            latitude = userLat
            longitude = userLng
        }
        val placeLocation = Location("").apply {
            latitude = placeLat
            longitude = placeLng
        }
        return userLocation.distanceTo(placeLocation)
    }

    private fun startDotAnimation() {
        val dots = listOf(dot1, dot2, dot3)
        for ((index, dot) in dots.withIndex()) {
            ObjectAnimator.ofPropertyValuesHolder(
                dot,
                PropertyValuesHolder.ofFloat("scaleX", 1.5f, 0.5f, 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.5f, 0.5f, 1.5f)
            ).apply {
                duration = 500
                startDelay = (index * 150).toLong()
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    private fun getShortLink(url: String): SpannableString {
        return if (url.startsWith("https://www.google.com/maps")) {
            val spanString = SpannableString("ดูแผนที่")
            spanString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            }, 0, spanString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanString
        } else {
            SpannableString(url)
        }
    }

    private fun decorateDescription(rawText: String): String {
        val lower = rawText.lowercase()
        val icon = when {
            "coffee" in lower || "คาเฟ่" in lower || "cafe" in lower -> "☕ "
            "ramen" in lower || "ราเมน" in lower -> "\uD83C\uDF5C " // 🍜
            "park" in lower || "สวน" in lower -> "\uD83C\uDF33 " // 🌳
            "restaurant" in lower || "ร้านอาหาร" in lower -> "\uD83C\uDF7D️ " // 🍽️
            "bar" in lower || "ผับ" in lower -> "\uD83C\uDF7A " // 🍺
            "museum" in lower || "พิพิธภัณฑ์" in lower -> "\uD83C\uDFF0 " // 🏰
            else -> "ℹ️ "
        }
        return "$icon$rawText"
    }

    private fun getExtraMessage(description: String): String {
        val lower = description.lowercase()
        return when {
            "คาเฟ่" in lower || "cafe" in lower || "coffee" in lower ->
                " ใช้เวลาร่วมกับคู่ของคุณ ดื่มด่ำไปกับบรรยากาศดี ๆ และเครื่องดื่มที่คุณชอบ"
            "ร้านอาหาร" in lower || "restaurant" in lower || "cuisine" in lower ->
                " ลิ้มรสอาหารแสนอร่อยในบรรยากาศอบอุ่นกับคู่ของคุณ"
            "สวน" in lower || "park" in lower ->
                " เดินเล่นในบรรยากาศธรรมชาติ สูดอากาศบริสุทธิ์ไปพร้อมกัน"
            "บาร์" in lower || "bar" in lower || "pub" in lower ->
                " สนุกกับบรรยากาศยามค่ำคืน พร้อมเครื่องดื่มและเสียงเพลง"
            "พิพิธภัณฑ์" in lower || "museum" in lower ->
                " เดินชมและเรียนรู้สิ่งใหม่ ๆ ไปด้วยกัน"
            else ->
                " สถานที่นี้เหมาะสำหรับการใช้เวลาร่วมกัน"
        }
    }
}
