package th.ac.rmutto.finlove

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.text.SpannableString
import android.text.Spanned
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import org.json.JSONArray
import androidx.emoji2.text.EmojiCompat

class ChatAdapter(private val currentUserID: Int) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {


    private var messages: List<ChatMessage> = listOf()

    companion object {
        private const val VIEW_TYPE_LEFT = 1
        private const val VIEW_TYPE_RIGHT = 2
    }

    fun setMessages(newMessages: List<ChatMessage>) {
        messages = newMessages
        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        val message = messages[position]
        return if (message.senderID == currentUserID) {
            VIEW_TYPE_RIGHT
        } else {
            VIEW_TYPE_LEFT
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == VIEW_TYPE_RIGHT) {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_right, parent, false)
            RightChatViewHolder(view)
        } else {
            val view = LayoutInflater.from(parent.context).inflate(R.layout.item_chat_message_left, parent, false)
            LeftChatViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messages[position]
        if (holder is RightChatViewHolder) {
            holder.bind(message)
        } else if (holder is LeftChatViewHolder) {
            holder.bind(message)
        }
    }

    // แก้ไข decodeIfDoubleEscaped เพื่อรับ Context เป็นพารามิเตอร์
    private fun decodeIfDoubleEscaped(context: Context, s: String?): SpannableString {
        if (s.isNullOrEmpty()) return SpannableString("")

        val maybe = s.replace("\\\\u", "\\u") // แปลง \\u -> \u
        return try {
            // ตรวจสอบว่า URL เป็น Google Maps หรือไม่
            if (maybe.startsWith("https://www.google.com/maps")) {
                val spanString = SpannableString("ดูแผนที่")
                spanString.setSpan(object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        // เปิด Google Maps เมื่อคลิกที่ลิงก์
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(maybe))
                        context.startActivity(intent)
                    }
                }, 0, spanString.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                return spanString
            }

            // หากไม่ใช่ URL ให้แสดงข้อความปกติ
            JSONArray("[\"$maybe\"]").getString(0).let {
                SpannableString(it)
            }
        } catch (_: Exception) {
            SpannableString(s) // ถ้าเกิดข้อผิดพลาด ให้แสดงข้อความเดิม
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatMessage: ChatMessage) {
            val decoded = decodeIfDoubleEscaped(itemView.context, chatMessage.message) // ส่ง context
            messageText.text = decoded
            messageText.movementMethod = LinkMovementMethod.getInstance() // ทำให้ลิงก์คลิกได้

            val url = chatMessage.profilePicture
            if (url.isNullOrBlank()) {
                profileImage.setImageResource(R.drawable.ic_user)
                return
            }

            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.error)
                .circleCrop()
                .into(profileImage)

            profileImage.setOnClickListener {
                val intent = Intent(itemView.context, OtherProfileActivity::class.java)
                intent.putExtra("userID", chatMessage.senderID)
                itemView.context.startActivity(intent)
            }
        }
    }

    inner class RightChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatMessage: ChatMessage) {
            val decoded = decodeIfDoubleEscaped(itemView.context, chatMessage.message) // ส่ง context
            messageText.text = decoded
            messageText.movementMethod = LinkMovementMethod.getInstance() // ทำให้ลิงก์คลิกได้
        }
    }
}
