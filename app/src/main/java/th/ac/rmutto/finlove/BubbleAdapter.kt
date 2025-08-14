package th.ac.rmutto.finlove.ui.help

import android.graphics.Color
import android.graphics.Typeface
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.*
import android.widget.FrameLayout
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import th.ac.rmutto.finlove.*

class BubbleAdapter(
    private val onCtaClick: (BubbleAction) -> Unit   // << callback เมื่อกดคำ CTA
) : ListAdapter<BubbleMessage, BubbleAdapter.VH>(DIFF) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_bubble, parent, false)
        return VH(v, onCtaClick)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.bind(getItem(position))
    }

    class VH(view: View, private val onCtaClick: (BubbleAction) -> Unit) :
        RecyclerView.ViewHolder(view) {

        private val tv: TextView = view.findViewById(R.id.txtBubble)

        fun bind(item: BubbleMessage) {
            // พื้นหลัง/สีตามชนิด
            if (item.type == BubbleType.PERSONAL) {
                tv.setBackgroundResource(R.drawable.bg_bubble_personal)
                tv.setTextColor(Color.WHITE)
            } else {
                tv.setBackgroundResource(R.drawable.bg_bubble_generic)
                tv.setTextColor(0xFF222222.toInt())
            }

            // ทำให้คำ CTA (ถ้ามี) เป็นลิงก์กดได้
            val hasCta = item.ctaText != null && item.ctaAction != null
            if (hasCta) {
                val base = item.text
                val cta = item.ctaText!!
                val full = base + cta
                val ss = SpannableString(full)

                val start = base.length
                val end = start + cta.length
                val span = object : ClickableSpan() {
                    override fun onClick(widget: View) {
                        onCtaClick(item.ctaAction!!)   // ส่งให้ Fragment จัดการนำทาง
                    }
                    override fun updateDrawState(ds: TextPaint) {
                        super.updateDrawState(ds)
                        ds.isUnderlineText = false
                        ds.typeface = Typeface.DEFAULT_BOLD
                        ds.color = ContextCompat.getColor(tv.context, R.color.link_blue)
                    }
                }
                ss.setSpan(span, start, end, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
                tv.text = ss
                tv.movementMethod = LinkMovementMethod.getInstance()
                tv.highlightColor = Color.TRANSPARENT
            } else {
                tv.text = item.text
                tv.movementMethod = null
            }

            // จัดตำแหน่ง (ซ้ายเหมือนกันทั้งสองแบบในดีไซน์ปัจจุบัน)
            (tv.layoutParams as? FrameLayout.LayoutParams)?.apply {
                gravity = Gravity.START
                tv.layoutParams = this
            }
        }
    }

    companion object {
        private val DIFF = object : DiffUtil.ItemCallback<BubbleMessage>() {
            override fun areItemsTheSame(a: BubbleMessage, b: BubbleMessage) = a === b
            override fun areContentsTheSame(a: BubbleMessage, b: BubbleMessage) = a == b
        }
    }
}
