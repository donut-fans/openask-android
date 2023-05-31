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
import fans.openask.R
import fans.openask.databinding.DialogAnswerBinding
import fans.openask.databinding.FragmentAwaitingBinding
import fans.openask.http.errorMsg
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
class SenseiAskFragment : BaseFragment() {
	private val TAG = "AwaitingFragment"
	
	private var pageNo = 1
	private var pageSize = 10
	
	var list = mutableListOf<SenseiAnswerModel>()
	lateinit var adapter: SenseiAnswerAdapter
	
	private lateinit var mBinding: FragmentAwaitingBinding
	
	companion object {
		fun getInstance(userId:String):SenseiAskFragment{
			val fragment = SenseiAskFragment()
			
			var bundle = Bundle()
			bundle.putString("userId",userId)
			fragment.arguments = bundle
			
			return fragment
		}
		
		init {
			System.loadLibrary("lame")
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
	
	private suspend fun getList() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/user-page/questions")
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
							.post(UpdateNumEvent(UpdateNumEvent.EVENT_TYPE_ASKS, list.size))
					
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
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
}