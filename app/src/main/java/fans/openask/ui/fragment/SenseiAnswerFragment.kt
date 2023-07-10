package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
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
import com.fans.donut.data.file.OSSTokenData
import com.fans.donut.listener.OnItemClickListener
import com.fans.donut.utils.oss.FileUploader
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.ywl5320.wlmedia.WlMedia
import com.ywl5320.wlmedia.enums.WlComplete
import com.ywl5320.wlmedia.enums.WlPlayModel
import com.ywl5320.wlmedia.listener.WlOnMediaInfoListener
import fans.openask.R
import fans.openask.databinding.DialogAnswerBinding
import fans.openask.databinding.DialogEavesdropBinding
import fans.openask.databinding.FragmentAwaitingBinding
import fans.openask.http.errorMsg
import fans.openask.model.AnswerStateModel
import fans.openask.model.EavesdropModel
import fans.openask.model.SenseiAnswerModel
import fans.openask.model.event.UpdateNumEvent
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.adapter.SenseiAnswerAdapter
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


/**
 *
 * Created by Irving
 */
class SenseiAnswerFragment : BaseFragment() {
	private val TAG = "AwaitingFragment"
	
	private var pageNo = 1
	private var pageSize = 10
	
	var list = mutableListOf<SenseiAnswerModel>()
	lateinit var adapter: SenseiAnswerAdapter
	
	var wlMedia: WlMedia? = null
	
	private lateinit var mBinding: FragmentAwaitingBinding
	
	companion object {
		fun getInstance(userId:String):SenseiAnswerFragment{
			val fragment = SenseiAnswerFragment()
			
			var bundle = Bundle()
			bundle.putString("userId",userId)
			fragment.arguments = bundle
			
			return fragment
		}
	}
	
	override fun getResId(): Int {
		return R.layout.fragment_awaiting
	}
	
	override fun initView(contentView: View) {
		adapter = SenseiAnswerAdapter(list)
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
		
		adapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				if (list[position].answerState != null){
					if (list[position].answerState!!.answerContent.isNullOrEmpty()){//未付费
						if (list[position].answerState != null) {
							showEavesdropDialog(list[position].answerState!!.answerId!!,
								list[position].questionId!!)
						}
					}else{//已经付费
						list[position].answerState!!.answerContent?.let { play(it) }
					}
				}else{
					//数据未加载到
					ToastUtils.show("data loading...")
				}
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
	
	override fun onPause() {
		super.onPause()
		wlMedia?.stop()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	
	private fun showEavesdropDialog(answerId:String,questionId:String) {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_eavesdrop) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogEavesdropBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					lifecycleScope.launch { eavesdrop(answerId,questionId) }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun eavesdrop(answerId:String,questionId:String){
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/question/eavesdropped")
				.add("answerId", answerId)
				.add("payMethodId", 8)
				.toAwaitResponse<EavesdropModel>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					var list = mutableListOf<String>()
					list.add(questionId)
					getAnswerState(list)
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getList() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/user-page/answers")
				.add("clientId", 7)
				.add("userId", arguments?.get("userId"))
				.add("pageSize", pageSize)
				.add("pageNo", pageNo)
				.toAwaitResponse<List<SenseiAnswerModel>>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					if (pageNo == 1) {
						list.clear()
					}
					
					this.list.addAll(it)
					adapter.notifyDataSetChanged()
					
					EventBus.getDefault()
							.post(UpdateNumEvent(UpdateNumEvent.EVENT_TYPE_ANSWER, list.size))
					
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
					
					var questionList = mutableListOf<String>()
					for (i in 0 until list.size){
						questionList.add(list[i].questionId!!)
					}
					getAnswerState(questionList)
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getAnswerState(questionIds: MutableList<String>) {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/answer/by-questionIds")
				.add("questionIds", questionIds)
				.toAwaitResponse<Map<String, AnswerStateModel>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					for (i in list.indices) {
						var model = it.get(list[i].questionId)
						model?.let { list[i].answerState = model }
						adapter.notifyDataSetChanged()
					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun play(url: String) {
		if (wlMedia?.isPlaying == true) {
			wlMedia?.stop()
			wlMedia?.release()
		}
		
		if (wlMedia == null) {
			wlMedia = WlMedia()
			wlMedia?.setPlayModel(WlPlayModel.PLAYMODEL_ONLY_AUDIO)
		}
		
		wlMedia?.source = url
		
		(activity as BaseActivity).showLoadingDialog("Voice Loading...")
		wlMedia?.setOnMediaInfoListener(object : WlOnMediaInfoListener {
			override fun onPrepared() {
				LogUtils.e(TAG, "onPrepared")
				(activity as BaseActivity).dismissLoadingDialog()
				wlMedia?.start()
			}
			
			override fun onError(p0: Int, p1: String) {
				LogUtils.e(TAG, "onError $p1")
				(activity as BaseActivity).showFailedDialog(p1)
			}
			
			override fun onComplete(p0: WlComplete?, p1: String?) {
				LogUtils.e(TAG, "onComplete $p1")
			}
			
			override fun onTimeInfo(p0: Double, p1: Double) {
				LogUtils.e(TAG, "onTimeInfo")
			}
			
			override fun onSeekFinish() {
				LogUtils.e(TAG, "onSeekFinish")
			}
			
			override fun onLoopPlay(p0: Int) {
				LogUtils.e(TAG, "onLoopPlay")
			}
			
			override fun onLoad(p0: Boolean) {
				LogUtils.e(TAG, "onLoad")
			}
			
			override fun decryptBuffer(p0: ByteArray?): ByteArray {
				LogUtils.e(TAG, "decryptBuffer")
				return byteArrayOf()
			}
			
			override fun readBuffer(p0: Int): ByteArray {
				LogUtils.e(TAG, "readBuffer")
				return byteArrayOf()
			}
			
			override fun onPause(p0: Boolean) {
				LogUtils.e(TAG, "onPause")
			}
		})

//		wlMedia?.prepared()
		wlMedia?.next()
	}
}