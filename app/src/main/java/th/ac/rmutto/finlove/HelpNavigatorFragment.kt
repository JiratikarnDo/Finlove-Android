package th.ac.rmutto.finlove.ui.help   // <-- ปรับให้ตรงโฟลเดอร์/แพ็กเกจจริง

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextSwitcher
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels        // <-- สำคัญ
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.animation.AnimatorSet
import android.widget.ImageView
import th.ac.rmutto.finlove.utils.AnimationHelper
import th.ac.rmutto.finlove.R               // <-- ใช้ R ของแอป
import th.ac.rmutto.finlove.HelpNavigatorViewModel  // <-- ปรับ path ให้ตรงไฟล์จริง


class HelpNavigatorFragment : Fragment() {

    private val vm: HelpNavigatorViewModel by viewModels()

    private lateinit var txtQuestion: TextSwitcher
    private lateinit var imgMascot: ImageView
    private var mascotAnim: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // เปลี่ยนเป็นชื่อ layout จริงของคุณ (ไฟล์ที่มี TextSwitcher id=txtQuestion)
        val v = inflater.inflate(R.layout.fragment_reccomendprofile, container, false)
        imgMascot = v.findViewById(R.id.imgMascot)   // <- เพิ่มบรรทัดนี้
        txtQuestion = v.findViewById(R.id.txtQuestion)
        txtQuestion.setFactory {
            TextView(requireContext()).apply {
                // ใส่สไตล์เทียบกับ XML เดิม
                setPadding(18, 18, 18, 18)
                textSize = 20f
            }
        }
        txtQuestion.inAnimation = AlphaAnimation(0f, 1f).apply { duration = 250 }
        txtQuestion.outAnimation = AlphaAnimation(1f, 0f).apply { duration = 250 }

        // ปุ่ม (ถ้ามีใน layout เดียวกัน)
        v.findViewById<View>(R.id.btnHelpNow)?.setOnClickListener {
            // ทำอะไรก็ว่าไป เช่นไปหน้า HelpCenter
        }
        v.findViewById<View>(R.id.btnNotNow)?.setOnClickListener {
            vm.nextNow()
        }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        viewLifecycleOwner.lifecycleScope.launch {
// เริ่มอนิเมชันแบบผูก lifecycle (แนะนำ)
            mascotAnim = AnimationHelper.attachMascotIdle(viewLifecycleOwner, imgMascot)

            // แตะมาสคอตให้เด้งเล่น ๆ
            imgMascot.setOnClickListener { AnimationHelper.boing(it) }
            // กลับหน้าก่อนหน้าเมื่อกด "ไว้ทีหลัง!"
            view.findViewById<View>(R.id.btnNotNow)?.setOnClickListener {
                // ถ้าโปรเจกต์นี้ยังไม่ใช้ Navigation Component:
                requireActivity().onBackPressedDispatcher.onBackPressed()
            }

            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                // เริ่มหมุนข้อความ (ใส่ userId จริงถ้ามี)
                vm.start(userId = null, intervalMs = 6000L)

                launch {
                    vm.question.collect { txt ->
                        txtQuestion.setText(txt)
                    }
                }
            }
        }

        // แตะเพื่อข้ามไปข้อความถัดไปทันที
        txtQuestion.setOnClickListener { vm.nextNow() }
    }
    override fun onDestroyView() {
        super.onDestroyView()
        AnimationHelper.stopAndReset(imgMascot, mascotAnim)
        mascotAnim = null
    }

    override fun onStop() {
        super.onStop()
        vm.stop()
    }
}
