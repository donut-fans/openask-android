package fans.openask.ui.activity

import android.content.Intent
import android.os.CountDownTimer
import android.text.SpannableString
import android.text.Spanned
import android.text.TextPaint
import android.text.method.LinkMovementMethod
import android.text.style.ClickableSpan
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.tencent.mmkv.MMKV
import com.tokenpocket.opensdk.base.TPListener
import com.tokenpocket.opensdk.base.TPManager
import com.tokenpocket.opensdk.simple.model.Authorize
import com.tokenpocket.opensdk.simple.model.Blockchain
import com.tokenpocket.opensdk.simple.model.Signature
import fans.openask.BuildConfig
import fans.openask.OpenAskApplication
import fans.openask.R
import fans.openask.databinding.ActivityLoginBinding
import fans.openask.databinding.DialogEmailLoginBinding
import fans.openask.http.errorMsg
import fans.openask.model.NonceData
import fans.openask.model.TPWalletLoginData
import fans.openask.model.TPWalletSignData
import fans.openask.model.UserInfo
import fans.openask.model.twitter.TwitterExtInfoModel
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import fans.openask.utils.isEmail
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse


/**
 *
 * Created by Irving
 */
class LoginActivity : BaseActivity() {
	var TAG = "LoginActivity"
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	lateinit var mBinding: ActivityLoginBinding
	
	private var countDownTimer:CountDownTimer? = null
	
	companion object {
		fun launch(activity: BaseActivity) {
			activity.startActivity(Intent(activity, LoginActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_login
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF", true)
		
		setBottomText()
	}
	
	override fun initData() {
		firebaseAuth = FirebaseAuth.getInstance()
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.ivBtnSigninWallet.setOnClickListener {
			getWalletAddress()
		}
		
		mBinding.ivBtnSigninTwitter.setOnClickListener {
			getTwitterInfo()
		}
		
		mBinding.ivEmail.setOnClickListener {
			showEmailSignDialog()
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		countDownTimer?.cancel()
		super.onDestroy()
	}
	
	private fun showEmailSignDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_email_login) {
			override fun onBind(dialog: CustomDialog, v: View) {
				val binding = DataBindingUtil.bind<DialogEmailLoginBinding>(v)!!
				
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtnSendCode.setOnClickListener {
					var email = binding.etEmail.text.toString()
					if (isEmail(email)) {
						lifecycleScope.launch {
							showLoadingDialog("Loading...")
							RxHttp.postJson("/validator/send-code")
									.add("email", email)
									.add("type", 23)
									.toAwaitResponse<Boolean>()
									.awaitResult {
										dismissLoadingDialog()
										binding.tvBtnSendCode.isEnabled = false
										binding.tvBtnSendCode.text = "60s Reacquire"
										
										countDownTimer?.cancel()
										countDownTimer = object :CountDownTimer(60000,1000){
											override fun onTick(p0: Long) {
												binding.tvBtnSendCode.text = "${p0/1000}s Reacquire"
											}
											
											override fun onFinish() {
												binding.tvBtnSendCode.isEnabled = true
												binding.tvBtnSendCode.text = "Send Code"
											}
											
										}
										countDownTimer?.start()
										
									}.onFailure {
										showFailedDialog(it.errorMsg)
									}
						}
					} else {
						ToastUtils.show("Please enter the correct email address")
					}
				}
				
				binding.tvBtn.setOnClickListener {
					var email = binding.etEmail.text.toString()
					var code = binding.etCode.text.toString()
					if (isEmail(email)) {
						if (code.isNullOrEmpty()){
							ToastUtils.show("Please enter the code")
						}else{
							showLoadingDialog("Loading...")
							lifecycleScope.launch {
								RxHttp.postJson("/user/login")
										.add("email", email)
										.add("code", code)
										.toAwaitResponse<UserInfo>()
										.awaitResult {
											dialog.dismiss()
											dismissLoadingDialog()
											MMKV.defaultMMKV().encode("userInfo", it)
											
											OpenAskApplication.instance.userInfo = it
											OpenAskApplication.instance.initRxHttp(it.token!!)
											MainActivity.launch(this@LoginActivity)
											finish()
										}.onFailure {
											showFailedDialog(it.errorMsg)
										}
							}
						}
					} else {
						ToastUtils.show("Please enter the correct email address")
					}
				}
			}
		}).maskColor = resources.getColor(R.color.black_50)
	}
	
	private fun setBottomText() {
		val fullText = "By using OpenAsk, you agree to the terms of service and privacy policy"
		val clickableText1 = "terms of service"
		val clickableText2 = "privacy policy"
		
		val spannableString = SpannableString(fullText)
		
		val clickableSpan1: ClickableSpan = object : ClickableSpan() {
			override fun onClick(view: View) {
				WebActivity.launch(this@LoginActivity,
					"terms of service",
					"https://openask.gitbook.io/openask-terms-of-service/")
			}
			
			override fun updateDrawState(ds: TextPaint) {
				super.updateDrawState(ds)
				ds.isUnderlineText = true // 设置下划线
			}
		}
		
		val clickableSpan2: ClickableSpan = object : ClickableSpan() {
			override fun onClick(view: View) {
				WebActivity.launch(this@LoginActivity,
					"privacy policy",
					"https://openask.gitbook.io/openask-privacy-policy/")
			}
			
			override fun updateDrawState(ds: TextPaint) {
				super.updateDrawState(ds)
				ds.isUnderlineText = true // 设置下划线
			}
		}
		
		val startIndex1 = fullText.indexOf(clickableText1)
		val endIndex1 = startIndex1 + clickableText1.length
		
		val startIndex2 = fullText.indexOf(clickableText2)
		val endIndex2 = startIndex2 + clickableText2.length
		
		spannableString.setSpan(clickableSpan1,
			startIndex1,
			endIndex1,
			Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		spannableString.setSpan(clickableSpan2,
			startIndex2,
			endIndex2,
			Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
		
		mBinding.tvService.text = spannableString
		mBinding.tvService.movementMethod = LinkMovementMethod.getInstance()
	}
	
	private fun getTwitterInfo() {
		val provider =
			OAuthProvider.newBuilder("twitter.com") //		provider.addCustomParameter("lang",'')
		val pendingResultTask = firebaseAuth.pendingAuthResult
		
		if (pendingResultTask != null) {            // There's something already here! Finish the sign-in for your user.
			pendingResultTask.addOnSuccessListener {                    // User is signed in.
				LogUtils.e(TAG, "pendingResultTask addOnSuccessListener" + it.toString())
			}.addOnFailureListener {                    // Handle failure.
				LogUtils.e(TAG, "pendingResultTask addOnFailureListener")
			}
		} else {            // There's no pending result so you need to start the sign-in flow.
			// See below.
		}
		
		firebaseAuth.startActivityForSignInWithProvider( /* activity = */this, provider.build())
				.addOnSuccessListener {                // User is signed in.
					LogUtils.e(TAG, "firebaseAuth addOnSuccessListener" + it.toString())
					
					LogUtils.e(TAG, "Str111 = ")
					var str = Gson().toJson(it.additionalUserInfo?.profile)
					LogUtils.e(TAG, "Str = " + it.user?.uid)
					LogUtils.e(TAG, "Str = " + str)
					var extInfo = TwitterExtInfoModel(it.user?.uid,
						it.user?.providerId,
						it.user?.photoUrl.toString(),
						it.user?.displayName,
						it.additionalUserInfo?.profile?.get("screen_name").toString(),
						it.additionalUserInfo?.profile?.get("description").toString(),
						it.additionalUserInfo?.profile?.get("followers_count").toString().toInt())
					lifecycleScope.launch { twitterLogin(extInfo) }
				}.addOnFailureListener {                // Handle failure.
					LogUtils.e(TAG, "firebaseAuth addOnFailureListener " + it.toString())
				}
	}
	
	private suspend fun getNonce(walletAddress: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/user/wallet-login/get-sign-nonce").add("clientType", 7)
				.add("walletAddress", walletAddress).toAwaitResponse<NonceData>().awaitResult {
					LogUtils.e(TAG, "getNonce awaitResult = " + Gson().toJson(it))
					dismissLoadingDialog()
					startSign(walletAddress, it.nonce!!)
				}.onFailure {
					LogUtils.e(TAG, "getNonce onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun startSign(walletAddress: String, nonce: String) {
		showLoadingDialog("Loading...")
		val signature = Signature() //标识链
		val blockchains: MutableList<Blockchain> = ArrayList()
		blockchains.add(Blockchain("ethereum", "1"))
		signature.blockchains = blockchains
		
		signature.dappName = resources.getString(R.string.app_name)
		signature.dappIcon =
			"https://eosknights.io/img/icon.png" //开发者自己定义的业务ID，用于标识操作，在授权登录中，需要设置该字段
		//开发者自己定义的业务ID，用于标识操作，在授权登录中，需要设置该字段
		signature.actionId =
			"web-db4c5466-1a03-438c-90c9-2172e8becea5" //签名类型 从Android 1.6.8版本开始，EVM网络支持 ethPersonalSign ethSignTypedDataLegacy ethSignTypedData
		//ethSignTypedData_v4 四种签名类型
		//签名类型 从Android 1.6.8版本开始，EVM网络支持 ethPersonalSign ethSignTypedDataLegacy ethSignTypedData
		//ethSignTypedData_v4 四种签名类型
		signature.signType = "ethPersonalSign" //ethSign类型，签名的数据是16进制字符串
		//ethSign类型，签名的数据是16进制字符串
		signature.message =
			nonce //开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		//开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		//		signature.setCallbackUrl("http://115.205.0.178:9011/taaBizApi/taaInitData")
		TPManager.getInstance().signature(this, signature, object : TPListener {
			override fun onSuccess(s: String) {
				dismissLoadingDialog()
				
				LogUtils.e(TAG, "startSign onSuccess " + s)
//				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
				
				var data = Gson().fromJson(s, TPWalletSignData::class.java)
				
				lifecycleScope.launch {
					login(walletAddress, nonce, data.sign!!)
				}
			}
			
			override fun onError(s: String) {
				showFailedDialog(s)
//				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onCancel(s: String) {
				showFailedDialog("You canceled the signature")
//				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
		})
	}
	
	private suspend fun login(walletAddress: String, nonce: String, signature: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/user/wallet-login/verify-sign")
				.add("clientType", 7)
				.add("walletAddress", walletAddress).add("nonce", nonce).add("signature", signature)
				.toAwaitResponse<UserInfo>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					MMKV.defaultMMKV().encode("userInfo", it)
					
					OpenAskApplication.instance.userInfo = it
					OpenAskApplication.instance.initRxHttp(it.token!!)
					MainActivity.launch(this)
					finish()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun twitterLogin(info: TwitterExtInfoModel) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/user/open-ask/firebase-twitter-login")
				.add("clientType", 7)
				.add("bio", info.bio)
				.add("displayName", info.displayName)
				.add("followersCount", info.followersCount)
				.add("photoUrl", info.photoUrl)
				.add("providerId", info.providerId)
				.add("screenName", info.screenName)
				.add("twitterUid", info.twitterUid)
				.toAwaitResponse<UserInfo>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					MMKV.defaultMMKV().encode("userInfo", it)
					
					OpenAskApplication.instance.userInfo = it
					OpenAskApplication.instance.initRxHttp(it.token!!)
					MainActivity.launch(this)
					finish()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun getWalletAddress() {
		val authorize = Authorize()
		val blockchains: MutableList<Blockchain> = ArrayList()
		if (BuildConfig.DEBUG){
			blockchains.add(Blockchain("ethereum", "11155111"))
		}else{
			blockchains.add(Blockchain("ethereum", "1"))
		}
		
		authorize.blockchains = blockchains
		
		authorize.dappName = resources.getString(R.string.app_name)
		authorize.dappIcon = "https://eosknights.io/img/icon.png" //开发者自己定义的业务id
		//开发者自己定义的业务id
		authorize.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		TPManager.getInstance().authorize(this, authorize, object : TPListener {
			override fun onSuccess(s: String) {
				LogUtils.e(TAG, "onSuccess " + s)
				//Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
				var data = Gson().fromJson(s, TPWalletLoginData::class.java)
				
				runOnUiThread {
					lifecycleScope.launch {
						data.wallet?.let { getNonce(it) }
					}
				}
			}
			
			override fun onError(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onCancel(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
		})
	}
	
}