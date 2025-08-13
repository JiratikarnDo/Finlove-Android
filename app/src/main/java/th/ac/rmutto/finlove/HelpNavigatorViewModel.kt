package th.ac.rmutto.finlove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class HelpNavigatorViewModel(
    private val repo: HelpNavigatorRepository = HelpNavigatorRepository()
) : ViewModel() {

    private val _question = MutableStateFlow("มีปัญหาหรือ?")
    val question: StateFlow<String> = _question

    private var isRotating = false
    private var currentIndex = 0
    private var pool: List<String> = listOf(
        "รู้สึกคนแมตกับคุณน้อยหรอ?",
        "มีคนให้กดไลค์น้อยรึป่าว?",
        "มีปัญหาอยู่ใช่ไหม?",
        "อยากรับคำแนะนำโปรไฟล์แบบเร็ว ๆ ไหม?"
    )

    fun start(userId: Int?, intervalMs: Long = 4000L) {
        if (isRotating) return
        isRotating = true

        viewModelScope.launch {
            // กำหนดชนิดให้ชัดเจน เพื่อตัดปัญหา Nothing?/ambiguous
            val remote: List<String>? = try {
                repo.fetchQuestions(userId)
            } catch (_: Exception) {
                null
            }

            if (!remote.isNullOrEmpty()) {
                pool = remote
                currentIndex = 0
            }

            while (isRotating) {
                if (pool.isEmpty()) {
                    _question.value = "มีปัญหาหรือ?"
                } else {
                    _question.value = pool[currentIndex % pool.size]
                    currentIndex++
                }
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
        if (questions.isNotEmpty()) {
            pool = questions
            currentIndex = 0
        }
    }
}
