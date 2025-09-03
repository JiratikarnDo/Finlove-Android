package th.ac.rmutto.finlove.ui.setting

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import th.ac.rmutto.finlove.R
import android.content.Intent
import th.ac.rmutto.finlove.LoginActivity
import com.google.android.material.slider.RangeSlider
import android.content.res.ColorStateList
import android.graphics.Color
import android.widget.EditText
import th.ac.rmutto.finlove.ui.home.HomeFragment
import androidx.navigation.fragment.findNavController


class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    private val PREF_NAME = "FinLovePrefs"
    private val KEY_MIN_AGE = "age_min"
    private val KEY_MAX_AGE = "age_max"
    private val KEY_MAX_DISTANCE_KM = "max_distance_km"

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ageSlider = view.findViewById<RangeSlider>(R.id.ageRangeSlider)
        val ageValueText = view.findViewById<TextView>(R.id.ageValueText)
        val saveButton = view.findViewById<Button>(R.id.saveButton)   // ← ปุ่มบันทึก
        val distanceEdit = view.findViewById<EditText>(R.id.distanceStart)

        val prefs = requireContext().getSharedPreferences(PREF_NAME, android.content.Context.MODE_PRIVATE)

        val initialMin = 18
        val initialMax = 60
        // ← แก้: โหลดค่าที่เคยบันทึกไว้ (ถ้าไม่มี ใช้ค่า initial ของคุณ)
        val savedMin = prefs.getInt(KEY_MIN_AGE, initialMin)
        val savedMax = prefs.getInt(KEY_MAX_AGE, initialMax)

        // ตั้งค่าเริ่มต้นให้สไลเดอร์ + ข้อความ
        ageSlider.setValues(savedMin.toFloat(), savedMax.toFloat())
        ageValueText.text = "$savedMin - $savedMax"


        // ตั้งสี track และ thumb
        ageSlider.trackActiveTintList = ColorStateList.valueOf(Color.parseColor("#FE6AA6"))
        ageSlider.trackInactiveTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        ageSlider.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FE6AA6"))

        // ← แก้: อัปเดตข้อความ + บันทึกทุกครั้งที่เลื่อน
        ageSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minAge = values[0].toInt()
            val maxAge = values[1].toInt()
            ageValueText.text = "$minAge - $maxAge"
            // บันทึกลง prefs
            prefs.edit()
                .putInt(KEY_MIN_AGE, minAge)
                .putInt(KEY_MAX_AGE, maxAge)
                .apply()
        }

        // ----- ระยะทาง (กม.) -----
        // โหลดค่าที่เคยบันทึก; ถ้าไม่มี ใช้ 50 กม. เป็นค่าเริ่มต้น
        val savedMaxDistance = prefs.getFloat(KEY_MAX_DISTANCE_KM, 100f)
        distanceEdit.setText(
            if (savedMaxDistance % 1f == 0f) savedMaxDistance.toInt().toString()
            else savedMaxDistance.toString()
        )

        // กด "บันทึก" → เก็บลง prefs
        saveButton.setOnClickListener {
            val minAge = ageSlider.values[0].toInt()
            val maxAge = ageSlider.values[1].toInt()

            // ระยะทาง
            val kmText = distanceEdit.text?.toString()?.trim()
            val km = kmText?.toFloatOrNull()
            if (km == null || km <= 0f) {
                Toast.makeText(requireContext(), "กรุณาใส่ระยะทางมากกว่า 0 กม.", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // กันค่ามากเกินจริง เช่น > 500 กม.
            val MAX_ALLOWED_KM = 100000f
            val kmClamped = km.coerceIn(1f, MAX_ALLOWED_KM)

            // ✅ อัปเดตกลับไปที่ EditText ให้เห็นค่าที่ถูก clamp แล้ว
            distanceEdit.setText(
                if (kmClamped % 1f == 0f) kmClamped.toInt().toString() else kmClamped.toString()
            )
            prefs.edit()
                .putInt(KEY_MIN_AGE, minAge)
                .putInt(KEY_MAX_AGE, maxAge)
                .putFloat(KEY_MAX_DISTANCE_KM, kmClamped)
                .apply()

            // เด้ง Popup
            androidx.appcompat.app.AlertDialog.Builder(requireContext())
                .setTitle("ตั้งค่าสำเร็จ")
                .setMessage("คุณจะเจอคนอายุ $minAge-$maxAge ปี และห่างจากคุณไม่เกิน ${kmClamped} กม.")
                .setPositiveButton("ตกลง") { d, _ -> d.dismiss()
                }
                .show()   // <<<<<< ต้องมี
        }

        val logoutButton = view.findViewById<Button>(R.id.logoutButton)
        logoutButton.setOnClickListener {
            val userId = requireActivity().intent.getIntExtra("userID", -1)
            logoutUser(userId)
        }
    }

    private fun logoutUser(userId: Int) {
        // ตัวอย่างการ Logout
        Toast.makeText(requireContext(), "ออกจากระบบสำเร็จ", Toast.LENGTH_SHORT).show()

        val intent = Intent(requireContext(), LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
    }
}
