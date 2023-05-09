package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import fans.openask.R
import fans.openask.databinding.ActivityLoginBinding
import fans.openask.http.errorMsg
import fans.openask.utils.EmailVerifyUtil
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
class LoginActivity: BaseActivity() {
	
	var TAG = "LoginActivity"
	
	lateinit var mBinding:ActivityLoginBinding
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,LoginActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_login
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF",true)
	}
	
	override fun initData() {
	
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvSignupValue.setOnClickListener {
			SignupActivity.launch(this)
			finish()
		}
		
		mBinding.tvBtnContinue.setOnClickListener {
			if (EmailVerifyUtil.isEmail(mBinding.etEmail.text.toString())){
				mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_e2e2e2_8dp)
				mBinding.tvErrorMsg.visibility = View.GONE
				lifecycleScope.launch {
					checkUserExist(mBinding.etEmail.text.toString())
				}
			}else{
				mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_fa5151_8dp)
				mBinding.tvErrorMsg.text = "Please enter a valid email address."
				mBinding.tvErrorMsg.visibility = View.VISIBLE
			}
		}
		
		mBinding.viewTwitter.setOnClickListener { ToastUtils.show("Developing...") }
		mBinding.viewWallet.setOnClickListener { ToastUtils.show("Developing...") }
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private suspend fun checkUserExist(email:String){
		showLoadingDialog("Loading...")
		RxHttp.get("/user/check-exist")
			.add("email", email)
			.toAwaitResponse<Boolean>()
			.awaitResult {
				LogUtils.e(TAG,"awaitResult = "+it.toString())
				dismissLoadingDialog()
				if (it) {//has registed
					mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_e2e2e2_8dp)
					mBinding.tvErrorMsg.visibility = View.GONE
					VerificationCodeActivity.launch(this,email,true)
					finish()
				} else {//new user
					mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_fa5151_8dp)
					mBinding.tvErrorMsg.text = "No account yet, please register first."
					mBinding.tvErrorMsg.visibility = View.VISIBLE
				}
			}.onFailure {
				LogUtils.e(TAG, "onFailure = "+it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
}