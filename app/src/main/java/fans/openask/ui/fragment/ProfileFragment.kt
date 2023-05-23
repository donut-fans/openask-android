package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.databinding.FragmentProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.AsksModel
import fans.openask.model.UserInfo
import fans.openask.model.WalletData
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.utils.LogUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.io.IOException


/**
 *
 * Created by Irving
 */
class ProfileFragment : BaseFragment() {
	private val TAG = "ProfileFragment"
	
	private val REQUEST_RECORD_AUDIO_PERMISSION = 200
	
	lateinit var mBinding: FragmentProfileBinding
	
	private var outputFile: String? = null
	
	var mediaRecorder: MediaRecorder? = null
	
	private var pageSize = 20
	private var pageNo = 1
	
	lateinit var asksAdapter: AsksAdapter
	var list = mutableListOf<AsksModel>()
	
	var mediaPlayer: MediaPlayer? = null
	
	override fun getResId(): Int {
		return R.layout.fragment_profile
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		if (userInfo != null) {
			setProfileInfo(userInfo)
		}
		
		asksAdapter = AsksAdapter(list)
		mBinding.recyclerView.adapter = asksAdapter
		
		lifecycleScope.launch {
			getWallet()
			getAskedList()
		}
	}
	
	override fun initEvent() {
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAsks.isEnabled = false
			mBinding.tvEavesdrop.isEnabled = true
			
			pageNo = 1
			lifecycleScope.launch { getAskedList() }
		}
		
		mBinding.tvEavesdrop.setOnClickListener {
			mBinding.tvAsks.isEnabled = true
			mBinding.tvEavesdrop.isEnabled = false
			
			pageNo = 1
			lifecycleScope.launch { getEavesdroppedList() }
		}
		
		mBinding.refreshLayout.setOnRefreshListener {
			pageNo = 1
			if (!mBinding.tvAsks.isEnabled) {
				lifecycleScope.launch { getAskedList() }
			} else {
				lifecycleScope.launch { getEavesdroppedList() }
			}
		}
		
		mBinding.refreshLayout.setOnLoadMoreListener {
			pageNo += 1
			if (!mBinding.tvAsks.isEnabled) {
				lifecycleScope.launch { getAskedList() }
			} else {
				lifecycleScope.launch { getEavesdroppedList() }
			}
		}
		
		asksAdapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				list[position].answerContent?.let { play(it) }
			}
		}
		
		mBinding.ivBtnBecome.setOnTouchListener(object : View.OnTouchListener {
			override fun onTouch(p0: View?, p1: MotionEvent): Boolean {
				if (p1.action == MotionEvent.ACTION_DOWN) {
					startRecording()
				} else if (p1.action == MotionEvent.ACTION_UP) {
					stopRecording()
				}
				
				return true
			}
		})
		
		mBinding.ivAvator.setOnClickListener {
			LogUtils.e(TAG,"outputFile $outputFile")
			outputFile?.let { it1 -> play(it1) } }
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		mediaPlayer?.release()
		mediaPlayer = null
		
		stopRecording()
		super.onDestroy()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
	}
	
	private fun startRecording() {
		if (ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
				Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
				Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			// 设置输出文件路径
			outputFile = context?.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + System.currentTimeMillis() + ".wav"
			
			mediaRecorder = MediaRecorder()
			mediaRecorder?.reset()
			mediaRecorder?.setAudioSource(MediaRecorder.AudioSource.MIC)
			mediaRecorder?.setOutputFormat(MediaRecorder.OutputFormat.DEFAULT)
			mediaRecorder?.setAudioEncoder(MediaRecorder.AudioEncoder.DEFAULT)
			mediaRecorder?.setMaxDuration(60 * 1000)
			mediaRecorder?.setOutputFile(outputFile)
			
			try {
				mediaRecorder?.prepare()
			} catch (e: IOException) {
				e.printStackTrace()
			}
			mediaRecorder?.start()
			
		} else {
			ActivityCompat.requestPermissions(activity as Activity,
				arrayOf(Manifest.permission.RECORD_AUDIO,Manifest.permission.MANAGE_EXTERNAL_STORAGE),
				REQUEST_RECORD_AUDIO_PERMISSION)
		}
	}
	
	private fun stopRecording() {
		mediaRecorder?.stop()
		mediaRecorder?.release()
		mediaRecorder = null
	}
	
	private fun play(url: String) {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		mediaPlayer?.stop()
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepare()
		} catch (e: Exception) {
			e.printStackTrace()
		}
		
		mediaPlayer?.start()
	}
	
	private fun setProfileInfo(userInfo: UserInfo) {
		Glide.with(this).load(userInfo.headIcon).placeholder(R.drawable.icon_avator)
				.error(R.drawable.icon_avator).circleCrop().into(mBinding.ivAvator)
		
		mBinding.tvUsername.text = userInfo.nickname
	}
	
	private suspend fun getAskedList() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/user-questions").add("clientType", 7).add("clientId", 7)
				.add("pageSize", pageSize).add("pageNo", pageNo).toAwaitResponse<List<AsksModel>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
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
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getEavesdroppedList() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/my-eavesdropped").add("clientType", 7).add("clientId", 7)
				.add("pageSize", pageSize).add("pageNo", pageNo).toAwaitResponse<List<Any>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getWallet() {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet").add("clientType", 7).add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					mBinding.tvBalanceValue.text = "$" + it.balance
					mBinding.tvEarningValue.text = "$" + it.totalEarning
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
}