package fans.openask.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import fans.openask.R
import fans.openask.databinding.ItemSenseiListBinding
import fans.openask.model.SenseiListModel

/**
 *
 * Created by Irving
 */
class SenseiListAdapter(list:MutableList<SenseiListModel>) : RecyclerView.Adapter<SenseiListAdapter.ViewHolder>() {
	
	var list = mutableListOf<SenseiListModel>()
	
	var onItemClickListener:OnItemClickListener? = null
	var onItemAskClickListener:OnItemClickListener? = null
	
	init {
		this.list = list
	}
	
	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var binding = DataBindingUtil.bind<ItemSenseiListBinding>(itemView)!!
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		var itemView =
			LayoutInflater.from(parent.context).inflate(R.layout.item_sensei_list, parent, false)
		return ViewHolder(itemView)
	}
	
	override fun getItemCount(): Int {
		return list.size
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		Glide.with(holder.binding.ivAvator)
			.load(list[position].senseiAvatarUrl)
			.placeholder(R.drawable.icon_avator)
			.error(R.drawable.icon_avator)
			.circleCrop()
			.into(holder.binding.ivAvator)
		
		holder.binding.tvNickname.text = list[position].senseiName
		holder.binding.tvUsername.text = list[position].senseiUsername
		holder.binding.tvFollowerCount.text = list[position].followersCount+"  followers"
		holder.binding.tvAnswerCount.text = list[position].followersCount+"  answers"
		holder.binding.tvDesc.text = list[position].bio
		
		holder.binding.layout.setOnClickListener { onItemClickListener?.onItemClick(position) }
		
		holder.binding.ivAsk.setOnClickListener { onItemAskClickListener?.onItemClick(position) }
	}
	
}