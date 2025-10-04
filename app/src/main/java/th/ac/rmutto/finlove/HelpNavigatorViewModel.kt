package th.ac.rmutto.finlove

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject

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

    // NEW: ViewModel ไม่มี Context ห้าม getString ที่นี่
    var baseUrl: String =
        ""                 // ให้ Fragment เซ็ตค่าให้ เช่น vm.baseUrl = getString(R.string.root_url).trimEnd('/')
    private val client = OkHttpClient()      // NEW

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
            if (!remote.isNullOrEmpty()) {
                pool = remote; currentIndex = 0
            }

            while (isRotating && _ui.value is UiState.Intro) {
                _question.value = if (pool.isEmpty()) "มีปัญหาหรือ?"
                else pool[currentIndex % pool.size]
                currentIndex++
                delay(intervalMs)
            }
        }
    }

    fun stop() {
        isRotating = false
    }

    fun nextNow() {
        if (pool.isEmpty()) return
        currentIndex++
        _question.value = pool[currentIndex % pool.size]
    }

    fun setPool(questions: List<String>) {
        if (questions.isNotEmpty()) {
            pool = questions; currentIndex = 0
        }
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

    private fun stopDots() {
        dotsJob?.cancel(); dotsJob = null
    }

    /** เรียกเมื่อกด "ใช่แล้ว!" */
    fun confirm(userId: Int?) {
        stop()
        _ui.value = UiState.Loading()
        startDots()

        viewModelScope.launch {
            // หน่วงให้รู้สึกกำลังคิด (เอาออกได้)
            delay(1200)

            val generic = listOf(
                BubbleMessage("เหมือนคุณจะพลาดบางอย่างไป", BubbleType.GENERIC),
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

    // CHANGED: ตรงนี้ยิง API จริง แล้ว map → BubbleMessage (ไม่เปลี่ยน signature/flow)
    private suspend fun fetchPersonalRecommendations(userId: Int?): List<BubbleMessage> {
        if (userId == null) {
            return listOf(BubbleMessage("ไม่พบรหัสผู้ใช้ (userId)", BubbleType.GENERIC))
        }
        if (baseUrl.isBlank()) {
            return listOf(BubbleMessage("baseUrl ยังไม่ถูกตั้งค่า", BubbleType.GENERIC))
        }

        val url = "${baseUrl.trimEnd('/')}/api_v2/profile/recommend/$userId"
        val req = Request.Builder()
            .url(url)
            .get()
            .addHeader("Accept", "application/json")
            .build()

        val body = withContext(Dispatchers.IO) {
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) {
                    val err = resp.body?.string().orEmpty()
                    throw RuntimeException("HTTP ${resp.code}: ${err.take(200)}")
                }
                resp.body?.string().orEmpty()
            }
        }

        return parseRecommendToBubbles(body)
    }

    // 3) แปลง JSON → Bubble+CTA (dedupe ต่อ section)
    private fun parseRecommendToBubbles(jsonText: String): List<BubbleMessage> {
        val out = mutableListOf<BubbleMessage>()
        val obj = JSONObject(jsonText)

        // header
        val score = obj.optInt("score", -1)
        val completeness = obj.optString("completeness", "")
        if (score >= 0 || completeness.isNotBlank()) {
            val head = buildString {
                if (completeness.isNotBlank()) append("ความสมบูรณ์โปรไฟล์: $completeness")
                if (score >= 0) {
                    if (isNotEmpty()) append(" "); append("(คะแนน $score)")
                }
            }
            if (head.isNotBlank()) out += BubbleMessage(head, BubbleType.GENERIC)
        }

        val seenSections = mutableSetOf<String>()
        val recs = obj.optJSONArray("recommendations")

        if (recs != null && recs.length() > 0) {
            for (i in 0 until recs.length()) {
                val txt = recs.optString(i).trim()

                // ตรงคำก่อน (แม่นสุด)
                val direct = phraseMap.firstOrNull { txt.contains(it.first, ignoreCase = true) }
                val section = direct?.second?.first ?: guessSection(txt)
                val cta = direct?.second?.second ?: " แก้ไข"

                if (section.isNotBlank() && seenSections.add(section)) {
                    out += BubbleMessage(
                        text = txt,
                        type = BubbleType.PERSONAL,
                        ctaText = cta,
                        ctaAction = BubbleAction.OpenProfileSection(section)
                    )
                } else {
                    // ถ้าซ้ำ section เดิมแล้ว ให้แสดงเป็นข้อความธรรมดา (ไม่ต้อง CTA ซ้ำ)
                    out += BubbleMessage(txt, BubbleType.GENERIC)
                }
            }
        } else {
            out += BubbleMessage(
                "โปรไฟล์คุณดีมากแล้ว ลองเพิ่มรูปหรือยืนยันตัวตนเพื่อโอกาสที่ดีกว่า",
                BubbleType.GENERIC
            )
        }

        // breakdown (ออปชัน)
        obj.optJSONObject("breakdown")?.let { bd ->
            val it = bd.keys()
            while (it.hasNext()) {
                val key = it.next()
                val item = bd.optJSONObject(key) ?: continue
                val ok = item.optBoolean("ok")
                val tip = item.optString("tip")
                if (!ok && tip.isNotBlank()) {
                    out += BubbleMessage(
                        text = "• $tip",
                        type = BubbleType.PERSONAL,
                        ctaText = "แก้ไข",
                        ctaAction = BubbleAction.OpenProfileSection(guessSection(tip))
                    )
                }
            }
        }

        return out
    }

    // NEW: เดา section จากข้อความ เพื่อให้ปุ่ม "แก้ไข" พาไปถูกจุด
    // 1) แม็พ "ข้อความจาก API" → (section, CTA)
    private val phraseMap: List<Pair<String, Pair<String, String>>> = listOf(
        "เพิ่มรูปโปรไฟล์" to ("photo" to " เพิ่มรูป"),
        "เพิ่มชื่อเล่น" to ("nickname" to " ตั้งชื่อเล่น"),
        "ยืนยันตัวตน" to ("verify" to " ยืนยัน"),
        "กรอกข้อมูลพื้นฐานให้ครบ" to ("profile" to " แก้ไข"),
        "เลือกเป้าหมายความสัมพันธ์" to ("goal" to " ตั้งค่า"),
        "เลือกความชอบอย่างน้อย 3 ข้อ" to ("preferences" to " เลือกเพิ่ม"),
        "เขียนคำแนะนำตัว (Bio) ให้ยาวขึ้น" to ("bio" to " แก้ไข"),
        "กรุณาระบุที่อยู่เบื้องต้น"               to ("home"           to " แก้ไข"),
        "ระบุบ้าน/เขตที่อยู่"                      to ("home"           to " แก้ไข"),
        "กรุณาระบุจังหวัด"                         to ("province"       to " แก้ไข"),
        "ระบุจังหวัด"                              to ("province"       to " แก้ไข"),
        "เปิดแชร์ตำแหน่ง (Location)"               to ("location"       to " เปิดตำแหน่ง"),
        "ระบุระดับการศึกษา"                        to ("education"      to " ระบุ"),
        "เพิ่มข้อมูลการศึกษา"                      to ("education"      to " ระบุ"),
        "ระบุอาชีพ"                                 to ("career"         to " ระบุ"),
        "เพิ่มข้อมูลอาชีพ"                          to ("career"         to " ระบุ"),
        "กรอกชื่อ-นามสกุล"                         to ("profile"        to " แก้ไข"),
        "ระบุเพศ"                                   to ("gender"         to " ตั้งค่า"),
        "ระบุวันเกิด"                               to ("birthday"       to " เลือกวันที่")
    )

    // 2) fallback regex ถ้าอนาคตข้อความเปลี่ยนเล็กน้อย
    private val sectionPatterns: List<Pair<String, Regex>> = listOf(
        "bio"            to Regex("bio|คำแนะนำตัว|แนะนำตัว", RegexOption.IGNORE_CASE),
        "photo"          to Regex("รูป|ภาพ|แกลเลอ", RegexOption.IGNORE_CASE),
        "verify"         to Regex("ยืนยัน|verify|ตัวตน|บัตร", RegexOption.IGNORE_CASE),
        "preferences"    to Regex("ความชอบ|ความสนใจ|interest|เลือก.*(3|สาม).*", RegexOption.IGNORE_CASE),
        "home"           to Regex("บ้าน|ที่อยู่|เขต", RegexOption.IGNORE_CASE),
        "province"       to Regex("จังหวัด", RegexOption.IGNORE_CASE),
        "location"       to Regex("ตำแหน่ง|location|gps|พิกัด|แชร์ตำแหน่ง", RegexOption.IGNORE_CASE),
        "goal"           to Regex("เป้าหมาย|goal", RegexOption.IGNORE_CASE),
        "interestGender" to Regex("เพศที่สนใจ|gender", RegexOption.IGNORE_CASE),
        "nickname"       to Regex("ชื่อเล่น|nickname", RegexOption.IGNORE_CASE),
        "education"      to Regex("การศึกษา|วุฒิ|education|ระดับการศึกษา", RegexOption.IGNORE_CASE),
        "career"         to Regex("อาชีพ|งาน|career", RegexOption.IGNORE_CASE),
        "gender"         to Regex("เพศ(?!ที่สนใจ)|gender", RegexOption.IGNORE_CASE),
        "birthday"       to Regex("วันเกิด|date ?of ?birth|dob", RegexOption.IGNORE_CASE),
        "profile"        to Regex("ข้อมูลพื้นฐาน|ชื่อ.*สกุล|ชื่อจริง|นามสกุล|basic", RegexOption.IGNORE_CASE)
    )

    private fun guessSection(text: String): String {
        phraseMap.firstOrNull { text.contains(it.first, ignoreCase = true) }
            ?.let { return it.second.first }
        val t = text.lowercase()
        return sectionPatterns.firstOrNull { it.second.containsMatchIn(t) }?.first ?: "profile"
    }
}

