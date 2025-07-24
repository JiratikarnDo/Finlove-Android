package th.ac.rmutto.finlove.ui.Dateing

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import th.ac.rmutto.finlove.Place
import th.ac.rmutto.finlove.R
import android.content.Intent
import th.ac.rmutto.finlove.ChatActivity




class DatingPlaceFragment : Fragment() {
    private lateinit var txtTitle: TextView
    private lateinit var txtDescription: TextView
    private lateinit var txtLocation: TextView
    private lateinit var imagePlace: ImageView
    private lateinit var btnNext: ImageButton
    private lateinit var btnPrev: ImageButton
    private lateinit var btnDate: Button

    private var currentIndex = 0
    private lateinit var placeList: List<Place>
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
        val btnBackToChat: Button = view.findViewById(R.id.button2)


        txtTitle = view.findViewById(R.id.txtPlacetitle)
        txtDescription = view.findViewById(R.id.txtplaceDescription)
        txtLocation = view.findViewById(R.id.txtPlacelocat)
        imagePlace = view.findViewById(R.id.imageplaceview)
        btnNext = view.findViewById(R.id.imageButtonnext)
        btnPrev = view.findViewById(R.id.imageButtonback)
        btnDate = view.findViewById(R.id.button)


        placeList = listOf(
            Place("ท้องฟ้าจำลอง", "ท้องฟ้าจำลองเอกมัยหรือศูนย์วิทยาศาสตร์เพื่อการศึกษา เป็นหนึ่งในสถานที่ยอดฮิตพวกคุณสามารถมีเวลาที่ดีร่วมกันได้ที่นี่", "ใกล้ BTS จตุจักร", 13.7563, 100.5018, R.drawable.skyroom_pic),
            Place("สวนเบญจกิตติ", "เต็มไปด้วยธรรมชาติที่ร่มลื่นให้ความรู้สึกสงบและปลอดภัย พร้อมกิจกรรมมากมายที่ทำร่วมกันได้ เช่น ปั่นจักรยานและนั่งพูดคุย", "สุขุมวิท 39", 13.7350, 100.5432, R.drawable.benjakiti_park),
            Place("สยามพารากอน", "ศูนย์การค้าหรูใจกลางกรุงเทพฯ แหล่งรวมร้านค้าแบรนด์ดัง โรงหนัง พิพิธภัณฑ์สัตว์น้ำ และร้านอาหารชั้นนำ", "991 ถนนพระราม 1 แขวงปทุมวัน เขตปทุมวัน กรุงเทพมหานคร 10330", 13.745972, 100.534423, R.drawable.siam_paragon)
        )

        showPlace(currentIndex)

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
            val place = placeList[currentIndex]
            val message = "ที่นี่น่าสนใจดีนะ คุณอยากลองเดทกับฉันไหม?\n\n" +
                    "📍 ${place.title}\n" +
                    "📝 ${place.description}\n" +
                    "🌐 พิกัด: ${txtLocation.text}"

            val dialogView = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_confirm_date, null)
            val dialog = android.app.Dialog(requireContext())
            dialog.setContentView(dialogView)
            dialog.setCancelable(true)

            val txtTitle = dialogView.findViewById<TextView>(R.id.txtDialogTitle)
            val txtMessage = dialogView.findViewById<TextView>(R.id.txtDialogMessage)
            val btnConfirm = dialogView.findViewById<Button>(R.id.btnConfirm)
            val btnCancel = dialogView.findViewById<Button>(R.id.btnCancel)

            txtMessage.text = "คุณต้องการชวนอีกฝ่ายไปยังสถานที่นี้หรือไม่?\n\n📍 ${place.title}"

            btnConfirm.setOnClickListener {
                dialog.dismiss()
                val intent = Intent(requireContext(), ChatActivity::class.java).apply {
                    putExtra("matchID", arguments?.getInt("matchID") ?: -1)
                    putExtra("senderID", arguments?.getInt("senderID") ?: -1)
                    putExtra("nickname", arguments?.getString("nickname") ?: "ไม่ระบุชื่อ")
                    putExtra("autoMessage", message)
                }
                startActivity(intent)
            }

            btnCancel.setOnClickListener {
                dialog.dismiss()
            }

            dialog.show()
        }

        btnBackToChat.setOnClickListener {
            requireActivity().onBackPressed()
        }


        return view
    }

    private fun showPlace(index: Int) {
        val place = placeList[index]
        txtTitle.text = place.title
        txtDescription.text = place.description
        imagePlace.setImageResource(place.imageResId)

        val distance = calculateDistance(userLat, userLng, place.latitude, place.longitude)
        val distanceText = String.format("ห่างจากฉัน %.2f กม.", distance / 1000)
        txtLocation.text = distanceText
    }

    private fun calculateDistance(
        userLat: Double,
        userLng: Double,
        placeLat: Double,
        placeLng: Double
    ): Float {
        val userLocation = android.location.Location("").apply {
            latitude = userLat
            longitude = userLng
        }
        val placeLocation = android.location.Location("").apply {
            latitude = placeLat
            longitude = placeLng
        }
        return userLocation.distanceTo(placeLocation)
    }
}
