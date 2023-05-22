package fans.openask.ui.activity

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.ActivitySenseiProfileBinding
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogEavesdropBinding
import fans.openask.http.errorMsg
import fans.openask.model.AnswerStateModel
import fans.openask.model.AsksModel
import fans.openask.model.SenseiAnswerModel
import fans.openask.model.SenseiListModel
import fans.openask.model.SenseiProifileData
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.ui.adapter.SenseiAnswerAdapter
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
	
	private var pageSize = 3
	private var pageNo = 1
	
	lateinit var userId: String
	
	lateinit var asksAdapter: SenseiAnswerAdapter
	var list = mutableListOf<SenseiAnswerModel>()
	
	var mediaPlayer: MediaPlayer? = null
	
	companion object {
		fun launch(activity: BaseActivity, userNo: String, senseiUsername: String, userId: String) {
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
		
		asksAdapter = SenseiAnswerAdapter(list)
		mBinding.recyclerView.adapter = asksAdapter
	}
	
	override fun initData() {
		userNo = intent.getStringExtra("userNo")!!
		senseiUsername = intent.getStringExtra("senseiUsername")!!
		userId = intent.getStringExtra("userId")!!
		
		lifecycleScope.launch {
			getSenseiProfile()
			getAnswerList(userId!!)
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
		
		asksAdapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				if (list[position].answerState != null) {
					if (list[position].answerState!!.answerContent.isNullOrEmpty()) {//未付费
						//付费
						
					} else {//已经付费
						list[position].answerState!!.answerContent?.let { play(it) }
					}
				} else {
					ToastUtils.show("Loading failed!")
				}
			}
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
		mediaPlayer?.setOnPreparedListener(object : MediaPlayer.OnPreparedListener {
			override fun onPrepared(p0: MediaPlayer?) {
				dismissLoadingDialog()
				mediaPlayer?.start()
			}
		})
		
		mediaPlayer?.setOnBufferingUpdateListener(object : OnBufferingUpdateListener {
			override fun onBufferingUpdate(p0: MediaPlayer?, p1: Int) {
				LogUtils.e(TAG, "onBufferingUpdate $p1")
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
		
		mBinding.tvAnswers.text = "Answers(${data.answersCount})"
		mBinding.tvAsks.text = "Asks(${data.askCount})"
		
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
				.add("pageSize", pageSize).add("pageNo", pageNo)
				.toAwaitResponse<List<SenseiAnswerModel>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					if (pageNo == 1) {
						list.clear()
					}
					
					list.addAll(it)
					asksAdapter.notifyDataSetChanged()
					
					var array = mutableListOf<String>()
					for (i in it.indices) {
						it[i].questionId?.let { it1 -> array.add(it1) }
					}
					getAnswerState(array)
					
					if (list.size == 0) {
						mBinding.layoutEmpty.visibility = View.VISIBLE
						mBinding.tvEmptyTitle.text = "No questions just yet"
						mBinding.tvEmptyTitle2.text =
							"Ask questions of Sensei you are interested in"
						mBinding.ivEmpty.setImageResource(R.drawable.icon_ask_empty)
					} else {
						mBinding.layoutEmpty.visibility = View.GONE
					}
					
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
	
	private suspend fun getAnswerState(questionIds: MutableList<String>) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/answer/by-questionIds")
				.add("questionIds", questionIds)
				.toAwaitResponse<Map<String, AnswerStateModel>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					for (i in list.indices) {
						var model = it.get(list[i].questionId)
						model?.let { list[i].answerState = model }
						asksAdapter.notifyDataSetChanged()
					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showEavesdropDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_eavesdrop) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogEavesdropBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
				
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
}