package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import fans.openask.R
import fans.openask.http.errorMsg
import fans.openask.model.SenseiListModel
import fans.openask.utils.LogUtils
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

/**
 *
 * Created by Irving
 */
class SenseiProfileActivity:BaseActivity() {
	private val TAG = "SenseiProfileActivity"
	
	private var senseiProfileType = ""
	private var senseiUsername = ""
	
	companion object {
		fun launch(activity: BaseActivity, senseiProfileType: String, senseiUsername: String) {
			var intent = Intent(activity, SenseiProfileActivity::class.java)
			intent.putExtra("senseiProfileType", senseiProfileType)
			intent.putExtra("senseiUsername", senseiUsername)
			activity.startActivity(intent)
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_sensei_profile
	}
	
	override fun initView() {
		
	}
	
	override fun initData() {
		
	}
	
	override fun initEvent() {
		
	}
	
	override fun setBindingView(view: View) {
		
	}
	
	private suspend fun getSenseiProfile() {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/user/sensei/user-info/$senseiProfileType}/$senseiUsername}")
			.add("clientType", 7)
			.add("clientId", 7)
			.toAwaitResponse<List<SenseiListModel>>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				dismissLoadingDialog()
				
				
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
}