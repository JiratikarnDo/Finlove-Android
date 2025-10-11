package th.ac.rmutto.finlove.ui.wholike

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.*
import th.ac.rmutto.finlove.R
import th.ac.rmutto.finlove.User
import th.ac.rmutto.finlove.WholikeAdapter
import java.io.IOException
import androidx.navigation.fragment.findNavController
import com.google.gson.GsonBuilder
import th.ac.rmutto.finlove.StringListAdapter


class WhoLikeFragment : Fragment() {

    private fun formatKm(km: Double?): String {
        if (km == null) return "ไม่ทราบระยะทาง"
        return if (km < 1.0) "น้อยกว่า 1 กม." else String.format(java.util.Locale("th","TH"), "%.1f กม.", km)
    }

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WholikeAdapter
    private lateinit var switchShowLiked: Switch
    private lateinit var textSwitchTitle: TextView
    private lateinit var textSwitchSubTitle: TextView
    private val client = OkHttpClient()

    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(object : TypeToken<List<String>>() {}.type, StringListAdapter())
            .create()
    }

    private var showLikedByMe = false

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wholike, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLikes)
        switchShowLiked = view.findViewById(R.id.switchShowLiked)
        textSwitchTitle = view.findViewById(R.id.titleLikes) // id ตาม layout ของคุณ
        textSwitchSubTitle = view.findViewById(R.id.subtitleLikes)

        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        // ฟัง event สลับ
        switchShowLiked.setOnCheckedChangeListener { _, isChecked ->
            showLikedByMe = isChecked
            if (showLikedByMe) {
                textSwitchTitle.text = "คนที่คุณกดไลค์" // หรือข้อความที่ต้องการ
                textSwitchSubTitle.text = "รอหน่อยนะพวกเขาอาจชอบคุณ"
                fetchUsersILiked()
            } else {
                textSwitchTitle.text = "คนที่กดไลค์คุณ" // หรือข้อความที่ต้องการ
                textSwitchSubTitle.text = "มีคนชอบคุณเยอะเลยลองกดไลค์กลับดูสิ!!"
                fetchWhoLikeUsers()
            }
        }


        fetchWhoLikeUsers()

        return view
    }

    private fun fetchWhoLikeUsers() {
        val sharedPref = requireActivity().getSharedPreferences(
            "FinLovePrefs",
            android.content.Context.MODE_PRIVATE
        )
        val currentUserID = sharedPref.getInt("userID", -1)

        if (currentUserID == -1) {
            Toast.makeText(requireContext(), "กรุณาล็อกอินก่อนใช้งาน", Toast.LENGTH_SHORT).show()
            return
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        // 👉 ดึง matched users ก่อน
        val matchesUrl = getString(R.string.root_url) + "/api_v2/matches/$currentUserID"
        val matchesRequest = Request.Builder().url(matchesUrl).build()

        client.newCall(matchesRequest).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "โหลดข้อมูลไม่สำเร็จ (แมท)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                val matchedIDs = mutableSetOf<Int>()
                response.body?.string()?.let { matchesJson ->
                    try {
                        val jsonArray =
                            com.google.gson.JsonParser.parseString(matchesJson).asJsonArray
                        jsonArray.forEach {
                            val obj = it.asJsonObject
                            matchedIDs.add(obj["userID"].asInt)
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }

                // 👉 ดึง wholike ต่อหลังจากได้ matchedIDs แล้ว
                val whoLikeUrl =
                    getString(R.string.root_url) + "/api_v2/wholike?userID=$currentUserID"
                val whoLikeRequest = Request.Builder().url(whoLikeUrl).build()

                client.newCall(whoLikeRequest).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        activity?.runOnUiThread {
                            Toast.makeText(
                                requireContext(),
                                "โหลดข้อมูลไม่สำเร็จ (wholike)",
                                Toast.LENGTH_SHORT
                            ).show()
                        }
                    }


                    override fun onResponse(call: Call, response: Response) {
                        response.body?.string()?.let { jsonString ->
                            Log.d("WhoLikeFragment", "Response JSON: $jsonString")
                            val listType = object : TypeToken<List<User>>() {}.type
                            val users: List<User> = gson.fromJson(jsonString, listType)

                            // ✅ กรองเฉพาะคนที่ยังไม่แมท
                            val filteredUsers = users.filter { it.id !in matchedIDs }
                            // ✅ ทำ map สำหรับข้อความระยะทาง
                            val distances = filteredUsers.associate { u -> u.id to formatKm(u.distance) }

                            activity?.runOnUiThread {
                                adapter = WholikeAdapter(
                                    filteredUsers,
                                    { clickedUser ->
                                        val bundle = Bundle().apply {
                                            putInt("userID", currentUserID)
                                            putInt("selectedUserID", clickedUser.id)
                                        }
                                        findNavController().navigate(R.id.navigation_home, bundle)
                                    },
                                    itemClickable = !showLikedByMe,
                                    distances = distances
                                )
                                recyclerView.adapter = adapter
                            }
                        }
                    }
                })
            }
        })
    }

    private fun fetchUsersILiked() {
        val sharedPref = requireActivity().getSharedPreferences(
            "FinLovePrefs",
            android.content.Context.MODE_PRIVATE
        )
        val currentUserID = sharedPref.getInt("userID", -1)
        if (currentUserID == -1) {
            Toast.makeText(requireContext(), "กรุณาล็อกอินก่อนใช้งาน", Toast.LENGTH_SHORT).show()
            return
        }

        val url = getString(R.string.root_url) + "/api_v2/likedbyme?userID=$currentUserID"
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(
                        requireContext(),
                        "โหลดข้อมูลไม่สำเร็จ (likedbyme)",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    Log.d("WhoLikeFragment", "LikedByMe Response: $jsonString")
                    val listType = object : TypeToken<List<User>>() {}.type
                    val users: List<User> = gson.fromJson(jsonString, listType)
                    activity?.runOnUiThread {
                        adapter = WholikeAdapter(users, { clickedUser ->
                            val bundle = Bundle().apply {
                                putInt("userID", currentUserID)
                                putInt("selectedUserID", clickedUser.id)
                            }
                            findNavController().navigate(R.id.navigation_home, bundle)
                        }, itemClickable = !showLikedByMe) // << เพิ่มตรงนี้!
                        recyclerView.adapter = adapter
                    }
                }
            }
        })
    }
}
