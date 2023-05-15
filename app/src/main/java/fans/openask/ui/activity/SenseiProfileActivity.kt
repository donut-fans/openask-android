package fans.openask.ui.activity

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import fans.openask.R
import fans.openask.databinding.ActivitySenseiProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.SenseiListModel
import fans.openask.model.SenseiProifileData
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.lang.Exception
import java.text.SimpleDateFormat

/**
 *
 * Created by Irving
 */
class SenseiProfileActivity : BaseActivity() {
	private val TAG = "SenseiProfileActivity"
	
	private lateinit var mBinding: ActivitySenseiProfileBinding
	
	private var userNo = ""
	private var senseiUsername = ""
	
	var mediaPlayer:MediaPlayer? = null
	
	companion object {
		fun launch(activity: BaseActivity, userNo: String, senseiUsername: String) {
			var intent = Intent(activity, SenseiProfileActivity::class.java)
			intent.putExtra("userNo", userNo)
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
		userNo = intent.getStringExtra("userNo")!!
		senseiUsername = intent.getStringExtra("senseiUsername")!!
		
		lifecycleScope.launch { getSenseiProfile() }
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		mediaPlayer?.release()
		mediaPlayer = null
		super.onDestroy()
	}
	
	private fun play(url:String){
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		mediaPlayer?.stop()
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepare()
		}catch (e:Exception){
			e.printStackTrace()
		}
		
		mediaPlayer?.start()
	}
	
	private suspend fun getSenseiProfile() {
		showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/user/user-page/$userNo/$senseiUsername").add("clientType", 7)
			.add("clientId", 7).toAwaitResponse<SenseiProifileData>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				dismissLoadingDialog()
				
				renderUI(it)
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
	private fun renderUI(data: SenseiProifileData) {
		Glide.with(this).load(data.avatarUrl).placeholder(R.drawable.icon_avator)
			.error(R.drawable.icon_avator).into(mBinding.ivAvator)
		
		mBinding.tvNickname.text = data.displayName
		mBinding.tvUsername.text = data.username
		mBinding.tvFollowerCount.text = data.followersCount + " followers"
		data.selfIntroAudioCreateTime?.let {
			mBinding.tvPostTime.text =
				"Post on " + SimpleDateFormat("dd-MM-yyyy").format(data.selfIntroAudioCreateTime)
		}
		mBinding.tvTime.text = data.audioDuration
		
		mBinding.ivPlay.setOnClickListener {
			if (!data.selfIntroUrl.isNullOrEmpty()){
				play(data.selfIntroUrl!!)
			}else{
				ToastUtils.show("No Voice")
			}
		}
	}
	
}