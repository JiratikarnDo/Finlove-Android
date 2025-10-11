// LoadingDialogFragment.kt
package th.ac.rmutto.finlove

import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.os.Bundle
import android.view.*
import android.view.animation.AccelerateDecelerateInterpolator
import androidx.fragment.app.DialogFragment
import th.ac.rmutto.finlove.databinding.ActivityLoadingBinding

class LoadingDialogFragment : DialogFragment() {

    private var _binding: ActivityLoadingBinding? = null
    private val binding get() = _binding!!

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // ใช้ธีม edge-to-edge ของเรา (แทน android.R.style.Theme_Translucent_NoTitleBar)
        setStyle(STYLE_NORMAL, R.style.Theme_Finlove_EdgeToEdge)
        isCancelable = false
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = ActivityLoadingBinding.inflate(inflater, container, false)
        dialog?.window?.apply {
            setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        startDotAnimation()
        return binding.root
    }

    override fun onDestroyView() {
        _binding = null
        super.onDestroyView()
    }

    private fun startDotAnimation() {
        val dots = listOf(binding.dot1, binding.dot2, binding.dot3)
        for ((index, dot) in dots.withIndex()) {
            ObjectAnimator.ofPropertyValuesHolder(
                dot,
                PropertyValuesHolder.ofFloat("scaleX", 1.5f, 0.5f, 1.5f),
                PropertyValuesHolder.ofFloat("scaleY", 1.5f, 0.5f, 1.5f)
            ).apply {
                duration = 500
                startDelay = (index * 150).toLong()
                repeatCount = ObjectAnimator.INFINITE
                interpolator = AccelerateDecelerateInterpolator()
                start()
            }
        }
    }

    companion object {
        fun show(fm: androidx.fragment.app.FragmentManager): LoadingDialogFragment {
            val d = LoadingDialogFragment()
            d.show(fm, "loading")
            return d
        }
    }
}
