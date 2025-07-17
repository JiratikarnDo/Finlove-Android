package th.ac.rmutto.finlove.ui.wholike

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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


class WhoLikeFragment : Fragment() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: WholikeAdapter
    private val client = OkHttpClient()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_wholike, container, false)

        recyclerView = view.findViewById(R.id.recyclerViewLikes)
        recyclerView.layoutManager = LinearLayoutManager(requireContext())

        fetchWhoLikeUsers()

        return view
    }

    private fun fetchWhoLikeUsers() {
        // ดึง userID ที่ล็อกอินจริง ๆ จาก SharedPreferences (ตั้งสมมติชื่อไฟล์และคีย์ให้ตรงกับที่คุณเก็บ)
        val sharedPref = requireActivity().getSharedPreferences("FinLovePrefs", android.content.Context.MODE_PRIVATE)
        val currentUserID = sharedPref.getInt("userID", -1)  // default -1 = ไม่มี userID

        if (currentUserID == -1) {
            Toast.makeText(requireContext(), "กรุณาล็อกอินก่อนใช้งาน", Toast.LENGTH_SHORT).show()
            return
        }

        recyclerView.layoutManager = GridLayoutManager(requireContext(), 2)

        val url = getString(R.string.root_url) + "/api_v2/wholike?userID=$currentUserID"

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                activity?.runOnUiThread {
                    Toast.makeText(requireContext(), "โหลดข้อมูลไม่สำเร็จ", Toast.LENGTH_SHORT).show()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.body?.string()?.let { jsonString ->
                    Log.d("WhoLikeFragment", "Response JSON: $jsonString")
                    val gson = Gson()
                    val listType = object : TypeToken<List<User>>() {}.type
                    val users: List<User> = gson.fromJson(jsonString, listType)

                    activity?.runOnUiThread {
                        adapter = WholikeAdapter(users) { clickedUser ->
                            // เมื่อคลิกรายการใน RecyclerView ให้ navigate พร้อมส่งข้อมูล
                            val bundle = Bundle().apply {
                                putInt("userID", currentUserID)  // id ผู้ใช้หลัก (เรา)
                                putInt("selectedUserID", clickedUser.id)  // id ผู้ใช้ที่ถูกคลิก
                            }
                            findNavController().navigate(R.id.navigation_home, bundle)
                        }
                        recyclerView.adapter = adapter
                    }
                }
            }
        })
    }

}
