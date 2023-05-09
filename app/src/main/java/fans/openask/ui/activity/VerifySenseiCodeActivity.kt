package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import fans.openask.R
import fans.openask.databinding.ActivityVerifySenseiCodeBinding
import fans.openask.http.errorMsg
import fans.openask.utils.LogUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

/**
 *
 * Created by Irving
 */
class VerifySenseiCodeActivity:BaseActivity() {
	private val TAG = "VerifySenseiCodeActivity"
	
	lateinit var mBinding:ActivityVerifySenseiCodeBinding
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,VerifySenseiCodeActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_verify_sensei_code
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF",true)
	}
	
	override fun initData() {
		
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvBtnContinue.setOnClickListener {
			lifecycleScope.launch {
				verifySenseiCode(mBinding.etCode.text.toString())
			}
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private suspend fun verifySenseiCode(code:String){
		showLoadingDialog("Loading...")
		RxHttp.get("/user/verify-invite-code")
			.add("inviteCode", code)
			.toAwaitResponse<Boolean>()
			.awaitResult {
				LogUtils.e(TAG,"awaitResult = "+it.toString())
				dismissLoadingDialog()
				
				SenseiVerifyStep1Activity.launch(this)
				finish()
			}.onFailure {
				LogUtils.e(TAG, "onFailure = "+it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
}