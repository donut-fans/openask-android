package fans.openask.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.databinding.DataBindingUtil
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Adapter
import com.bumptech.glide.Glide
import fans.openask.R
import fans.openask.databinding.ItemCategoryBinding
import fans.openask.model.TAGModel

/**
 *
 * Created by Irving
 */
class CategoryAdapter(list:MutableList<TAGModel>): Adapter<CategoryAdapter.ViewHolder>() {
	
	var list = mutableListOf<TAGModel>()
	
	init {
		this.list = list
	}
	
	fun getCheckedTags():MutableList<Int>{
		var ids = mutableListOf<Int>()
		for (i in 0 until list.size){
			if (list[i].isChecked == true) {
				ids.add(list[i].id!!)
			}
		}
		return ids
	}
	
	class ViewHolder:RecyclerView.ViewHolder{
		constructor(itemView: View) : super(itemView)
		val binding = DataBindingUtil.bind<ItemCategoryBinding>(itemView)!!
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		var itemView = LayoutInflater.from(parent.context).inflate(R.layout.item_category,parent,false)
		return ViewHolder(itemView)
	}
	
	override fun getItemCount(): Int {
		return list.size
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		if (list[position].isChecked != true){
			holder.binding.layout.background = null
			holder.binding.ivChecked.visibility = View.GONE
		}else{
			holder.binding.layout.setBackgroundResource(R.drawable.bg_catetory_select)
			holder.binding.ivChecked.visibility = View.VISIBLE
		}
		
		holder.binding.textview.text = list[position].name
		Glide.with(holder.binding.image).load(list[position].iconUrl).into(holder.binding.image)
		
		holder.itemView.setOnClickListener {
			if (list[position].isChecked == true){
				list[position].isChecked = false
				holder.binding.layout.background = null
				holder.binding.ivChecked.visibility = View.GONE
			}else{
				list[position].isChecked = true
				holder.binding.layout.setBackgroundResource(R.drawable.bg_catetory_select)
				holder.binding.ivChecked.visibility = View.VISIBLE
			}
			notifyItemChanged(position)
		}
	}
	
}