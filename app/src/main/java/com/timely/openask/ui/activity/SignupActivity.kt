package com.timely.openask.ui.activity

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import com.timely.openask.R
import com.timely.openask.databinding.ActivitySignupBinding
import com.timely.openask.utils.EmailVerifyUtil

/**
 *
 * Created by Irving
 */
class SignupActivity: BaseActivity() {
	
	lateinit var mBinding:ActivitySignupBinding
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,SignupActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_signup
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF",true)
	}
	
	override fun initData() {
	
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvLoginValue.setOnClickListener {
			LoginActivity.launch(this)
		}
		
		mBinding.tvBtnContinue.setOnClickListener {
			if (EmailVerifyUtil.isEmail(mBinding.etEmail.text.toString())){
				mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_e2e2e2_8dp)
				mBinding.tvErrorMsg.visibility = View.GONE
				
				VerificationCodeActivity.launch(this,mBinding.etEmail.text.toString(),false)
				finish()
			}else{
				mBinding.etEmail.setBackgroundResource(R.drawable.shape_stroke_fa5151_8dp)
				mBinding.tvErrorMsg.text = "Please enter a valid email address."
				mBinding.tvErrorMsg.visibility = View.VISIBLE
			}
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
}