package th.ac.rmutto.finlove

import android.content.Intent
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.text.SimpleDateFormat
import java.util.*

class WholikeAdapter(
    private val items: List<User>, // หรือ UserLike หากคุณมี UserLike
    private val onItemClick: (User) -> Unit  // เพิ่ม callback สำหรับคลิก
) : RecyclerView.Adapter<WholikeAdapter.ViewHolder>() {

    // ViewHolder สำหรับการอ้างถึง element ใน layout item
    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imageProfile: ImageView = view.findViewById(R.id.imageProfile)
        val textNickname: TextView = view.findViewById(R.id.textNickname)
        val imageVerified: ImageView = view.findViewById(R.id.imageVerified)
        val textUserid: TextView = view.findViewById(R.id.textUserid)
        val textAge: TextView = view.findViewById((R.id.textAge))
    }

    // สร้าง ViewHolder จาก layout item_wholike
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_wholike, parent, false)
        return ViewHolder(view)
    }

    fun calculateAge(dateString: String?): Int {
        if (dateString.isNullOrEmpty()) {
            return -1
        }
        return try {
            // แก้รูปแบบวันที่ให้ตรงกับฐานข้อมูล
            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val birthDate = sdf.parse(dateString)

            val today = Calendar.getInstance()
            val dob = Calendar.getInstance()
            dob.time = birthDate!!

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

    // คืนค่าจำนวน item ใน List
    override fun getItemCount(): Int = items.size

    // เชื่อมโยงข้อมูลจาก List ไปที่ View ในแต่ละ item
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = items[position]

        holder.textNickname.text = user.nickname
        holder.imageVerified.visibility = if (user.verify == 1) View.VISIBLE else View.GONE
        holder.textUserid.text = user.id.toString()  // ← ใช้ .id แทน .userID
        val age = calculateAge(user.dateBirth)
        holder.textAge.text = if (age >= 0) "$age" else "ไม่ทราบอายุ"
        Log.d("WholikeAdapter", "DateBirth: '${user.dateBirth}'")
        Log.d("WholikeAdapter", "User: ${user.nickname}, ID: ${user.id}")



        val context = holder.itemView.context
        val baseUrl = context.getString(R.string.root_url)
        val imageUrl = if (user.imageFile.startsWith("http")) {
            user.imageFile
        } else {
            "$baseUrl/api_v2/user/image/${user.imageFile}"
        }

        Glide.with(context)
            .load(imageUrl)
            .placeholder(R.drawable.ic_user)
            .centerCrop()
            .into(holder.imageProfile)

        holder.itemView.setOnClickListener {
            onItemClick(user)  // เรียก callback ให้ Fragment จัดการ navigation
        }
    }


}
