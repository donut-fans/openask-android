package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.FragmentSenseisBinding
import fans.openask.http.errorMsg
import fans.openask.model.SenseiListModel
import fans.openask.model.WalletData
import fans.openask.ui.adapter.SenseiListAdapter
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.activity.SenseiProfileActivity
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
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
	private var pageSize = 5
	
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
		
		adapter.onItemAskClickListener = object :OnItemClickListener{
			override fun onItemClick(position: Int) {
				lifecycleScope.launch { getWallet(list[position]) }
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
	
	private fun showAskDialog(data:SenseiListModel,walletData: WalletData){
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskBinding>(v)!!
				
				Glide.with(this@SenseisFragment).load(data.senseiAvatarUrl).placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator).circleCrop().into(binding.ivAvator)
				binding.tvName.text = data.senseiName
				binding.tvUserName.text = data.senseiUsername
				binding.tvMinPrice.text = "$"+data.minPriceAmount
				binding.tvBalanceValue.text = "$"+walletData.balance.toString()
				
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.ivBtnAsk.setOnClickListener {
					if (binding.etContent.text.isEmpty()){
						ToastUtils.show("Input your question plz")
						return@setOnClickListener
					}
					
					if (binding.etPrice.text.isEmpty()){
						ToastUtils.show("Input price plz")
						return@setOnClickListener
					}
					
					lifecycleScope.launch {
						dialog.dismiss()
						postAsk(data.senseiUid!!,binding.etContent.text.toString(),1,binding.etPrice.text.toString())
					}
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun postAsk(questioneeUid:String,questionContent:String,payMethodId:Int,payAmount:String){
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/question/submit-question")
			.add("clientType", 7)
			.add("clientId", 7)
			.add("questioneeUid",questioneeUid)
			.add("questionContent",questionContent)
			.add("payMethodId",payMethodId)
			.add("payAmount",payAmount)
			.toAwaitResponse<Any>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				(activity as MainActivity).dismissLoadingDialog()
				showAskPostedDialog()
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				(activity as MainActivity).showFailedDialog(it.errorMsg)
			}
	}
	
	private fun showAskPostedDialog(){
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask_posted) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskPostedBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.ivShare.setOnClickListener {
					ToastUtils.show("Share")
				}
				
				binding.ivView.setOnClickListener {
					ToastUtils.show("View")
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun getWallet(data:SenseiListModel){
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet")
			.add("clientType", 7)
			.add("clientId", 7)
			.toAwaitResponse<WalletData>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				(activity as MainActivity).dismissLoadingDialog()
				showAskDialog(data,it)
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				(activity as MainActivity).showFailedDialog(it.errorMsg)
			}
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
				
				mBinding.refreshLayout.finishRefresh()
				mBinding.refreshLayout.finishLoadMore()
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				(activity as MainActivity).showFailedDialog(it.errorMsg)
			}
	}
}