package th.ac.rmutto.finlove

import kotlinx.coroutines.delay

class HelpNavigatorRepository {
    // ต้องเป็น suspend และ "คืนค่า List<String>"
    suspend fun fetchQuestions(userId: Int?): List<String> {
        // TODO: ภายหลังค่อยเปลี่ยนเป็นเรียก API จริง
        delay(200)
        return listOf(
            "อยากอัปเดตรูปโปรไฟล์ไหม?",
            "เพิ่มความสนใจเพื่อเจอเมตช์ที่ดีขึ้นไหม?",
            "เปิด GPS เพื่อหาเมตช์ใกล้คุณไหม?"
        )
        // ถ้าจะให้ว่างก็คืน emptyList() — สำคัญคือต้องเป็น List<String>
    }
}
