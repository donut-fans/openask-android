package com.timely.openask.ui.activity

import android.content.Intent
import android.view.View
import com.timely.openask.R

/**
 *
 * Created by Irving
 */
class SenseiVerifyStep1Activity:BaseActivity() {
	
	companion object{
		fun launch(activity: BaseActivity){
			activity.startActivity(Intent(activity,SenseiVerifyStep1Activity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_sensei_verify_step1
	}
	
	override fun initView() {
		
	}
	
	override fun initData() {
		
	}
	
	override fun initEvent() {
		
	}
	
	override fun setBindingView(view: View) {
		
	}
}