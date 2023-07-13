package fans.openask.ui.activity

import android.app.Activity
import android.content.Intent
import android.view.View
import androidx.core.view.GravityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import fans.openask.OpenAskApplication
import fans.openask.R
import fans.openask.databinding.ActivityMainBinding
import fans.openask.model.RemindCountData
import fans.openask.model.UpdateUserInfo
import fans.openask.model.UserInfo
import fans.openask.model.event.BecomeSenseiEvent
import fans.openask.ui.fragment.BaseFragment
import fans.openask.ui.fragment.SenseisFragment
import fans.openask.ui.fragment.OrderFragment
import fans.openask.ui.fragment.ProfileFragment
import fans.openask.ui.fragment.UserActivitiesFragment
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

class MainActivity : BaseActivity() {
	private val TAG = "MainActivity"
	
	var userInfo: UserInfo? = null
	
	companion object {
		fun launch(activity: Activity) {
			var intent = Intent(activity, MainActivity::class.java)
			intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
			activity.startActivity(intent)
		}
	}
	
	private var mSenseisFragment: SenseisFragment? = null
	private var mUserActivitiesActivity: UserActivitiesFragment? = null
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
		mBinding.ivLogo.setOnClickListener {
			twitterLogin()
		}
		
		mBinding.ivMenu.setOnClickListener {
			mBinding.drawerlayout.openDrawer(GravityCompat.END)
			lifecycleScope.launch {
				getRemindCount()
			}
		}
		
		mBinding.ivClose.setOnClickListener {
			mBinding.drawerlayout.closeDrawers()
		}
		
		mBinding.tvLogout.setOnClickListener {
			OpenAskApplication.instance.startReLogin()
		}
		
		mBinding.tvSenseis.setOnClickListener {
			LogUtils.e(TAG, "tvSenseis")
			
			showHomePage()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = false
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis_selected)
			mBinding.tvActivities.isEnabled = true
			mBinding.ivActivities.setImageResource(R.drawable.icon_drawer_activities)
			mBinding.tvAskforu.isEnabled = true
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks)
			mBinding.tvProfile.isEnabled = true
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile)
		}
		
		mBinding.tvActivities.setOnClickListener {
			LogUtils.e(TAG, "tvAskforu")
			
			showActivities()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = true
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis)
			mBinding.tvActivities.isEnabled = false
			mBinding.ivActivities.setImageResource(R.drawable.icon_drawer_activities_selected)
			mBinding.tvAskforu.isEnabled = true
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks)
			mBinding.tvProfile.isEnabled = true
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile)
		}
		
		mBinding.tvAskforu.setOnClickListener {
			LogUtils.e(TAG, "tvAskforu")
			
			showOrder()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = true
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis)
			mBinding.tvActivities.isEnabled = true
			mBinding.ivActivities.setImageResource(R.drawable.icon_drawer_activities)
			mBinding.tvAskforu.isEnabled = false
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks_selected)
			mBinding.tvProfile.isEnabled = true
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile)
		}
		
		mBinding.tvProfile.setOnClickListener {
			LogUtils.e(TAG, "tvProfile")
			
			showProfile()
			mBinding.drawerlayout.closeDrawers()
			
			mBinding.tvSenseis.isEnabled = true
			mBinding.ivSenseis.setImageResource(R.drawable.icon_drawer_senseis)
			mBinding.tvActivities.isEnabled = true
			mBinding.ivActivities.setImageResource(R.drawable.icon_drawer_activities)
			mBinding.tvAskforu.isEnabled = true
			mBinding.ivAskforu.setImageResource(R.drawable.icon_drawer_asks)
			mBinding.tvProfile.isEnabled = false
			mBinding.ivProfile.setImageResource(R.drawable.icon_drawer_profile_selected)
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	var backTime: Long = 0
	override fun onBackPressed() {
		if (System.currentTimeMillis() - backTime <= 1500) {
			super.onBackPressed()
		} else {
			ToastUtils.show("Press again to exit")
			backTime = System.currentTimeMillis()
		}
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onSenseiEvent(any: BecomeSenseiEvent) {
		mBinding.tvAskforu.visibility = View.VISIBLE
		mBinding.ivAskforu.visibility = View.VISIBLE
		
		lifecycleScope.launch {
			userInfo?.username?.let { updateUserInfo(it) }
		}
	}
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	private fun twitterLogin() {
		firebaseAuth = Firebase.auth
		
		val provider = OAuthProvider.newBuilder("twitter.com")
		val pendingResultTask = firebaseAuth.pendingAuthResult
		if (pendingResultTask != null) {
			// There's something already here! Finish the sign-in for your user.
			pendingResultTask
					.addOnSuccessListener {
						LogUtils.e(TAG,
							"pendingResultTask addOnSuccessListener " + it.toString())
						// User is signed in.
						// IdP data available in
						// authResult.getAdditionalUserInfo().getProfile().
						// The OAuth access token can also be retrieved:
						// ((OAuthCredential)authResult.getCredential()).getAccessToken().
						// The OAuth secret can be retrieved by calling:
						// ((OAuthCredential)authResult.getCredential()).getSecret().
					}
					.addOnFailureListener {
						// Handle failure.
						LogUtils.e(TAG,
							"pendingResultTask addOnFailureListener " + it.toString())
					}
		} else {
			// There's no pending result so you need to start the sign-in flow.
			// See below.
			LogUtils.e(TAG, "pendingResultTask == null  ")
		}
		
		firebaseAuth
				.startActivityForSignInWithProvider(this, provider.build())
				.addOnSuccessListener {
					LogUtils.e(TAG,
						"startActivityForSignInWithProvider addOnSuccessListener " + it.toString())
					// User is signed in.
					// IdP data available in
					// authResult.getAdditionalUserInfo().getProfile().
					// The OAuth access token can also be retrieved:
					// ((OAuthCredential)authResult.getCredential()).getAccessToken().
					// The OAuth secret can be retrieved by calling:
					// ((OAuthCredential)authResult.getCredential()).getSecret().
					LogUtils.e(TAG, "Str111 = ")
					var str = Gson().toJson(it.additionalUserInfo)
					LogUtils.e(TAG, "Str = " + str)
				}
				.addOnFailureListener {
					LogUtils.e(TAG,
						"startActivityForSignInWithProvider addOnFailureListener " + it.toString())
				}
	}
	
	private fun setUserInfo() {
		if (userInfo == null) {
			userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		}
		
		if (userInfo != null) {
			Glide.with(this)
					.load(userInfo!!.headIcon)
					.placeholder(R.drawable.icon_avator)
					.error(R.drawable.icon_avator)
					.circleCrop()
					.into(mBinding.ivAvator)
			
			mBinding.ivBtnSignin.visibility = View.GONE
			mBinding.tvNickname.visibility = View.VISIBLE
			mBinding.tvUsername.visibility = View.VISIBLE
			
			mBinding.tvNickname.text = userInfo!!.nickname
			
			if (userInfo!!.username == userInfo!!.email) {
				mBinding.tvUsername.visibility = View.INVISIBLE
			} else {
				mBinding.tvUsername.visibility = View.VISIBLE
				mBinding.tvUsername.text = "@" + userInfo!!.username
			}
			
			if (userInfo!!.isSensei == true) {
				mBinding.tvAskforu.visibility = View.VISIBLE
				mBinding.ivAskforu.visibility = View.VISIBLE
			} else {
				mBinding.tvAskforu.visibility = View.GONE
				mBinding.ivAskforu.visibility = View.GONE
			}
			
		} else {
			mBinding.ivBtnSignin.visibility = View.VISIBLE
			mBinding.tvNickname.visibility = View.GONE
			mBinding.tvUsername.visibility = View.GONE
		}
	}
	
	private fun showHomePage() {
		var transaction = supportFragmentManager.beginTransaction()
		
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
		LogUtils.e(TAG, "showOrder")
		
		var transaction = supportFragmentManager.beginTransaction()
		
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
	
	private fun showActivities() {
		LogUtils.e(TAG, "showOrder")
		
		var transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mUserActivitiesActivity == null) {
			mUserActivitiesActivity = UserActivitiesFragment()
			transaction.add(R.id.frameLayout, mUserActivitiesActivity!!)
		} else {
			transaction.show(mUserActivitiesActivity!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mUserActivitiesActivity
	}
	
	private fun showProfile() {
		var transaction = supportFragmentManager.beginTransaction()
		
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
	
	/**
	 * 1.获取用户成为师傅步骤
	 */
	private suspend fun getRemindCount() {
		RxHttp.get("/open-ask/get-remind-count")
				.toAwaitResponse<RemindCountData>()
				.awaitResult {
					if (it.myAsksCount == 0) {
						mBinding.viewAskPoint.visibility = View.GONE
					} else {
						mBinding.viewAskPoint.visibility = View.VISIBLE
					}

//					if (it.answerAwaitingCount == 0){
//						mBinding.viewAskPoint.visibility = View.GONE
//					}else{
//						mBinding.viewAskPoint.visibility = View.VISIBLE
//					}
				}.onFailure {
				}
	}
	
	/**
	 * 1.获取用户成为师傅步骤
	 */
	private suspend fun updateUserInfo(userName: String) {
		RxHttp.get("/open-ask/user/user-page-info/$userName")
				.toAwaitResponse<UpdateUserInfo>()
				.awaitResult {
					
					userInfo?.username = it.displayName
					userInfo?.nickname = it.username
					
					MMKV.defaultMMKV().encode("userInfo", userInfo)
					setUserInfo()
					mProfileFragment?.setProfileInfo(userInfo!!)
					
				}.onFailure {
				}
	}
}