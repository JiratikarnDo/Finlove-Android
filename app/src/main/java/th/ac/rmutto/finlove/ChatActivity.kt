package th.ac.rmutto.finlove

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import th.ac.rmutto.finlove.databinding.ActivityChatBinding
import androidx.navigation.findNavController
import androidx.navigation.fragment.NavHostFragment
import th.ac.rmutto.finlove.ui.Dateing.DatingPlaceFragment



class ChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityChatBinding
    private val client = OkHttpClient()
    private var matchID: Int = -1
    private var senderID: Int = -1
    private var receiverNickname: String = ""
    private var isBlocked: Boolean = false
    private var isBannerClosed = false
    private var isInitialLoad = true
    private var lastMessageCount = 0


    private val handler = Handler(Looper.getMainLooper())
    private val refreshInterval = 2000L // รีเฟรชทุก 2 วินาที
    private val refreshRunnable = object : Runnable {
        override fun run() {
            fetchChatMessages() // เรียกใช้การอัปเดตข้อความ
            handler.postDelayed(this, refreshInterval) // ตั้งเวลาเรียกใช้อีกครั้ง
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // รับ matchID, senderID, และ nickname ของคู่สนทนา
        matchID = intent.getIntExtra("matchID", -1)
        senderID = intent.getIntExtra("senderID", -1)
        receiverNickname = intent.getStringExtra("nickname") ?: ""

        Log.d("ChatActivity", "Received matchID: $matchID, senderID: $senderID, nickname: $receiverNickname")

        if (matchID == -1 || senderID == -1) {
            Toast.makeText(this, "ไม่พบข้อมูลการสนทนา", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        // ตั้งค่า Toolbar ให้แสดงชื่อเล่นของคู่สนทนา
        setSupportActionBar(binding.toolbar)
        supportActionBar?.title = receiverNickname
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.toolbar.setNavigationOnClickListener {
            finish()
        }
        binding.btnCloseBanner.setOnClickListener {
            binding.popupBanner.visibility = View.GONE
            isBannerClosed = true // จำว่าเคยปิดไปแล้ว
        }

        // เพิ่มโค้ด popupbanner ตรงนี้ได้
        binding.txtLinkMore.setOnClickListener {
            binding.popupBanner.visibility = View.GONE
            isBannerClosed = true

            // ปิด scroll ชั่วคราว
            binding.recyclerViewChat.layoutManager = object : LinearLayoutManager(this) {
                override fun canScrollVertically(): Boolean = false
            }

            // ✅ ส่งข้อมูลไปด้วย
            val intent = Intent(this, DatingPlaceActivity::class.java)
            intent.putExtra("matchID", matchID)
            intent.putExtra("senderID", senderID)
            intent.putExtra("nickname", receiverNickname)
            startActivity(intent)
        }

        // เพิ่มโค้ด popupbanner ตรงนี้ได้
        // เพิ่มโค้ด popupbanner ตรงนี้ได้

        // ตั้งค่า RecyclerView
        val chatAdapter = ChatAdapter(senderID)
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        binding.recyclerViewChat.adapter = chatAdapter

        fetchChatMessages()



        // เมื่อผู้ใช้ส่งข้อความ
        binding.sendButton.setOnClickListener {
            val message = binding.messageInput.text.toString().trim()
            if (message.isNotEmpty()) {
                sendMessage(message)
                binding.messageInput.text.clear()
            }
        }

        // กำหนดฟังก์ชันให้กับปุ่ม Block และ Unblock
        binding.toolbar.findViewById<Button>(R.id.buttonBlockChat).setOnClickListener {
            blockChat()
        }
        binding.toolbar.findViewById<Button>(R.id.buttonUnblockChat).setOnClickListener {
            unblockChat()
        }
        // ตรวจสอบว่ามีข้อความอัตโนมัติไหม
        intent.getStringExtra("autoMessage")?.let { message ->
            Log.d("ChatActivity", "✅ autoMessage received: $message")
            sendMessage(message)
        }

    }

    override fun onResume() {
        super.onResume()
        // คืนค่า layoutManager ให้ scroll ได้ตามปกติ
        isInitialLoad = true
        binding.recyclerViewChat.layoutManager = LinearLayoutManager(this)
        handler.post(refreshRunnable) // เริ่มการอัปเดตข้อความเมื่อ Activity กลับมาแสดง
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(refreshRunnable) // หยุดการอัปเดตข้อความเมื่อ Activity หยุดทำงาน
    }

    private fun blockChat() {
        if (isBlocked) {
            Toast.makeText(this, "คุณไม่สามารถบล็อคได้ เนื่องจากถูกบล็อกจากอีกฝ่าย", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/block-chat"
            val requestBody = FormBody.Builder()
                .add("userID", senderID.toString())
                .add("matchID", matchID.toString())
                .add("isBlocked", "1")
                .build()

            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isBlocked = true
                        Toast.makeText(this@ChatActivity, "บล็อกแชทเรียบร้อย", Toast.LENGTH_SHORT).show()
                        binding.toolbar.findViewById<Button>(R.id.buttonBlockChat).isEnabled = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "ไม่สามารถบล็อคแชทได้ ลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun unblockChat() {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/unblock-chat"
            val requestBody = FormBody.Builder()
                .add("userID", senderID.toString())
                .add("matchID", matchID.toString())
                .build()

            val request = Request.Builder().url(url).post(requestBody).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isBlocked = false
                        Toast.makeText(this@ChatActivity, "ปลดบล็อคแชทเรียบร้อย", Toast.LENGTH_SHORT).show()
                        binding.toolbar.findViewById<Button>(R.id.buttonBlockChat).isEnabled = true
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "ไม่สามารถปลดบล็อคแชทได้ ลองใหม่อีกครั้ง", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun fetchChatMessages() {
        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/chats/$matchID"
            val request = Request.Builder().url(url).build()

            try {
                val response = client.newCall(request).execute()
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    val messages = parseChatMessages(responseBody)
                    withContext(Dispatchers.Main) {
                        if (messages.isEmpty()) {
                            binding.emptyChatMessage.visibility = View.VISIBLE
                            binding.recyclerViewChat.visibility = View.GONE
                        } else {
                            binding.emptyChatMessage.visibility = View.GONE
                            binding.recyclerViewChat.visibility = View.VISIBLE
                            (binding.recyclerViewChat.adapter as ChatAdapter).setMessages(messages)
                            // ถ้าเป็นการโหลดครั้งแรก หรือมีข้อความใหม่เพิ่มขึ้น ให้ scroll ลงล่างสุด
                            if (isInitialLoad || messages.size > lastMessageCount) {
                                binding.recyclerViewChat.scrollToPosition(messages.size - 1)
                                isInitialLoad = false
                            }
                            lastMessageCount = messages.size
                        }
                        if (messages.size >= 15 && !isBannerClosed) {
                            binding.popupBanner.visibility = View.VISIBLE
                        } else {
                            binding.popupBanner.visibility = View.GONE
                        }
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(this@ChatActivity, "ไม่สามารถดึงข้อมูลการสนทนาได้", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun sendMessage(message: String) {
        if (isBlocked) {
            Toast.makeText(this, "คุณไม่สามารถส่งข้อความได้ เนื่องจากคุณถูกบล็อก", Toast.LENGTH_SHORT).show()
            return
        }

        lifecycleScope.launch(Dispatchers.IO) {
            val url = getString(R.string.root_url) + "/api_v2/chats/$matchID"
            val requestBody = FormBody.Builder()
                .add("senderID", senderID.toString())
                .add("message", message)
                .build()

            val request = Request.Builder()
                .url(url)
                .post(requestBody)
                .build()

            try {
                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        if (response.code == 403) {
                            Toast.makeText(this@ChatActivity, "คุณถูกบล็อกจากการส่งข้อความในแชทนี้", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(this@ChatActivity, "ไม่สามารถส่งข้อความได้", Toast.LENGTH_SHORT).show()
                        }
                    }
                } else {
                    fetchChatMessages()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ChatActivity, "เกิดข้อผิดพลาด: ${e.message}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    private fun parseChatMessages(responseBody: String?): List<ChatMessage> {
        val messages = mutableListOf<ChatMessage>()
        responseBody?.let {
            try {
                val jsonObject = JSONObject(it)
                val messagesArray = jsonObject.getJSONArray("messages")

                for (i in 0 until messagesArray.length()) {
                    val messageObject = messagesArray.getJSONObject(i)
                    val chatMessage = ChatMessage(
                        messageObject.getInt("senderID"),
                        messageObject.getString("nickname"),
                        messageObject.getString("imageFile"),
                        messageObject.getString("message"),
                        messageObject.getString("timestamp")
                    )
                    messages.add(chatMessage)
                }
            } catch (e: Exception) {
                Log.e("ChatActivity", "Error parsing chat messages: ${e.message}")
            }
        }
        return messages
    }
}

// Data class for storing chat message data
data class ChatMessage(
    val senderID: Int,
    val nickname: String,
    val profilePicture: String,
    val message: String,
    val timestamp: String
)
