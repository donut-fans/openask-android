package fans.openask.ui.activity

import android.view.View
import androidx.lifecycle.lifecycleScope
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.OpenAskApplication
import fans.openask.http.errorMsg
import fans.openask.model.RemindCountData
import fans.openask.model.UserInfo
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

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
		lifecycleScope.launch {
			getTimeStamp()
		}
	}
	
	override fun initEvent() {
	}
	
	override fun setBindingView(view: View) {
	}
	
	private suspend fun getTimeStamp() {
		RxHttp.get("/common/ts")
				.toAwaitResponse<Long>()
				.awaitResult {
					OpenAskApplication.instance.timestamp = it
					
					var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
					
					if (userInfo == null){
						LoginActivity.launch(this)
					}else{
						MainActivity.launch(this)
						OpenAskApplication.instance.initRxHttp(userInfo?.token!!)
						OpenAskApplication.instance.userInfo = userInfo
					}
					
					finish()
				}.onFailure {
					ToastUtils.show(it.errorMsg)
				}
	}
}