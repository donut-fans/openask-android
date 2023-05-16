package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import fans.openask.R
import fans.openask.databinding.FragmentProfileBinding
import fans.openask.model.UserInfo
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

/**
 *
 * Created by Irving
 */
class ProfileFragment :BaseFragment(){
	
	lateinit var mBinding:FragmentProfileBinding
	
	override fun getResId(): Int {
		return R.layout.fragment_profile
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		lifecycleScope.launch { getProfile() }
	}
	
	override fun initEvent() {
	
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden)
		setStatusBarColor("#FFFFFF",true)
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF",true)
	}
	
	private suspend fun getProfile(){
		RxHttp.get("/user/profile")
			.toAwaitResponse<UserInfo>()
			.awaitResult {
				setProfileInfo(it)
			}.onFailure {
			
			}
	}
	
	private fun setProfileInfo(userInfo: UserInfo){
		Glide.with(this)
			.load(userInfo.headIcon)
			.placeholder(R.drawable.icon_avator_default)
			.error(R.drawable.icon_avator_default)
			.circleCrop()
			.into(mBinding.ivAvator)
	}
	
}