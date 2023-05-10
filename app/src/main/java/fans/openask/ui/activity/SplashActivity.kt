package fans.openask.ui.activity

import android.view.View
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.OpenAskApplication
import fans.openask.model.UserInfo

/**
 *
 * Created by Irving
 */
class SplashActivity: BaseActivity() {
	
	override fun getResId(): Int {
		return R.layout.activity_splash
	}
	
	override fun initView() {
		setStatusBarTranParent()
	}
	
	override fun initData() {
		var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		
//		if (userInfo == null){
			LoginActivity.launch(this)
//		}else{
//			MainActivity.launch(this)
//			OpenAskApplication.instance.initRxHttp(userInfo?.token!!)
//		}
		
		finish()
	}
	
	override fun initEvent() {
	}
	
	override fun setBindingView(view: View) {
	}
	
}