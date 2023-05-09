package fans.openask.ui.activity

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import fans.openask.R
import fans.openask.databinding.ActivityMainBinding
import fans.openask.ui.fragment.BaseFragment
import fans.openask.ui.fragment.SenseisFragment
import fans.openask.ui.fragment.OrderFragment
import fans.openask.ui.fragment.ProfileFragment

class MainActivity : BaseActivity() {
	
	companion object {
		fun launch(activity: Activity) {
			var intent = Intent(activity, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			activity.startActivity(intent)
		}
	}
	
	private var mSenseisFragment:SenseisFragment? = null
	private var mOrderFragment:OrderFragment? = null
	private var mProfileFragment:ProfileFragment? = null
	private var mCurrentFragment:BaseFragment? = null
	
	lateinit var mBinding: ActivityMainBinding
	
	override fun getResId(): Int {
		return R.layout.activity_main
	}
	
	override fun initView() {
		showHomePage()
	}
	
	override fun initData() {
	
	}
	
	override fun initEvent() {
		mBinding.ivMenu.setOnClickListener {
			mBinding.drawerlayout.openDrawer(GravityCompat.END)
		}
		
		mBinding.ivClose.setOnClickListener {
			mBinding.drawerlayout.closeDrawers()
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private fun showHomePage() {
		val transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mSenseisFragment == null) {
			mSenseisFragment = SenseisFragment()
			transaction.add(R.id.frameLayout, mSenseisFragment!!)
		} else {
			transaction.show(mSenseisFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mSenseisFragment
	}
	
	private fun showOrder() {
		val transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mOrderFragment == null) {
			mOrderFragment = OrderFragment()
			transaction.add(R.id.frameLayout, mOrderFragment!!)
		} else {
			transaction.show(mOrderFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mOrderFragment
	}
	
	private fun showProfile() {
		val transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mProfileFragment == null) {
			mProfileFragment = ProfileFragment()
			transaction.add(R.id.frameLayout, mProfileFragment!!)
		} else {
			transaction.show(mProfileFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mProfileFragment
	}
}