package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.bumptech.glide.Glide
import com.fans.donut.data.file.OSSTokenData
import com.fans.donut.listener.OnItemClickListener
import com.fans.donut.utils.oss.FileUploader
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.DialogAnswerBinding
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.DialogFundAddedBinding
import fans.openask.databinding.FragmentAwaitingBinding
import fans.openask.databinding.FragmentSenseisBinding
import fans.openask.http.errorMsg
import fans.openask.model.AsksModel
import fans.openask.model.SenseiListModel
import fans.openask.model.WalletData
import fans.openask.model.event.UpdateNumEvent
import fans.openask.ui.activity.AddFundActivity
import fans.openask.ui.adapter.SenseiListAdapter
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.activity.SenseiProfileActivity
import fans.openask.ui.adapter.AwaitingAnswerAdapter
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import me.linjw.demo.lame.Encoder
import me.linjw.demo.lame.Recorder
import org.greenrobot.eventbus.EventBus
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.io.File
import java.io.IOException


/**
 *
 * Created by Irving
 */
class AwaitingFragment : BaseFragment() {
	private val TAG = "AwaitingFragment"
	
	private val REQUEST_RECORD_AUDIO_PERMISSION = 200
	
	private var pageNo = 1
	private var pageSize = 10
	
	private var outputFilePath: String? = null
	
	var mediaRecorder: MediaRecorder? = null
	
	var list = mutableListOf<AsksModel>()
	lateinit var adapter: AwaitingAnswerAdapter
	
	private lateinit var mBinding: FragmentAwaitingBinding
	
	override fun getResId(): Int {
		return R.layout.fragment_awaiting
	}
	
	override fun initView(contentView: View) {
		adapter = AwaitingAnswerAdapter(list)
		mBinding.recyclerView.adapter = adapter
	}
	
	override fun initData() {
		lifecycleScope.launch { getList() }
	}
	
	override fun initEvent() {
		mBinding.refreshLayout.setOnRefreshListener {
			pageNo = 1
			lifecycleScope.launch { getList() }
		}
		
		mBinding.refreshLayout.setOnLoadMoreListener {
			pageNo += 1
			lifecycleScope.launch { getList() }
		}
		
		adapter.onItemAnswerClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				showAnswerDialog(list[position].questionId!!)
			}
		}
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onDestroy() {
		stopRecording()
		super.onDestroy()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	private fun showAnswerDialog(questionId:String){
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_answer) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAnswerBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtnSubmit.setOnClickListener {
					if (!outputFilePath.isNullOrEmpty()){
						lifecycleScope.launch {
							val array = outputFilePath!!.split("/")
							getToken(array[array.size-1],outputFilePath!!, questionId,10.0)
						}
					}else{
						ToastUtils.show("Please answer ")
					}
				}
				
				binding.tvBtnRecord.setOnTouchListener(object :OnTouchListener{
					override fun onTouch(p0: View?, p1: MotionEvent): Boolean {
						if (p1.action == MotionEvent.ACTION_DOWN) {
							startRecording()
						} else if (p1.action == MotionEvent.ACTION_UP) {
							stopRecording()
						}
						return true
					}
				})
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun getList() {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/answers-feed")
				.add("askQuestionStatus", 0)
				.add("clientId", 7)
				.add("pageNo", pageNo)
				.add("pageSize", pageSize)
				.toAwaitResponse<List<AsksModel>>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					
					if (pageNo == 1) {
						list.clear()
					}
					
					this.list.addAll(it)
					adapter.notifyDataSetChanged()
					
					EventBus.getDefault()
							.post(UpdateNumEvent(UpdateNumEvent.EVENT_TYPE_AWAITING, list.size))
					
					if (list.size == 0){
						mBinding.layoutEmpty.visibility = View.VISIBLE
					}else{
						mBinding.layoutEmpty.visibility = View.GONE
					}
					
					if (it.size < pageSize) {
						mBinding.refreshLayout.setEnableLoadMore(false)
					} else {
						mBinding.refreshLayout.setEnableLoadMore(true)
					}
					
					mBinding.refreshLayout.finishRefresh()
					mBinding.refreshLayout.finishLoadMore()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getToken(fileName:String,filePath:String,questionId:String,contentSize:Double){
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/oss/get-token")
				.add("fileName", fileName)
				.add("temp", 1)
				.toAwaitResponse<OSSTokenData>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					
					uploadFile(it,filePath,questionId,contentSize)
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun uploadFile(data:OSSTokenData,filePath: String,questionId:String,contentSize:Double){
		(activity as MainActivity).showLoadingDialog("Loading...")
		context?.let {
			FileUploader().uploadFile(it,data,filePath,object :FileUploader.UploadListener{
				override fun onStart() {
					(activity as MainActivity).showLoadingDialog("Uploading...")
				}
				
				override fun onProgress(progress: Int) {
					(activity as MainActivity).showLoadingDialog("Uploading($progress/100)")
				}
				
				override fun onSuccess(request: PutObjectRequest?) {
					(activity as MainActivity).dismissLoadingDialog()
					
					lifecycleScope.launch {
						submitAnswer(questionId,data.fileName!!,contentSize)
					}
				}
				
				override fun onFailure(msg: String) {
					(activity as MainActivity).showFailedDialog(msg)
				}
				
			})
		}
	}
	
	private suspend fun submitAnswer(questionId:String,content:String,contentSize:Double){
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/answer/submit-answer")
				.add("answerContentType", 1)
				.add("content", content)
				.add("contentSize", contentSize)
				.add("questionId", questionId)
				.toAwaitResponse<OSSTokenData>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private val recorder = Recorder(object :Recorder.IRecordListener{
		override fun onRecord(pcm: ByteArray, dataLen: Int) {
			encoder.encode(pcm, dataLen)
		}
	})
	private val encoder = Encoder()
	
	companion object {
		init {
			System.loadLibrary("lame")
		}
		
		private const val TAG = "MainActivity"
		
		private const val AUDIO_SOURCE = MediaRecorder.AudioSource.MIC
		private const val SAMPLE_RATE = 44100
		private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
		private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO // 单通道
//        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_STEREO // 双通道
		
		@RequiresApi(Build.VERSION_CODES.M)
		val CHANNEL_COUNT = AudioFormat.Builder()
				.setChannelMask(CHANNEL_CONFIG)
				.build()
				.channelCount
	}
	
	private fun startRecording() {
		LogUtils.e(TAG,"startRecording")
		if (ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
				Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
			ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
				Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
			//设置输出文件路径
			outputFilePath = context?.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + System.currentTimeMillis() + "_android_audio.mp3"
			
			encoder.start(File(outputFilePath), SAMPLE_RATE, CHANNEL_COUNT)
			recorder.start(AUDIO_SOURCE, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
			
		} else {
			ActivityCompat.requestPermissions(activity as Activity,
				arrayOf(Manifest.permission.RECORD_AUDIO, Manifest.permission.MANAGE_EXTERNAL_STORAGE),
				REQUEST_RECORD_AUDIO_PERMISSION)
		}
	}
	
	private fun stopRecording() {
		LogUtils.e(TAG,"stopRecording")
		
		recorder.stop()
		encoder.stop()
	}
}