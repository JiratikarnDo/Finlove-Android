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

    override fun getItemCount(): Int = messages.size

    inner class LeftChatViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val profileImage: ImageView = itemView.findViewById(R.id.profile_image)
        private val messageText: TextView = itemView.findViewById(R.id.message_text)

        fun bind(chatMessage: ChatMessage) {
            messageText.text = chatMessage.message
            Glide.with(itemView.context)
                .load(chatMessage.profilePicture)
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
            messageText.text = chatMessage.message
        }
    }
}
