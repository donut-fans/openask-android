package fans.openask.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import fans.openask.R
import fans.openask.databinding.ItemAwaitingAnswerBinding
import fans.openask.model.AsksModel
import java.math.BigDecimal
import java.text.SimpleDateFormat


/**
 *
 * Created by Irving
 */
class AwaitingAnswerAdapter(list: MutableList<AsksModel>) : Adapter<AwaitingAnswerAdapter.ViewHolder>() {
	var list = mutableListOf<AsksModel>()
	
	init {
		this.list = list
	}
	
	var onItemClickListener:OnItemClickListener? = null
	var onItemAnswerClickListener:OnItemClickListener? = null
	
	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var binding = DataBindingUtil.bind<ItemAwaitingAnswerBinding>(itemView)!!
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		var itemView =
			LayoutInflater.from(parent.context).inflate(R.layout.item_awaiting_answer, parent, false)
		return ViewHolder(itemView)
	}
	
	override fun getItemCount(): Int {
		return list.size
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		Glide.with(holder.itemView).load(list[position].answererAvatarUrl)
			.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator).circleCrop()
			.into(holder.binding.ivAvator)
		
		holder.binding.tvName.text = list[position].answererName
		holder.binding.tvTime.text =
			"Posted on " + SimpleDateFormat("K:mmaa,MMM dd,yyyy").format(list[position].questionAskTime)
		holder.binding.tvContent.text = list[position].questionContent
		
		holder.binding.tvMoney.text =
			"$" + BigDecimal(list[position].payAmount).stripTrailingZeros().toPlainString()
		
		holder.binding.tvAnswer.setOnClickListener {
			onItemAnswerClickListener?.onItemClick(position)
		}
	}
	
}