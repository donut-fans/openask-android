package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import fans.openask.R
import fans.openask.databinding.ActivityUserActivitiesBinding
import fans.openask.ui.fragment.AsksFragment
import fans.openask.ui.fragment.AwaitingFragment
import fans.openask.ui.fragment.BaseFragment
import fans.openask.ui.fragment.CompletedFragment

/**
 *
 * Created by Irving
 */
class UserActivitiesActivity:BaseActivity() {
	
	private lateinit var mBinding:ActivityUserActivitiesBinding
	
	private var mAsksFragment: AsksFragment? = null
	private var mOrderFragment: CompletedFragment? = null
	private var mCurrentFragment: BaseFragment? = null
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,UserActivitiesActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_user_activities
	}
	
	override fun initView() {
		
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
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private fun showAsksFragment() {
		val transaction = supportFragmentManager.beginTransaction()
		
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
		val transaction = supportFragmentManager.beginTransaction()
		
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