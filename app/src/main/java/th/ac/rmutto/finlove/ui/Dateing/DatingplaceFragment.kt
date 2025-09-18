package th.ac.rmutto.finlove.ui.Dateing

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.app.Dialog
import android.content.Intent
import android.location.Location
import android.os.Bundle
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
    val photoUrl: String?
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
                    "🌐 พิกัด: ${txtLocation.text}"

            val dialogView = LayoutInflater.from(requireContext())
                .inflate(R.layout.dialog_confirm_date, null)
            val dialog = Dialog(requireContext())
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)

            val txtMessage = dialogView.findViewById<TextView>(R.id.txtDialogMessage)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            txtMessage.text = "คุณต้องการชวนอีกฝ่ายไปยังสถานที่นี้หรือไม่?\n\n📍 ${place.title}"

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
                                photoUrl = it.photo_url
                            )
                        }
                        currentIndex = 0
                        showPlace(currentIndex)
                    }
                } else {
                    Toast.makeText(requireContext(), "โหลดข้อมูลไม่สำเร็จ", Toast.LENGTH_SHORT).show()
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
        val place = placeList[index]
        txtTitle.text = place.title
        txtDescription.text = place.description

        Glide.with(this)
            .load(place.photoUrl)
            .placeholder(R.drawable.ic_edit)
            .override(600, 400)        // กำหนดขนาดที่ต้องการ (px)
            .centerCrop()              // ตัดกลางให้พอดีกับ ImageView
            .into(imagePlace)

        val distance = calculateDistance(userLat, userLng, place.latitude, place.longitude)
        txtLocation.text = String.format("ห่างจากฉัน %.2f กม.", distance / 1000)
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

}
