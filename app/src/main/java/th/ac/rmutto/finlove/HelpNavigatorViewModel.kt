package th.ac.rmutto.finlove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class BubbleType { GENERIC, PERSONAL }
// ✔ เพิ่มชนิดแอ็กชัน
sealed class BubbleAction {
    object OpenProfile : BubbleAction()
    data class OpenProfileSection(val section: String) : BubbleAction()
}
// ✔ เพิ่มฟิลด์ ctaText / ctaAction
data class BubbleMessage(
    val text: String,
    val type: BubbleType,
    val ctaText: String? = null,
    val ctaAction: BubbleAction? = null
)
sealed class UiState {
    object Intro : UiState()
    data class Loading(val dots: String = "•") : UiState()
    data class Result(val messages: List<BubbleMessage>) : UiState()
}

class HelpNavigatorViewModel : ViewModel() {

    // ---- UI State ----
    private val _ui = MutableStateFlow<UiState>(UiState.Intro)
    val ui: StateFlow<UiState> = _ui

    // ---- คำถามหมุนในหน้า Intro ----
    private val _question = MutableStateFlow("มีปัญหาหรือ?")
    val question: StateFlow<String> = _question

    private var isRotating = false
    private var currentIndex = 0
    private var pool: List<String> = listOf(
        "รู้สึกคนแมตกับคุณน้อยหรอ?",
        "มีคนกดไลค์น้อยรึเปล่า?",
        "มีปัญหาอยู่ใช่ไหม?",
        "อยากรับคำแนะนำโปรไฟล์แบบเร็ว ๆ ไหม?"
    )

    fun start(userId: Int?, intervalMs: Long = 4000L) {
        if (isRotating) return
        if (_ui.value !is UiState.Intro) return
        isRotating = true

        viewModelScope.launch {
            val remote = runCatching { fetchQuestions(userId) }.getOrNull()
            if (!remote.isNullOrEmpty()) { pool = remote; currentIndex = 0 }

            while (isRotating && _ui.value is UiState.Intro) {
                _question.value = if (pool.isEmpty()) "มีปัญหาหรือ?"
                else pool[currentIndex % pool.size]
                currentIndex++
                delay(intervalMs)
            }
        }
    }

    fun stop() { isRotating = false }

    fun nextNow() {
        if (pool.isEmpty()) return
        currentIndex++
        _question.value = pool[currentIndex % pool.size]
    }

    fun setPool(questions: List<String>) {
        if (questions.isNotEmpty()) { pool = questions; currentIndex = 0 }
    }

    // ---- Loading dots ----
    private var dotsJob: Job? = null
    private fun startDots() {
        dotsJob?.cancel()
        dotsJob = viewModelScope.launch {
            val frames = listOf("•", "••", "•••")
            var i = 0
            while (isActive && _ui.value is UiState.Loading) {
                _ui.value = UiState.Loading(frames[i % frames.size])
                i++; delay(350)
            }
        }
    }
    private fun stopDots() { dotsJob?.cancel(); dotsJob = null }

    /** เรียกเมื่อกด "ใช่แล้ว!" */
    fun confirm(userId: Int?) {
        stop()
        _ui.value = UiState.Loading()
        startDots()

        viewModelScope.launch {
            // หน่วงให้รู้สึกกำลังคิด (เอาออกได้)
            delay(1200)

            val generic = listOf(
                BubbleMessage("เหมือนคุณจะขาดบางข้อมูลทั่วไปที่จำเป็น", BubbleType.GENERIC),
                BubbleMessage("การขาดข้อมูลอาจทำให้คนอื่นเห็นคุณได้น้อยลง", BubbleType.GENERIC)
            )

            val personal = runCatching { fetchPersonalRecommendations(userId) }
                .getOrDefault(
                    listOf(
                        BubbleMessage(
                            text = "ลองเพิ่มความชอบให้ครบอย่าง ",
                            type = BubbleType.PERSONAL,
                            ctaText = "แก้ไข",
                            ctaAction = BubbleAction.OpenProfileSection("preferences")
                        ),
                        BubbleMessage(
                            text = "ยังไม่ได้เปิดตำแหน่ง ลองเปิดเพื่อหาเมตช์ใกล้ตัว ",
                            type = BubbleType.PERSONAL,
                            ctaText = "แก้ไข",
                            ctaAction = BubbleAction.OpenProfileSection("location")
                        )
                    )
                )

            val tail = listOf(BubbleMessage("ช่วยคุณได้แน่นอน !", BubbleType.GENERIC))

            stopDots()
            _ui.value = UiState.Result(generic + personal + tail)
        }
    }

    /** รีเซ็ตกลับ Intro เพื่อเริ่มหมุนใหม่ */
    fun resetToIntro() {
        stop(); stopDots()
        _ui.value = UiState.Intro
    }

    // ===============================
    //   ฟังก์ชันดึงข้อมูล (ไฟล์เดียว)
    // ===============================
    private suspend fun fetchQuestions(userId: Int?): List<String> {
        // TODO: เรียก API จริง; ตอนนี้จำลอง network ไว้ก่อน
        delay(200)
        return listOf(
            "รู้สึกคนแมตกับคุณน้อยหรอ?",
            "มีคนกดไลค์น้อยรึเปล่า?",
            "มีปัญหาอยู่ใช่ไหม?",
            "อยากรับคำแนะนำโปรไฟล์แบบเร็ว ๆ ไหม?"
        )
    }

    private suspend fun fetchPersonalRecommendations(userId: Int?): List<BubbleMessage> {
        // TODO: เรียก API จริงแล้ว map เป็น BubbleMessage(type = PERSONAL)
        delay(300)
        return listOf(
            BubbleMessage("คุณยังเลือกสิ่งที่ชื่อชอบไม่ครบ 3อย่าง ลองเลือกเพิ่มสิ", type = BubbleType.PERSONAL,
                ctaText = "แก้ไข",
                ctaAction = BubbleAction.OpenProfileSection("preferences")
            ),
            BubbleMessage("ขอบเขตการค้นหาของคุณอาจจะใกล้เกินไป ลองเพิ่มดูสิ", type = BubbleType.PERSONAL,
                ctaText = "แก้ไข",
                ctaAction = BubbleAction.OpenProfileSection("location")
            )
        )
    }
}
