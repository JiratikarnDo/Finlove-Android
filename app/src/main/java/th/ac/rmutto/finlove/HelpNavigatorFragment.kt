package th.ac.rmutto.finlove.ui.help

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.widget.TextSwitcher
import android.widget.TextView
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch
import android.animation.AnimatorSet
import th.ac.rmutto.finlove.R
import th.ac.rmutto.finlove.HelpNavigatorViewModel
import th.ac.rmutto.finlove.UiState
import th.ac.rmutto.finlove.utils.AnimationHelper
import androidx.recyclerview.widget.RecyclerView
import androidx.core.view.isVisible
import androidx.core.view.isInvisible
import androidx.navigation.fragment.findNavController
import th.ac.rmutto.finlove.BubbleAction

class HelpNavigatorFragment : Fragment() {

    private val vm: HelpNavigatorViewModel by viewModels()

    private lateinit var txtQuestion: TextSwitcher
    private lateinit var imgMascot: ImageView
    private lateinit var btnHelpNow: View
    private lateinit var btnNotNow: View
    private var rvResult: RecyclerView? = null
    private lateinit var bubbleAdapter: BubbleAdapter
    private var mascotAnim: AnimatorSet? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val v = inflater.inflate(R.layout.fragment_reccomendprofile, container, false)

        // ✅ ย้ายมาไว้ในบอดี้ — ตั้ง baseUrl ที่นี่
        vm.baseUrl = getString(R.string.root_url).trimEnd('/')

        btnHelpNow = v.findViewById(R.id.btnHelpNow)
        btnNotNow  = v.findViewById(R.id.btnNotNow)
        imgMascot  = v.findViewById(R.id.imgMascot)
        txtQuestion = v.findViewById(R.id.txtQuestion)

        txtQuestion.setFactory {
            TextView(requireContext()).apply {
                setPadding(18, 18, 18, 18)
                textSize = 20f
                // setBackgroundResource(R.drawable.bg_speech_bubble_down)
            }
        }
        txtQuestion.inAnimation = AlphaAnimation(0f, 1f).apply { duration = 250 }
        txtQuestion.outAnimation = AlphaAnimation(1f, 0f).apply { duration = 250 }

        // ปุ่ม
        btnHelpNow.setOnClickListener {
            val userId = requireActivity().intent.getIntExtra("userID", -1).takeIf { it != -1 }
            vm.confirm(userId) // เริ่ม Loading → Result (ยิง API จริงแล้ว)
        }
        btnNotNow.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        bubbleAdapter = BubbleAdapter { action ->
            when (action) {
                is BubbleAction.OpenProfileSection -> {
                    val args = Bundle().apply {
                        putBoolean("edit_mode", true)            // จะเปิดโหมดแก้ไข (ถ้าต้องการ)
                        putString("open_section", action.section) // เช่น "bio"
                    }
                    findNavController().navigate(R.id.navigation_profile, args)
                }
                else -> Unit
            }
        }

        rvResult = v.findViewById<RecyclerView>(R.id.rvResult).apply {
            adapter = bubbleAdapter
            // ✅ ต้องมี layoutManager ไม่งั้น RecyclerView ไม่แสดง
            layoutManager = androidx.recyclerview.widget.LinearLayoutManager(requireContext())
            visibility = View.GONE
        }

        return v
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // มาสคอตแอนิเมชัน
        mascotAnim = AnimationHelper.attachMascotIdle(viewLifecycleOwner, imgMascot)
        imgMascot.setOnClickListener { AnimationHelper.boing(it) }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // 1) สลับ UI ตามสถานะ
                launch {
                    vm.ui.collect { state ->
                        when (state) {
                            is UiState.Intro -> {
                                txtQuestion.isVisible = true
                                btnHelpNow.isVisible = true
                                btnNotNow.isVisible  = true
                                rvResult?.isVisible  = false
                                vm.start(userId = null, intervalMs = 6000L)
                            }
                            is UiState.Loading -> {
                                btnHelpNow.isInvisible = true
                                btnNotNow.isInvisible  = true
                                rvResult?.isVisible  = false
                                txtQuestion.isVisible = true
                                txtQuestion.setText(state.dots)
                            }
                            is UiState.Result -> {
                                txtQuestion.isVisible = false
                                btnHelpNow.isInvisible = true
                                btnNotNow.isVisible  = true
                                rvResult?.isVisible  = true
                                bubbleAdapter.submitList(state.messages)
                            }
                        }
                    }
                }

                // 2) หมุนคำถามเฉพาะตอนอยู่ Intro
                launch {
                    vm.question.collect { txt ->
                        if (vm.ui.value is UiState.Intro) {
                            txtQuestion.setText(txt)
                        }
                    }
                }
            }
        }

        // แตะเพื่อข้ามไปคำถามถัดไป (เฉพาะ Intro)
        txtQuestion.setOnClickListener {
            if (vm.ui.value is UiState.Intro) vm.nextNow()
        }
    }

    override fun onDestroyView() {
        rvResult?.adapter = null
        rvResult = null
        AnimationHelper.stopAndReset(imgMascot, mascotAnim)
        mascotAnim = null
        super.onDestroyView()
    }

    override fun onStop() {
        super.onStop()
        vm.stop()
    }
}
