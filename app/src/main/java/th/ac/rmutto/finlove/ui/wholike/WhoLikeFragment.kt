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
                            val gson = Gson()
                            val listType = object : TypeToken<List<User>>() {}.type
                            val users: List<User> = gson.fromJson(jsonString, listType)

                            // ✅ กรองเฉพาะคนที่ยังไม่แมท
                            val filteredUsers = users.filter { it.id !in matchedIDs }

                            activity?.runOnUiThread {
                                adapter = WholikeAdapter(filteredUsers) { clickedUser ->
                                    val bundle = Bundle().apply {
                                        putInt("userID", currentUserID)
                                        putInt("selectedUserID", clickedUser.id)
                                    }
                                    findNavController().navigate(R.id.navigation_home, bundle)
                                }
                                recyclerView.adapter = adapter
                            }
                        }
                    }
                })
            }
        })
    }
}
