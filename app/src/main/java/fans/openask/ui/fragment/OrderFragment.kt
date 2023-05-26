package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import fans.openask.R
import fans.openask.databinding.FragmentOrderBinding
import fans.openask.model.event.UpdateNumEvent
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *
 * Created by Irving
 */
class OrderFragment :BaseFragment(){
	
	private lateinit var mBinding:FragmentOrderBinding
	
	private var mAwaitingFragment: AwaitingFragment? = null
	private var mOrderFragment: CompletedFragment? = null
	private var mCurrentFragment: BaseFragment? = null
	
	override fun getResId(): Int {
		return R.layout.fragment_order
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		showAwaitingFragment()
	}
	
	override fun initEvent() {
		mBinding.tvAwaiting.setOnClickListener {
			mBinding.tvAwaiting.isEnabled = false
			mBinding.tvCompleted.isEnabled = true
			
			showAwaitingFragment()
		}
		
		mBinding.tvCompleted.setOnClickListener {
			mBinding.tvAwaiting.isEnabled = true
			mBinding.tvCompleted.isEnabled = false
			
			showCompletedFragment()
		}
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onEvent(event: UpdateNumEvent){
		if (event.eventType == UpdateNumEvent.EVENT_TYPE_AWAITING){
			mBinding.tvAwaiting.text = "Awaiting(${event.eventValue})"
		}else if (event.eventType == UpdateNumEvent.EVENT_TYPE_COMPLETED){
			mBinding.tvCompleted.text = "Completed(${event.eventValue})"
		}
	}
	
	private fun showAwaitingFragment() {
		val transaction = childFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mAwaitingFragment == null) {
			mAwaitingFragment = AwaitingFragment()
			transaction.add(R.id.frameLayout, mAwaitingFragment!!)
		} else {
			transaction.show(mAwaitingFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mAwaitingFragment
	}
	
	private fun showCompletedFragment() {
		val transaction = childFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mOrderFragment == null) {
			mOrderFragment = CompletedFragment()
			transaction.add(R.id.frameLayout, mOrderFragment!!)
		} else {
			transaction.show(mOrderFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mOrderFragment
	}
	
}