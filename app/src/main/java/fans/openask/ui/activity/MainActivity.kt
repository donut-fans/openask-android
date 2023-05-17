package fans.openask.ui.activity

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import com.bumptech.glide.Glide
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.databinding.ActivityMainBinding
import fans.openask.model.UserInfo
import fans.openask.ui.fragment.BaseFragment
import fans.openask.ui.fragment.SenseisFragment
import fans.openask.ui.fragment.OrderFragment
import fans.openask.ui.fragment.ProfileFragment
import fans.openask.utils.LogUtils

class MainActivity : BaseActivity() {
	private val TAG = "MainActivity"
	
	companion object {
		fun launch(activity: Activity) {
			var intent = Intent(activity, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			activity.startActivity(intent)
		}
	}
	
	private var mSenseisFragment: SenseisFragment? = null
	private var mOrderFragment: OrderFragment? = null
	private var mProfileFragment: ProfileFragment? = null
	private var mCurrentFragment: BaseFragment? = null
	
	lateinit var mBinding: ActivityMainBinding
	
	override fun getResId(): Int {
		return R.layout.activity_main
	}
	
	override fun initView() {
		showHomePage()
	}
	
	override fun initData() {
		setUserInfo()
	}
	
	override fun initEvent() {
		mBinding.ivMenu.setOnClickListener {
			mBinding.drawerlayout.openDrawer(GravityCompat.END)
		}
		
		mBinding.ivClose.setOnClickListener {
			mBinding.drawerlayout.closeDrawers()
		}
		
		mBinding.tvSenseis.setOnClickListener {
			LogUtils.e(TAG,"tvSenseis")
			
			showHomePage()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = false
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis_selected)
			mBinding.tvAskforu.isEnabled = true
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks)
			mBinding.tvProfile.isEnabled = true
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile)
		}
		
		mBinding.tvAskforu.setOnClickListener {
			LogUtils.e(TAG,"tvAskforu")
			
			showOrder()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = true
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis)
			mBinding.tvAskforu.isEnabled = false
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks)
			mBinding.tvProfile.isEnabled = true
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile_selected)
		}
		
		mBinding.tvProfile.setOnClickListener {
			LogUtils.e(TAG,"tvProfile")
			
			showProfile()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = true
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis)
			mBinding.tvAskforu.isEnabled = true
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks_selected)
			mBinding.tvProfile.isEnabled = false
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile)
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private fun setUserInfo() {
		var userInfo: UserInfo? =
			MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		
		if (userInfo != null) {
			Glide.with(this)
				.load(userInfo.headIcon)
				.placeholder(R.drawable.icon_avator)
				.error(R.drawable.icon_avator)
				.circleCrop()
				.into(mBinding.ivAvator)
			
			mBinding.ivBtnSignin.visibility = View.GONE
			mBinding.tvNickname.visibility = View.VISIBLE
			mBinding.tvUsername.visibility = View.VISIBLE
			
			mBinding.tvNickname.text = userInfo.nickname
			mBinding.tvUsername.text = userInfo.username
			
			if (userInfo.isSensei == true){
				mBinding.tvAskforu.visibility = View.VISIBLE
				mBinding.ivAskforu.visibility = View.VISIBLE
			}else{
				mBinding.tvAskforu.visibility = View.GONE
				mBinding.ivAskforu.visibility = View.GONE
			}
			
		}else{
			mBinding.ivBtnSignin.visibility = View.VISIBLE
			mBinding.tvNickname.visibility = View.GONE
			mBinding.tvUsername.visibility = View.GONE
		}
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