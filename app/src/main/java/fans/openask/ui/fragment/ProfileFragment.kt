package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.databinding.DialogBecomeSenseiBinding
import fans.openask.databinding.DialogBecomeSenseiStep2Binding
import fans.openask.databinding.DialogBecomeSenseiStep3Binding
import fans.openask.databinding.FragmentProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.AsksModel
import fans.openask.model.SenseiProfileSettingRepData
import fans.openask.model.UserInfo
import fans.openask.model.WalletData
import fans.openask.model.twitter.TwitterExtInfoModel
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.io.IOException


/**
 *
 * Created by Irving
 */
class ProfileFragment : BaseFragment() {
	private val TAG = "ProfileFragment"
	
	lateinit var mBinding: FragmentProfileBinding
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	var mediaPlayer: MediaPlayer? = null
	
	override fun getResId(): Int {
		return R.layout.fragment_profile
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		if (userInfo != null) {
			setProfileInfo(userInfo)
		}
		
		firebaseAuth = FirebaseAuth.getInstance()
		
		lifecycleScope.launch {
			getWallet()
		}
	}
	
	override fun initEvent() {
		
		mBinding.ivBtnBecome.setOnClickListener {
			showBecomeDialog()
		}
		
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		mediaPlayer?.release()
		mediaPlayer = null
		super.onDestroy()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
	}
	
	private fun showBecomeDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					lifecycleScope.launch { getStep() }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	/**
	 * 1.获取用户成为师傅步骤
	 */
	private suspend fun getStep() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/user/sensei/step")
				.toAwaitResponse<Int>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					//1:绑定twitter；2填写min price 3：intro
					when (it) {
						1 -> {
							getTwitterInfo()
						}
						
						2 -> {
							showSetMinPriceDialog()
						}
						
						3 -> {
							showSetIntroDialog()
						}
					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun getTwitterInfo() {
		val provider =
			OAuthProvider.newBuilder("twitter.com") //		provider.addCustomParameter("lang",'')
		val pendingResultTask = firebaseAuth.pendingAuthResult
		if (pendingResultTask != null) {            // There's something already here! Finish the sign-in for your user.
			pendingResultTask.addOnSuccessListener {                    // User is signed in.
				LogUtils.e(TAG, "pendingResultTask addOnSuccessListener" + Gson().toJson(it))
			}.addOnFailureListener {                    // Handle failure.
				LogUtils.e(TAG, "pendingResultTask addOnFailureListener "+ Gson().toJson(it))
			}
		} else {
		
		}
		
		firebaseAuth.startActivityForSignInWithProvider(requireActivity(), provider.build())
				.addOnSuccessListener {
					LogUtils.e(TAG, "firebaseAuth addOnSuccessListener " + Gson().toJson(it))
					
					var msg = Gson().toJson(it)
					if (msg.length > 4000){
						for (i in 0..msg.length/4000){
							if ((i * 4000 + 4000) > msg.length){
								LogUtils.e(TAG,msg.substring(i * 4000,msg.length))
							}else{
								LogUtils.e(TAG,msg.substring(i * 4000,i*4000 + 4000))
							}
							
						}
					}

					
					lifecycleScope.launch { bindTwitter(it) }
				}.addOnFailureListener {                // Handle failure.
					LogUtils.e(TAG, "firebaseAuth addOnFailureListener " + Gson().toJson(it))
				}
	}
	
	private suspend fun bindTwitter(result:AuthResult) {
		
		var extInfo = TwitterExtInfoModel(result.user?.uid
			,result.user?.providerId
			,result.user?.photoUrl.toString()
			,result.user?.displayName
		,result.user?.displayName
		,result.additionalUserInfo?.profile?.get("description").toString()
		,result.additionalUserInfo?.profile?.get("followers_count").toString().toInt())
		
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/user/tripartite-account/bind-user")
				.add("openId", 1613901158325551000)
				.add("type", 1)
				.add("extInfo", extInfo)
				.toAwaitResponse<List<Any>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					showSetMinPriceDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showSetMinPriceDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_2) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStep2Binding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvPrice1.setOnClickListener {
					binding.etPrice.setText("9")
				}
				
				binding.tvPrice2.setOnClickListener {
					binding.etPrice.setText("99")
				}
				
				binding.tvPrice3.setOnClickListener {
					binding.etPrice.setText("999")
				}
				
				binding.tvBtn.setOnClickListener {
					if (binding.etPrice.text.isNullOrEmpty()){
						ToastUtils.show("Input your price please")
						return@setOnClickListener
					}
					dialog.dismiss()
					lifecycleScope.launch { setMinPrice(binding.etPrice.text.toString()) }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun setMinPrice(price:String){
		(activity as BaseActivity).showLoadingDialog("Loading...")
		
		var extInfo = SenseiProfileSettingRepData()
		extInfo.minPrice = price
		
		RxHttp.postJson("/open-ask/user/sensei/update-profile")
				.add("openId", 1613901158325551000)
				.add("type", 1)//1、设置最小金额  2、设置自我介绍语音
				.add("extInfo", extInfo)
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					showSetIntroDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showSetIntroDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_3) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStep3Binding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvSkip.setOnClickListener {
					dialog.dismiss()
				}
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					//TODO
					lifecycleScope.launch { setIntro("","") }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun setIntro(url:String,duration:String){
		(activity as BaseActivity).showLoadingDialog("Loading...")
		
		var extInfo = SenseiProfileSettingRepData()
		extInfo.audioUrl = url
		extInfo.audioDuration = duration
		
		RxHttp.postJson("/open-ask/user/sensei/update-profile")
				.add("openId", 1613901158325551000)
				.add("type", 2)//1、设置最小金额  2、设置自我介绍语音
				.add("extInfo", extInfo)
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					showSetIntroDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun play(url: String) {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		mediaPlayer?.stop()
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepare()
		} catch (e: Exception) {
			e.printStackTrace()
		}
		
		mediaPlayer?.start()
	}
	
	private fun setProfileInfo(userInfo: UserInfo) {
		Glide.with(this).load(userInfo.headIcon).placeholder(R.drawable.icon_avator)
				.error(R.drawable.icon_avator).circleCrop().into(mBinding.ivAvator)
		
		mBinding.tvUsername.text = userInfo.nickname
		
		if (userInfo.isSensei == true) {
			mBinding.ivBtnBecome.visibility = View.GONE
		} else {
			mBinding.ivBtnBecome.visibility = View.VISIBLE
		}
	}
	
	private suspend fun getWallet() {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet").add("clientType", 7).add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					mBinding.tvBalanceValue.text = "$" + it.balance
					mBinding.tvEarningValue.text = "$" + it.totalEarning
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
}