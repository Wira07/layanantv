package com.ali.layanantv.ui.customer

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.ali.layanantv.R

class HelpAdapter(
    private val helpItems: List<HelpItem>,
    private val onItemClick: (HelpItem) -> Unit
) : RecyclerView.Adapter<HelpAdapter.HelpViewHolder>() {

    inner class HelpViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val titleText: TextView = itemView.findViewById(R.id.tv_help_title)
        val descriptionText: TextView = itemView.findViewById(R.id.tv_help_description)
        val expandIcon: ImageView = itemView.findViewById(R.id.iv_expand_icon)
        val subItemsContainer: LinearLayout = itemView.findViewById(R.id.ll_sub_items)
        val cardView: View = itemView.findViewById(R.id.card_help_item)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HelpViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_help, parent, false)
        return HelpViewHolder(view)
    }

    override fun onBindViewHolder(holder: HelpViewHolder, position: Int) {
        val helpItem = helpItems[position]

        holder.titleText.text = helpItem.title
        holder.descriptionText.text = helpItem.description

        // Set expand icon rotation
        if (helpItem.isExpanded) {
            holder.expandIcon.rotation = 180f
            holder.subItemsContainer.visibility = View.VISIBLE
        } else {
            holder.expandIcon.rotation = 0f
            holder.subItemsContainer.visibility = View.GONE
        }

        // Clear previous sub items
        holder.subItemsContainer.removeAllViews()

        // Add sub items if expanded
        if (helpItem.isExpanded) {
            helpItem.subItems.forEach { subItem ->
                val subItemView = LayoutInflater.from(holder.itemView.context)
                    .inflate(R.layout.item_help_sub, holder.subItemsContainer, false)

                val questionText = subItemView.findViewById<TextView>(R.id.tv_question)
                val answerText = subItemView.findViewById<TextView>(R.id.tv_answer)

                questionText.text = subItem.question
                answerText.text = subItem.answer

                holder.subItemsContainer.addView(subItemView)
            }
        }

        // Handle click
        holder.cardView.setOnClickListener {
            onItemClick(helpItem)
        }
    }

    override fun getItemCount(): Int = helpItems.size
}