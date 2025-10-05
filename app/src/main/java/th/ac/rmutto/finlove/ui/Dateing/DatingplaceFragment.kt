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

    // ตำแหน่งสมมติของผู้ใช้
    private val userLat = 13.7563
    private val userLng = 100.5018

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
                    "🌐 ${place.gmapsUrl}"  // ส่ง URL เต็มไปในข้อความ

            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_confirm_date, null)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)

            val txtMessage = dialogView.findViewById<TextView>(R.id.txtDialogMessage)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            txtMessage.text = "คุณต้องการชวนอีกฝ่ายไปสถานที่นี้หรือไม่?\n\n📍 ${place.title}"

            // ตั้งค่าคลิกได้ที่ "ดูแผนที่"
            txtMessage.movementMethod = LinkMovementMethod.getInstance()  // ใช้เพื่อให้ข้อความที่เป็นลิงก์สามารถคลิกได้

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

    // บนคลาส DatingPlaceFragment (ใส่ตรงไหนก็ได้ในคลาส)
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

            // หัวข้อ "เนื่องจาก:" เป็นตัวหนา
            val headerStart = length
            append("เนื่องจาก:\n")
            setSpan(
                android.text.style.StyleSpan(android.graphics.Typeface.BOLD),
                headerStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            // bullet เหตุผล (คงข้อความเดิม)
            val bulletStart = length
            append("• คุณอาจอยู่ห่างกันเกินไป\n")
            setSpan(
                android.text.style.LeadingMarginSpan.Standard(0, 36),
                bulletStart, length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
            )

            append("กรุณาลองใหม่ภายหลัง")
        }

        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setIcon(android.R.drawable.ic_dialog_info) // ไอคอนมาตรฐาน ปลอดภัย ไม่พัง R
            .setTitle(title)
            .setMessage(msg)
            .setCancelable(false)
            .setPositiveButton("กลับไปแชท") { _, _ ->
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }
            .show()
    }

    private fun loadPlacesFromApi(matchId: Int) {
        val baseUrl = getString(R.string.root_url2)  // ดึงจาก strings.xml

        // ✅ แสดง dot loader
        dotContainer.visibility = View.VISIBLE
        startDotAnimation()

        // ✅ แสดง ProgressBar ก่อนโหลด
        txtTitle.visibility = View.GONE
        txtDescription.visibility = View.GONE
        txtLocation.visibility = View.GONE
        imagePlace.visibility = View.GONE
        btnNext.visibility = View.GONE
        btnPrev.visibility = View.GONE
        btnDate.visibility = View.GONE
        btnBackToChat.visibility = View.GONE

        val client = OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)  // รอ connect 30 วิ
            .readTimeout(60, TimeUnit.SECONDS)     // รออ่านผล 60 วิ
            .writeTimeout(60, TimeUnit.SECONDS)    // รอเขียน 60 วิ
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .addConverterFactory(GsonConverterFactory.create())
            .client(client)   // 👈 ผูก client ที่ตั้ง timeout
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
                                gmapsUrl = it.gmaps_url   // ✅ ดึงมาด้วย
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

    private fun showPlace(index: Int) {

        // ✅ ป้องกัน crash ถ้า list ว่างหรือ index ไม่ถูกต้อง
        if (placeList.isEmpty() || index !in placeList.indices) {
            txtTitle.text = "❌ ไม่มีสถานที่แนะนำ"
            txtDescription.text = "โปรดลองใหม่อีกครั้ง หรือขยายรัศมีค้นหา"
            imagePlace.setImageResource(R.drawable.ic_edit) // placeholder
            txtLocation.text = "ไม่มีพิกัดให้แสดง"
            return
        }

        val place = placeList[index]
        txtTitle.text = place.title

        // ✅ ถ้า description ว่าง → แสดงข้อความ fix
        val desc = if (place.description.isBlank() || place.description == "ไม่มีคำอธิบาย") {
            "ℹ️ ยังไม่มีรายละเอียดเพิ่มเติมสำหรับสถานที่นี้\n✨ แต่ที่นี่อาจเป็นจุดที่น่าสนใจสำหรับการนัดเดท!"
        } else {
            "${decorateDescription(place.description)}\n${getExtraMessage(place.description)}"
        }
        txtDescription.text = desc

        Glide.with(this)
            .load(place.photoUrl)
            .placeholder(R.drawable.ic_edit)
            .override(600, 400)        // กำหนดขนาดที่ต้องการ (px)
            .centerCrop()              // ตัดกลางให้พอดีกับ ImageView
            .into(imagePlace)

        val distance = calculateDistance(userLat, userLng, place.latitude, place.longitude)
        // ✅ ใช้ SpannableStringBuilder เพื่อกำหนด alignment แยกทีละบรรทัด
        val text = "📍 พิกัดของสถานที่\nห่างจากคุณประมาณ %.1f กม.".format(distance / 1000)
        val spannable = android.text.SpannableStringBuilder(text)

        // หาตำแหน่งของบรรทัด "ห่างจากคุณ..."
        val start = text.indexOf("ห่างจากคุณ")
        val end = text.length

        // ✅ จัดให้บรรทัดนี้อยู่ตรงกลาง
        spannable.setSpan(
            android.text.style.AlignmentSpan.Standard(android.text.Layout.Alignment.ALIGN_CENTER),
            start, end,
            Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
        )

        txtLocation.text = spannable
        // สร้างข้อความลิงก์
        val linkText = getShortLink(place.gmapsUrl)

        // ตั้งค่าให้ TextView รองรับการคลิกที่ลิงก์
        txtLocation.append("  ") // เพิ่มช่องว่างระหว่างข้อความ
        txtLocation.append(linkText)  // เพิ่มลิงก์ที่สามารถคลิกได้

        txtLocation.movementMethod = LinkMovementMethod.getInstance()  // ตั้งค่าให้สามารถคลิกลิงก์ได้
    }

    private fun showRetryDialog(message: String, onRetry: () -> Unit) {
        androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("โอ๊ะ! มีปัญหา")
            .setMessage(message)
            .setPositiveButton("ลองใหม่") { _, _ -> onRetry() }
            .setNegativeButton("ปิด", null)
            .show()
    }

    private fun handleHttpError(code: Int, matchId: Int) {
        when (code) {
            404, 502, 503, 504 -> {
                // ใช้ dialog แบบเดียวกับลิสต์ว่าง
                showNoSpotsDialog()
            }
            else -> {
                // เคสอื่นจะยังเป็น Toast เหมือนเดิม (ถ้าอยากให้เป็น dialog เดียวกันทั้งหมด ก็บอกได้)
                Toast.makeText(requireContext(), "โหลดข้อมูลไม่สำเร็จ ($code)", Toast.LENGTH_SHORT).show()
            }
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
        // ถ้า URL เป็น Google Maps จะทำให้ "ดูแผนที่" เป็นลิงก์ที่คลิกได้
        return if (url.startsWith("https://www.google.com/maps")) {
            val spanString = SpannableString("ดูแผนที่")
            spanString.setSpan(object : ClickableSpan() {
                override fun onClick(widget: View) {
                    // เปิด Google Maps
                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                    startActivity(intent)
                }
            }, 0, spanString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            spanString
        } else {
            SpannableString(url)  // ถ้าไม่ใช่ Google Maps ก็ให้แสดง URL ปกติ
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
