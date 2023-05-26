package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import fans.openask.R
import fans.openask.databinding.FragmentUserActivitiesBinding
import fans.openask.model.event.UpdateNumEvent
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *
 * Created by Irving
 */
class UserActivitiesFragment:BaseFragment() {
	
	private lateinit var mBinding:FragmentUserActivitiesBinding
	
	private var mAsksFragment: AsksFragment? = null
	private var mEavesdropFragment: EavesdropFragment? = null
	private var mCurrentFragment: BaseFragment? = null
	
	override fun getResId(): Int {
		return R.layout.fragment_user_activities
	}
	
	override fun initView(contentView: View) {
		
	}
	
	override fun initData() {
		showAsksFragment()
	}
	
	override fun initEvent() {
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAsks.isEnabled = false
			mBinding.tvEavesdrop.isEnabled = true
			showAsksFragment()
		}
		
		mBinding.tvEavesdrop.setOnClickListener {
			mBinding.tvAsks.isEnabled = true
			mBinding.tvEavesdrop.isEnabled = false
			
			showCompletedFragment()
		}
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onEvent(event: UpdateNumEvent){
		if (event.eventType == UpdateNumEvent.EVENT_TYPE_ASKS){
			mBinding.tvAsks.text = "Your Asks(${event.eventValue})"
		}else if (event.eventType == UpdateNumEvent.EVENT_TYPE_EAVESDROP){
			mBinding.tvEavesdrop.text = "Your Eavesdrop(${event.eventValue})"
		}
	}
	
	private fun showAsksFragment() {
		val transaction = childFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mAsksFragment == null) {
			mAsksFragment = AsksFragment()
			transaction.add(R.id.frameLayout, mAsksFragment!!)
		} else {
			transaction.show(mAsksFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mAsksFragment
	}
	
	private fun showCompletedFragment() {
		val transaction = childFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mEavesdropFragment == null) {
			mEavesdropFragment = EavesdropFragment()
			transaction.add(R.id.frameLayout, mEavesdropFragment!!)
		} else {
			transaction.show(mEavesdropFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mEavesdropFragment
	}
}