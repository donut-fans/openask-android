package fans.openask.ui.activity

import android.content.Intent
import android.os.CountDownTimer
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.OpenAskApplication
import fans.openask.databinding.ActivityVerifyCodeBinding
import fans.openask.http.errorMsg
import fans.openask.model.UserInfo
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

/**
 *
 * Created by Irving
 */
class VerificationCodeActivity : BaseActivity() {
	private var TAG = "VerificationCodeActivity"
	
	lateinit var mBinding: ActivityVerifyCodeBinding
	
	private lateinit var email: String
	private var isLogin = false
	
	private var mTimerTask: CountDownTimer? = null
	
	companion object {
		fun launch(activity: BaseActivity, email: String, isLogin: Boolean) {
			var intent = Intent(activity, VerificationCodeActivity::class.java)
			intent.putExtra("email", email)
			intent.putExtra("isLogin", isLogin)
			activity.startActivity(intent)
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_verify_code
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF", true)
	}
	
	override fun initData() {
		email = intent.getStringExtra("email")!!
		isLogin = intent.getBooleanExtra("isLogin", false)
		
		mBinding.tvEmail.text = email
		
		//		lifecycleScope.launch{
		//			sendCode(email,isLogin)
		//		}
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvBtnResend.setOnClickListener {
			lifecycleScope.launch {
				sendCode(email, isLogin)
			}
		}
		
		mBinding.tvBtnContinue.setOnClickListener {
			if (mBinding.etCode.text.length >= 4) {
				lifecycleScope.launch {
					if (isLogin) login(email, mBinding.etCode.text.toString())
					else login(email, mBinding.etCode.text.toString())
				}
			} else {
				ToastUtils.show("Please input verification code")
			}
		}
	}
	
	override fun onDestroy() {
		mTimerTask?.cancel()
		super.onDestroy()
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private suspend fun sendCode(email: String, isLogin: Boolean) {
		var type: String = if (isLogin) {
			"4"
		} else {
			"11"
		}
		
		showLoadingDialog("Loading...")
		RxHttp.postJson("/validator/send-code").add("email", email).add("type", type)
			.toAwaitResponse<Boolean>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				dismissLoadingDialog()
				
				mBinding.tvBtnResend.text = "Resend"
				startCountdown()
				
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
	private fun startCountdown() {
		mBinding.tvBtnResend.isEnabled = false
		mTimerTask = object : CountDownTimer(60000, 1000) {
			override fun onTick(p0: Long) {
				mBinding.tvBtnResend.text = "Resend(" + p0 / 1000 + ")"
			}
			
			override fun onFinish() {
				mBinding.tvBtnResend.text = "Resend"
				mBinding.tvBtnResend.isEnabled = true
			}
			
		}
		mTimerTask?.start()
	}
	
//	private suspend fun register(email: String, code: String) {
//		RxHttp.postJson("/validator/verify-code")
//			.add("email", email)
//			.add("type", "11")
//			.add("code", code)
//			.toAwaitResponse<Boolean>().awaitResult {
//				LogUtils.e(TAG, "awaitResult = " + it.toString())
//				dismissLoadingDialog()
//
//				UserTypeChooseActivity.launch(this)
//				finish()
//			}.onFailure {
//				LogUtils.e(TAG, "onFailure = " + it.message.toString())
//				showFailedDialog(it.errorMsg)
//			}
//	}
	
	private suspend fun login(email: String, code: String) {
		RxHttp.postJson("/user/login-h5")
			.add("email", email)
			.add("code", code)
			.add("productTYpe",2)
			.toAwaitResponse<UserInfo>()
			.awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				dismissLoadingDialog()
				
				MMKV.defaultMMKV().encode("userInfo",it)
				it.token?.let { it1 -> fans.openask.OpenAskApplication.instance.initRxHttp(it1) }
				
				UserTypeChooseActivity.launch(this)
				finish()
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
}