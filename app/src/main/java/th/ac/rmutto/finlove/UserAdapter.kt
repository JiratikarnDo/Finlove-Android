package th.ac.rmutto.finlove

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import com.bumptech.glide.load.DecodeFormat
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.request.RequestOptions

// Adapter สำหรับ RecyclerView
class UserAdapter(private val users: List<User>, private val currentUserID: Int) :
    RecyclerView.Adapter<UserAdapter.UserViewHolder>() {

    private val client = OkHttpClient()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return UserViewHolder(view)
    }

    override fun onBindViewHolder(holder: UserViewHolder, position: Int) {
        val user = users[position]
        holder.bind(user, currentUserID)


    }

    override fun getItemCount(): Int = users.size

    // ViewHolder สำหรับผู้ใช้แต่ละรายการ
    class UserViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val nickname: TextView = itemView.findViewById(R.id.textNickname)
        private val profileImage: ImageView = itemView.findViewById(R.id.imageProfile)
        private val likeButton: ImageButton = itemView.findViewById(R.id.buttonLike)
        private val ageTextView: TextView = itemView.findViewById(R.id.textAge)
        private val dislikeButton: ImageButton = itemView.findViewById(R.id.buttonDislike)

        // ฟังก์ชัน bind สำหรับกำหนดข้อมูลให้กับ View
        fun bind(user: User, currentUserID: Int) {
            nickname.text = user.nickname // ตั้งค่าชื่อเล่นจาก User data class
            val expectedUrl = user.imageFile
            profileImage.tag = expectedUrl

            Glide.with(itemView).clear(profileImage)
            profileImage.setImageDrawable(null)

            // คำนวณขนาดเป้าหมาย (fallback หากยังไม่ได้ layout)
            val w = if (profileImage.width > 0) profileImage.width else itemView.resources.displayMetrics.widthPixels
            val h = if (profileImage.height > 0) profileImage.height else (w * 4) / 3

            Glide.with(profileImage)
                .load(expectedUrl)
                .apply(
                    RequestOptions()
                        .disallowHardwareConfig()
                        .format(DecodeFormat.PREFER_RGB_565)
                        .timeout(60000) // ✅ รอเครือข่ายนานขึ้น ลดโอกาส fail ก่อน อีกตัวจะตามขึ้น
                )
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .signature(com.bumptech.glide.signature.ObjectKey(user.id.toString())) // ← เพิ่มบรรทัดนี้
                .skipMemoryCache(false)
                .dontAnimate()
                .override((w * 0.75f).toInt(), (h * 0.75f).toInt())
                .centerCrop()
                .placeholder(R.drawable.ic_user)
                .fallback(R.drawable.ic_user) // ✅ ถ้า URL เป็น null จะไม่ขึ้น error ก่อน
                .error(R.drawable.error)
                .into(profileImage)

            val age = calculateAge(user.dateBirth ?: "")
            Log.d("UserAdapter", "Calculated age: $age")
            ageTextView.text = if (age >= 0) "$age" else "ไม่ทราบอายุ"
            Log.d("UserAdapter", "user.dateBirth = ${user.dateBirth}")
            Log.d("UserAdapter", "calculated age = $age")

            // กดชอบผู้ใช้
            likeButton.setOnClickListener {
                likeUser(user.id, currentUserID) // ส่ง id ของผู้ใช้ที่ต้องการชอบ
            }

            // กดไม่ชอบผู้ใช้
            dislikeButton.setOnClickListener {
                dislikeUser(user.id, currentUserID) // ส่ง id ของผู้ใช้ที่ต้องการไม่ชอบ
            }

        }

        // ฟังก์ชันที่ใช้ในการส่ง HTTP POST ไปยัง api_v2
        private fun sendPostRequest(url: String, formBody: FormBody, callback: (Response?, IOException?) -> Unit) {
            val request = Request.Builder().url(url).post(formBody).build()
            val client = OkHttpClient()
            client.newCall(request).enqueue(object : okhttp3.Callback {
                override fun onFailure(call: okhttp3.Call, e: IOException) {
                    callback(null, e)
                }

                override fun onResponse(call: okhttp3.Call, response: Response) {
                    callback(response, null)
                }
            })
        }

        private fun calculateAge(dateString: String): Int {
            if (dateString.isEmpty()) return -1
            return try {
                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                val birthDate = sdf.parse(dateString) ?: return -1

                val today = Calendar.getInstance()
                val dob = Calendar.getInstance()
                dob.time = birthDate

                var age = today.get(Calendar.YEAR) - dob.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < dob.get(Calendar.DAY_OF_YEAR)) {
                    age--
                }
                age
            } catch (e: Exception) {
                e.printStackTrace()
                -1
            }
        }


        // ฟังก์ชันสำหรับการกดชอบ
        private fun likeUser(likedID: Int, likerID: Int) {
            val url = itemView.context.getString(R.string.root_url) + "/api_v2/like"
            val formBody = FormBody.Builder()
                .add("likerID", likerID.toString()) // userID ของผู้ใช้ที่กดชอบ
                .add("likedID", likedID.toString())
                .build()

            sendPostRequest(url, formBody) { response, e ->
                (itemView.context as AppCompatActivity).runOnUiThread {
                    if (e != null) {
                        Toast.makeText(itemView.context, "Failed to like user", Toast.LENGTH_SHORT).show()
                    } else if (response != null && response.isSuccessful) {
                        Toast.makeText(itemView.context, "User liked successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(itemView.context, "Error: ${response?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }

        // ฟังก์ชันสำหรับการกดไม่ชอบ
        private fun dislikeUser(dislikedID: Int, dislikerID: Int) {
            val url = itemView.context.getString(R.string.root_url) + "/api_v2/dislike"
            val formBody = FormBody.Builder()
                .add("dislikerID", dislikerID.toString()) // userID ของผู้ใช้ที่กดไม่ชอบ
                .add("dislikedID", dislikedID.toString())
                .build()

            sendPostRequest(url, formBody) { response, e ->
                (itemView.context as AppCompatActivity).runOnUiThread {
                    if (e != null) {
                        Toast.makeText(itemView.context, "Failed to dislike user", Toast.LENGTH_SHORT).show()
                    } else if (response != null && response.isSuccessful) {
                        Toast.makeText(itemView.context, "User disliked successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(itemView.context, "Error: ${response?.message}", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
    }
}
