package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.fans.donut.listener.OnItemClickListener
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshListener
import fans.openask.R
import fans.openask.databinding.FragmentSenseisBinding
import fans.openask.http.errorMsg
import fans.openask.model.SenseiListModel
import fans.openask.model.UserInfo
import fans.openask.ui.SenseiListAdapter
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.activity.SenseiProfileActivity
import fans.openask.utils.LogUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse

/**
 *
 * Created by Irving
 */
class SenseisFragment : BaseFragment() {
	private val TAG = "SenseisFragment"
	
	private var pageNo = 1
	private var pageSize = 10
	
	var list = mutableListOf<SenseiListModel>()
	lateinit var adapter: SenseiListAdapter
	
	private lateinit var mBinding: FragmentSenseisBinding
	
	override fun getResId(): Int {
		return R.layout.fragment_senseis
	}
	
	override fun initView(contentView: View) {
		adapter = SenseiListAdapter(list)
		mBinding.recyclerView.adapter = adapter
	}
	
	override fun initData() {
		lifecycleScope.launch { getSenseiList() }
	}
	
	override fun initEvent() {
		mBinding.refreshLayout.setOnRefreshListener {
			pageNo = 1
			lifecycleScope.launch { getSenseiList() }
		}
		
		mBinding.refreshLayout.setOnLoadMoreListener {
			pageNo += 1
			lifecycleScope.launch { getSenseiList() }
		}
		
		adapter.onItemClickListener = object :OnItemClickListener{
			override fun onItemClick(position: Int) {
				SenseiProfileActivity.launch(activity as BaseActivity,list[position].userNo!!,list[position].senseiUsername!!)
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
	
	private suspend fun getSenseiList() {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/sensei-list").add("clientType", 7).add("clientId", 7)
			.add("pageNo", pageNo).add("pageSize", pageSize)
			.toAwaitResponse<List<SenseiListModel>>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				(activity as MainActivity).dismissLoadingDialog()
				
				if (pageNo == 1){
					list.clear()
				}
				
				this.list.addAll(it)
				adapter.notifyDataSetChanged()
				
				if (it.size < pageSize){
					mBinding.refreshLayout.setEnableLoadMore(false)
				}else{
					mBinding.refreshLayout.setEnableLoadMore(true)
				}
				
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				(activity as MainActivity).showFailedDialog(it.errorMsg)
			}
	}
	
}