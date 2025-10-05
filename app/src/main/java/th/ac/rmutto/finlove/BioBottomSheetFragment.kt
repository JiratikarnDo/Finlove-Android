package th.ac.rmutto.finlove

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import th.ac.rmutto.finlove.R
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import th.ac.rmutto.finlove.databinding.FragmentBioBinding
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone   // ✅ ต้อง import ด้วย

class BioBottomSheetFragment : BottomSheetDialogFragment() {

    private var _binding: FragmentBioBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentBioBinding.inflate(inflater, container, false)

        // ✅ ดึงจาก arguments
        val nickname = arguments?.getString("nickname") ?: "ไม่ระบุชื่อ"
        val dateBirth = arguments?.getString("dateBirth") ?: ""
        val verify = arguments?.getInt("verify") ?: 0
        val imageUrl = arguments?.getString("profilePicture")
        val bioText = arguments?.getString("bio")
            ?.takeIf { !it.isNullOrBlank() && !it.equals("null", ignoreCase = true) }
            ?: "ไม่ระบุ"

        // ✅ คำนวณอายุ (คงโครงสร้างเดิมไว้ แต่เพิ่ม pattern/locale/timezone)
        val age = if (dateBirth.isNotEmpty()) {
            var dob: Date? = null
            val patterns = listOf( // ★ รองรับหลายรูปแบบมากขึ้น
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",   // Z หรือ +07:00
                "yyyy-MM-dd'T'HH:mm:ssX",       // Z หรือ +07:00
                "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", // literal 'Z'
                "yyyy-MM-dd'T'HH:mm:ss'Z'",     // literal 'Z'
                "yyyy-MM-dd",
                "yyyy-MM-dd HH:mm:ss",
                "dd/MM/yyyy",
                "dd-MM-yyyy",
                "EEE, dd MMM yyyy HH:mm:ss z"   // ต้องใช้ Locale อังกฤษ
            )
            for (p in patterns) {
                try {
                    val loc = if (p.contains("MMM") || p.contains("EEE")) Locale.ENGLISH else Locale.getDefault() // ★
                    val sdf = SimpleDateFormat(p, loc)
                    // ★ บังคับ UTC เฉพาะแบบ literal 'Z' เพื่อไม่ทับ offset จริง
                    if (p.endsWith("'Z'")) sdf.timeZone = TimeZone.getTimeZone("UTC")
                    dob = sdf.parse(dateBirth)
                    if (dob != null) break
                } catch (_: Exception) { }
            }
            if (dob != null) {
                val today = Calendar.getInstance()
                val cal = Calendar.getInstance().apply { time = dob }
                // ตัวเดิมของคุณ (คงไว้)
                var ageVal = today.get(Calendar.YEAR) - cal.get(Calendar.YEAR)
                if (today.get(Calendar.DAY_OF_YEAR) < cal.get(Calendar.DAY_OF_YEAR)) ageVal--
                ageVal
            } else null
        } else null

        val ageText = if (age != null && age >= 0) {
            "$age ปี ${if (verify == 1) "✓" else ""}"
        } else {
            "ไม่ทราบอายุ"
        }


        // ✅ ใส่ค่า UI
        binding.textNickname.text = nickname
        binding.textAgeVerify.text = ageText
        // 👇 ตรงนี้แหละ เพิ่ม fallback
        binding.textBio.text = if (bioText.isBlank()) {
            "ไม่ระบุ"
        } else {
            bioText
        }

        // ✅ โหลดรูป
        if (!imageUrl.isNullOrBlank()) {
            Glide.with(this)
                .load(imageUrl)
                .placeholder(R.drawable.ic_user)
                .circleCrop()
                .into(binding.imageProfile)
        } else {
            binding.imageProfile.setImageResource(R.drawable.ic_user)
        }
// ปุ่มปิด
        binding.dismissButton.setOnClickListener {
            dismiss()  // ปิด BottomSheet
        }
        return binding.root   // ✅ สำคัญ!
    }

    override fun onStart() {
        super.onStart()
        val dialog = dialog as BottomSheetDialog
        val bottomSheet = dialog.findViewById<View>(
            com.google.android.material.R.id.design_bottom_sheet
        )

        bottomSheet?.setBackgroundResource(android.R.color.transparent)

        bottomSheet?.let {
            val behavior = BottomSheetBehavior.from(it)
            behavior.state = BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        fun newInstance(
            userID: Int,
            nickname: String,
            profilePicture: String?,
            dateBirth: String,
            verify: Int,
            preferences: List<String>,
            bio: String?
        ): BioBottomSheetFragment {
            val fragment = BioBottomSheetFragment()
            val args = Bundle().apply {
                putInt("userID", userID)
                putString("nickname", nickname)
                putString("profilePicture", profilePicture)
                putString("dateBirth", dateBirth)  // ✅ ใช้ d เล็ก
                putInt("verify", verify)
                putStringArrayList("preferences", ArrayList(preferences))
                putString("bio", bio ?: "ไม่มีข้อมูล Bio")
            }
            fragment.arguments = args
            return fragment
        }
    }
}
