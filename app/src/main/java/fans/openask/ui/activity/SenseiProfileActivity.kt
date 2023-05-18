package fans.openask.ui.activity

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import fans.openask.R
import fans.openask.databinding.ActivitySenseiProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.AsksModel
import fans.openask.model.SenseiListModel
import fans.openask.model.SenseiProifileData
import fans.openask.ui.adapter.AsksAdapter
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
	
	private var pageSize = 20
	private var pageNo = 1
	
	lateinit var userId:String
	
	lateinit var asksAdapter: AsksAdapter
	var list = mutableListOf<AsksModel>()
	
	var mediaPlayer: MediaPlayer? = null
	
	companion object {
		fun launch(activity: BaseActivity, userNo: String, senseiUsername: String,userId: String) {
			var intent = Intent(activity, SenseiProfileActivity::class.java)
			intent.putExtra("userNo", userNo)
			intent.putExtra("senseiUsername", senseiUsername)
			intent.putExtra("userId", userId)
			activity.startActivity(intent)
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_sensei_profile
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF", true)
		
		asksAdapter = AsksAdapter(list)
		mBinding.recyclerView.adapter = asksAdapter
	}
	
	override fun initData() {
		userNo = intent.getStringExtra("userNo")!!
		senseiUsername = intent.getStringExtra("senseiUsername")!!
		userId = intent.getStringExtra("userId")!!
		
		lifecycleScope.launch {
			getSenseiProfile()
//			getAnswerList(userId!!)
		}
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvAnswers.setOnClickListener {
			mBinding.tvAnswers.isEnabled = false
			mBinding.tvAsks.isEnabled = true
			
			pageNo = 1
			lifecycleScope.launch { getAnswerList(userId) }
		}
		
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAnswers.isEnabled = true
			mBinding.tvAsks.isEnabled = false
			
			pageNo = 1
			lifecycleScope.launch { getAsksList(userId) }
		}
		
		mBinding.ivAsk.setOnClickListener {
		
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		mediaPlayer?.release()
		mediaPlayer = null
		super.onDestroy()
	}
	
	private fun play(url: String) {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		mediaPlayer?.stop()
		
		showLoadingDialog("Voice Loading...")
		mediaPlayer?.setOnPreparedListener(object :MediaPlayer.OnPreparedListener{
			override fun onPrepared(p0: MediaPlayer?) {
				dismissLoadingDialog()
				mediaPlayer?.start()
			}
		})
		
		mediaPlayer?.setOnBufferingUpdateListener(object :OnBufferingUpdateListener{
			override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {
				LogUtils.e(TAG,"onBufferingUpdate $p1")
			}
			
		})
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepareAsync()
		} catch (e: Exception) {
			e.printStackTrace()
		}
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
				.error(R.drawable.icon_avator).circleCrop().into(mBinding.ivAvator)
		
		mBinding.tvNickname.text = data.displayName
		mBinding.tvUsername.text = data.username
		mBinding.tvByName.text = "by " + data.username
		mBinding.tvFollowerCount.text = data.followersCount + " followers"
		data.selfIntroAudioCreateTime?.let {
			mBinding.tvPostTime.text =
				"Posted on " + SimpleDateFormat("K:mmaa,MMM dd,yyyy").format(data.selfIntroAudioCreateTime)
		}
		
		if (data.audioDuration != null) {
			val minutes = data.audioDuration!! / 60
			val seconds = data.audioDuration!! % 60
			mBinding.tvIntroDuration.text = String.format("%02d:%02d", minutes, seconds)
		}
		
		mBinding.ivPlay.setOnClickListener {
			if (!data.selfIntroUrl.isNullOrEmpty()) {
				play("https://openask-test-public.oss-us-west-1.aliyuncs.com/297b29bcac59445891ee10d6b9f5ae6d/1684392962508_h5_audio_a.mp3")
			} else {
				ToastUtils.show("No Voice")
			}
		}
	}
	
	private suspend fun getAnswerList(userId: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/user-page/answers").add("userId", userId).add("clientId", 7)
				.add("pageSize", pageSize).add("pageNo", pageNo).toAwaitResponse<List<AsksModel>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					if (pageNo == 1) {
						list.clear()
					}
					
					if (it.size < pageSize) {
						mBinding.refreshLayout.setEnableLoadMore(false)
					} else {
						mBinding.refreshLayout.setEnableLoadMore(true)
					}
					
					mBinding.refreshLayout.finishRefresh()
					mBinding.refreshLayout.finishLoadMore()
					
					list.addAll(it)
					asksAdapter.notifyDataSetChanged()
					mBinding.tvAsks.text = "Your Asks(${list.size})"
					
					//					if (list.size == 0) {
					//						mBinding.layoutEmpty.visibility = View.VISIBLE
					//						mBinding.tvEmptyTitle.text = "No questions just yet"
					//						mBinding.tvEmptyTitle2.text =
					//							"Ask questions of Sensei you are interested in"
					//						mBinding.ivEmpty.setImageResource(R.drawable.icon_ask_empty)
					//					} else {
					//						mBinding.layoutEmpty.visibility = View.GONE
					//					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getAsksList(userId: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/my-eavesdropped").add("userId", userId).add("clientId", 7)
				.add("pageSize", pageSize).add("pageNo", pageNo).toAwaitResponse<List<Any>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
}