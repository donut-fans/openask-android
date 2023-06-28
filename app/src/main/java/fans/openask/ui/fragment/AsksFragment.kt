package fans.openask.ui.fragment

import android.media.AudioManager
import android.media.MediaPlayer
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.ywl5320.wlmedia.WlMedia
import com.ywl5320.wlmedia.enums.WlComplete
import com.ywl5320.wlmedia.enums.WlPlayModel
import com.ywl5320.wlmedia.listener.WlOnMediaInfoListener
import fans.openask.R
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.FragmentAwaitingBinding
import fans.openask.databinding.FragmentCompletedBinding
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
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.ui.adapter.AwaitingAnswerAdapter
import fans.openask.ui.adapter.CompletedAdapter
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.lang.Exception


/**
 *
 * Created by Irving
 */
class AsksFragment : BaseFragment() {
	private val TAG = "CompletedFragment"
	
	private var pageNo = 1
	private var pageSize = 10
	
	var wlMedia: WlMedia? = null
	
	var list = mutableListOf<AsksModel>()
	lateinit var adapter: AsksAdapter
	
	private lateinit var mBinding: FragmentCompletedBinding
	
	override fun getResId(): Int {
		return R.layout.fragment_completed
	}
	
	override fun initView(contentView: View) {
		adapter = AsksAdapter(list)
		mBinding.recyclerView.adapter = adapter
	}
	
	override fun initData() {
		lifecycleScope.launch { getAskedList() }
	}
	
	override fun initEvent() {
		mBinding.refreshLayout.setOnRefreshListener {
			pageNo = 1
			lifecycleScope.launch { getAskedList() }
		}
		
		mBinding.refreshLayout.setOnLoadMoreListener {
			pageNo += 1
			lifecycleScope.launch { getAskedList() }
		}
		
		adapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				list[position].answerContent?.let { play(it) }
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
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onDestroy() {
		wlMedia?.stop()
		wlMedia?.release()
		wlMedia = null
		super.onDestroy()
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
					adapter.notifyDataSetChanged()
					
					EventBus.getDefault()
							.post(UpdateNumEvent(UpdateNumEvent.EVENT_TYPE_ASKS, list.size))
					
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