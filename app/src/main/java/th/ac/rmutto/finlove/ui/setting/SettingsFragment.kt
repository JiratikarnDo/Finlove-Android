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


class SettingsFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_settings, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val ageSlider = view.findViewById<RangeSlider>(R.id.ageRangeSlider)
        val ageValueText = view.findViewById<TextView>(R.id.ageValueText)

        val initialMin = 21f
        val initialMax = 40f
        ageSlider.setValues(initialMin, initialMax)
        ageValueText.text = "${initialMin.toInt()} - ${initialMax.toInt()}"

        // ตั้งสี track และ thumb
        ageSlider.trackActiveTintList = ColorStateList.valueOf(Color.parseColor("#FE6AA6"))
        ageSlider.trackInactiveTintList = ColorStateList.valueOf(Color.TRANSPARENT)
        ageSlider.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FE6AA6"))

        ageSlider.addOnChangeListener { slider, _, _ ->
            val values = slider.values
            val minAge = values[0].toInt()
            val maxAge = values[1].toInt()
            ageValueText.text = "$minAge - $maxAge"
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
