package fans.openask.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import fans.openask.R
import fans.openask.databinding.ItemAsksBinding
import fans.openask.databinding.ItemCompletedBinding
import fans.openask.model.AsksModel
import java.math.BigDecimal
import java.text.SimpleDateFormat


/**
 *
 * Created by Irving
 */
class CompletedAdapter(list: MutableList<AsksModel>) : Adapter<CompletedAdapter.ViewHolder>() {
	var list = mutableListOf<AsksModel>()
	
	init {
		this.list = list
	}
	
	var onItemClickListener:OnItemClickListener? = null
	var onItemPlayClickListener:OnItemClickListener? = null
	
	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var binding = DataBindingUtil.bind<ItemCompletedBinding>(itemView)!!
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		var itemView =
			LayoutInflater.from(parent.context).inflate(R.layout.item_completed, parent, false)
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
		when (list[position].questionStatus) { //0 awaiting 1 answered 2 exipired
			0 -> {
				holder.binding.layoutAnswer.visibility = View.GONE
			}
			
			1 -> {
				holder.binding.layoutAnswer.visibility = View.VISIBLE
			}
			
			2 -> {
				holder.binding.layoutAnswer.visibility = View.GONE
			}
		}
		
		if (holder.binding.layoutAnswer.visibility == View.VISIBLE) {
			Glide.with(holder.itemView).load(list[position].answererAvatarUrl)
				.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator).circleCrop()
				.into(holder.binding.ivAvator2)
			
			holder.binding.tvUserName.text = list[position].answererName
			holder.binding.tvAnsweredTime.text =
				"Answered on " + SimpleDateFormat("K:mmaa,MMM dd,yyyy").format(list[position].answerTime)
			
			val totalSeconds = list[position].answerContentSize // 假设这是你得到的秒数
			if (totalSeconds != null) {
				val minutes = totalSeconds / 60
				val seconds = totalSeconds % 60
				holder.binding.tvTimeDuration.text = String.format("%02d:%02d", minutes, seconds)
			}
			
			holder.binding.ivPlay.setOnClickListener { onItemPlayClickListener?.onItemClick(position) }
		}
		
	}
	
}