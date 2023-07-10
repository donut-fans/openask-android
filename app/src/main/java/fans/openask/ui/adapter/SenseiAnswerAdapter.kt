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
import fans.openask.databinding.ItemSenseiAnswerBinding
import fans.openask.model.AsksModel
import fans.openask.model.SenseiAnswerModel
import java.math.BigDecimal
import java.text.SimpleDateFormat


/**
 *
 * Created by Irving
 */
class SenseiAnswerAdapter(list: MutableList<SenseiAnswerModel>) : Adapter<SenseiAnswerAdapter.ViewHolder>() {
	var list = mutableListOf<SenseiAnswerModel>()
	
	init {
		this.list = list
	}
	
	var onItemClickListener:OnItemClickListener? = null
	var onItemPlayClickListener:OnItemClickListener? = null
	
	class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
		var binding = DataBindingUtil.bind<ItemSenseiAnswerBinding>(itemView)!!
	}
	
	override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
		var itemView =
			LayoutInflater.from(parent.context).inflate(R.layout.item_sensei_answer, parent, false)
		return ViewHolder(itemView)
	}
	
	override fun getItemCount(): Int {
		return list.size
	}
	
	override fun onBindViewHolder(holder: ViewHolder, position: Int) {
		Glide.with(holder.itemView).load(list[position].questionerAvatar)
			.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator).circleCrop()
			.into(holder.binding.ivAvator)
		
		holder.binding.tvName.text = list[position].questionerName
		holder.binding.tvTime.text =
			"Posted on " + SimpleDateFormat("K:mmaa,MMM dd,yyyy").format(list[position].askQuestionTime)
		holder.binding.tvContent.text = list[position].questionContent
		
		holder.binding.tvMoney.text =
			"$" + BigDecimal(list[position].rewardAmount).stripTrailingZeros().toPlainString()
		
		if (list[position].askQuestionStatus == 1){
			holder.binding.layoutAnswer.visibility = View.VISIBLE
			
			Glide.with(holder.itemView).load(list[position].questioneeAvatar)
					.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator).circleCrop()
					.into(holder.binding.ivAvator2)
			
			holder.binding.tvUserName.text = list[position].questioneeName
			holder.binding.tvAnsweredTime.text =
				"Answered on " + SimpleDateFormat("K:mmaa,MMM dd,yyyy").format(list[position].answerTime)
			
			if (list[position].answerState != null){
				if (list[position].answerState!!.answerContent.isNullOrEmpty()){//未付费
					holder.binding.ivPlay.setImageResource(R.drawable.icon_btn_pay)
				}else{//已经付费
					holder.binding.ivPlay.setImageResource(R.drawable.icon_btn_answer_play)
					if (list[position].answerState!!.contentSize != null) {
						val minutes = (list[position].answerState!!.contentSize!! / 60).toInt()
						val seconds = (list[position].answerState!!.contentSize!! % 60).toInt()
						holder.binding.tvTimeDuration.text = String.format("%02d:%02d", minutes, seconds)
					}
					
				}
			}else{
				holder.binding.ivPlay.setImageResource(R.drawable.icon_btn_pay)
			}
			
			holder.binding.ivPlay.setOnClickListener { onItemPlayClickListener?.onItemClick(position) }
			
		}else{
			holder.binding.layoutAnswer.visibility = View.GONE
		}
		
	}
	
}