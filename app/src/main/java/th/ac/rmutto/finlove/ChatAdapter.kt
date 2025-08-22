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

    /** ถอดกรณีที่ถูก double-escaped เช่น \\uD83D\\uDE00 -> 😀 */
    private fun decodeIfDoubleEscaped(s: String?): String {
        if (s.isNullOrEmpty()) return ""
        val maybe = s.replace("\\\\u", "\\u")           // แปลง \\u -> \u
        return try {
            // ให้ JSON แปลง \uXXXX เป็นตัวจริง
            JSONArray("[\"$maybe\"]").getString(0)
        } catch (_: Exception) {
            s
        }
    }

    override fun getItemCount(): Int = messages.size

    inner class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatMessage: ChatMessage) {
            val decoded = decodeIfDoubleEscaped(chatMessage.message)
            messageText.text = chatMessage.message
            messageText.text = EmojiCompat.get().process(decoded)
            val url = chatMessage.profilePicture
            Log.d("ChatAdapter", "Left avatar url for ${chatMessage.senderID} = $url")

            // กันเคส url ว่าง
            if (url.isNullOrBlank()) {
                profileImage.setImageResource(R.drawable.ic_user)
                return
            }

            Glide.with(itemView.context)
                .load(url)
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.error)
                .circleCrop()
                .addListener(object: com.bumptech.glide.request.RequestListener<android.graphics.drawable.Drawable> {
                    override fun onLoadFailed(
                        e: com.bumptech.glide.load.engine.GlideException?,
                        model: Any?, target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, isFirstResource: Boolean
                    ): Boolean {
                        Log.e("ChatAdapter", "Glide load failed: $model", e)
                        return false
                    }
                    override fun onResourceReady(
                        resource: android.graphics.drawable.Drawable?, model: Any?,
                        target: com.bumptech.glide.request.target.Target<android.graphics.drawable.Drawable>?, dataSource: com.bumptech.glide.load.DataSource?, isFirstResource: Boolean
                    ): Boolean { return false }
                })
                .into(profileImage)


            // กดที่รูปเพื่อไปหน้าโปรไฟล์
            profileImage.setOnClickListener {
                Log.d("ChatAdapter", "Clicked on profile image of user: ${chatMessage.senderID}")
                val intent = Intent(itemView.context, OtherProfileActivity::class.java)
                intent.putExtra("userID", chatMessage.senderID)  // ส่ง userID ของผู้ส่งไปที่ OtherProfileActivity
                itemView.context.startActivity(intent)
            }
        }
    }

    inner class RightChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatMessage: ChatMessage) {
            val decoded = decodeIfDoubleEscaped(chatMessage.message)
            messageText.text = chatMessage.message
            messageText.text = EmojiCompat.get().process(decoded)
        }
    }
}
