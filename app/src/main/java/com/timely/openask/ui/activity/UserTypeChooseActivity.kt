package com.timely.openask.ui.activity

import android.content.Intent
import android.view.View
import androidx.databinding.DataBindingUtil
import com.timely.openask.R
import com.timely.openask.databinding.ActivityUserTypeBinding

/**
 *
 * Created by Irving
 */
class UserTypeChooseActivity: BaseActivity() {
	
	private lateinit var mBinding: ActivityUserTypeBinding
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,UserTypeChooseActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_user_type
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF",true)
	}
	
	override fun initData() {
	
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.viewSifu.setOnClickListener {
			VerifySenseiCodeActivity.launch(this)
			finish()
		}
		
		mBinding.viewLearner.setOnClickListener {
			finish()
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
}