package com.timely.openask.ui.fragment

import android.view.View
import com.timely.openask.R

/**
 *
 * Created by Irving
 */
class SenseisFragment :BaseFragment(){
	
	override fun getResId(): Int {
		return R.layout.fragment_senseis
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
	}
	
	override fun initEvent() {
	}
	
	override fun setDataBindingView(view: View) {
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#221635",false)
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden)
			setStatusBarColor("#221635",false)
	}
	
}