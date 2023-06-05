package fans.openask.ui.fragment

import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.FragmentSenseisBinding
import fans.openask.http.errorMsg
import fans.openask.model.AskRepData
import fans.openask.model.SenseiListModel
import fans.openask.model.WalletData
import fans.openask.ui.activity.AddFundActivity
import fans.openask.ui.adapter.SenseiListAdapter
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.activity.SenseiProfileActivity
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import fans.openask.utils.share.ShareUtil
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
		
		adapter.onItemClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				SenseiProfileActivity.launch(activity as BaseActivity,
					list[position])
			}
		}
		
		adapter.onItemAskClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				lifecycleScope.launch { getWallet(list[position]) }
			}
		}
		
		adapter.onItemShareClickListener = object :OnItemClickListener{
			override fun onItemClick(position: Int) {
				var text = "I just found out @${list[position].senseiUsername} can reply to your questions via voice @OpenAskMe! Try it out!  #inspiretoask https://openask.me/${list[position].senseiUsername}"
				context?.let { ShareUtil.share(text, it) }
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
	
	private fun showAskDialog(data: SenseiListModel, walletData: WalletData) {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskBinding>(v)!!
				
				Glide.with(this@SenseisFragment).load(data.senseiAvatarUrl)
						.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator)
						.circleCrop().into(binding.ivAvator)
				binding.tvName.text = data.senseiName
				binding.tvUserName.text = data.senseiUsername
				binding.tvMinPrice.text = "$" + data.minPriceAmount
				
				var balance = 0.0
				
				var model: WalletData.AccountCoinModel? = null
				for (i in 0..walletData.accountDtos!!.size) {
					if (walletData.accountDtos!![i].currency == binding.tvPriceSymbol.text.toString()) {
						model = walletData.accountDtos!![i]
						break
					}
				}
				if (model != null) {
					binding.tvBalanceValue.text = "$" + model.totalBalance
					balance = model.totalBalance!!
				}
				
				binding.tvPriceSymbol.setOnClickListener {
					BottomMenu.show(arrayOf<String>("USD", "USDC", "USDT"))
							.setOnMenuItemClickListener { dialog, text, index ->
								run {
									when (index) {
										0 -> {
											binding.tvPriceSymbol.text = "USD"
										}
										
										1 -> binding.tvPriceSymbol.text = "USDC"
										2 -> binding.tvPriceSymbol.text = "USDT"
									}
									
									binding.tvBalanceKey.text = "Your ${binding.tvPriceSymbol.text} balance:"
									
									var model1: WalletData.AccountCoinModel? = null
									for (i in 0..walletData.accountDtos!!.size) {
										if (walletData.accountDtos!![i].currency == binding.tvPriceSymbol.text.toString()) {
											model1 = walletData.accountDtos!![i]
											break
										}
									}
									if (model1 != null) {
										binding.tvBalanceValue.text = model1.totalBalance.toString()
										balance - model1.totalBalance!!
									}
								}
								false
							}
					
				}
				
				binding.tvAddFund.setOnClickListener {
					AddFundActivity.launch(activity as BaseActivity)
				}
				
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.ivBtnAsk.setOnClickListener {
					if (binding.etContent.text.isEmpty()) {
						ToastUtils.show("Input your question plz")
						return@setOnClickListener
					}
					
					if (binding.etPrice.text.isEmpty()) {
						ToastUtils.show("Input price plz")
						return@setOnClickListener
					}
					
					if (balance < binding.etPrice.text.toString().toDouble()){
						ToastUtils.show("Balance not enough")
						return@setOnClickListener
					}
					
					lifecycleScope.launch {
						dialog.dismiss()
						postAsk(data.senseiUid!!,
							binding.etContent.text.toString(),
							1,
							binding.etPrice.text.toString(),data)
					}
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun postAsk(questioneeUid: String,
	                            questionContent: String,
	                            payMethodId: Int,
	                            payAmount: String,data: SenseiListModel) {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/question/submit-question")
				.add("clientType", 7)
				.add("clientId", 7)
				.add("questioneeUid", questioneeUid)
				.add("questionContent", questionContent)
				.add("payMethodId", payMethodId)
				.add("payAmount", payAmount)
				.toAwaitResponse<AskRepData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					it.quesionId?.let { it1 -> showAskPostedDialog(data, it1) }
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showAskPostedDialog(data: SenseiListModel,questionId:String) {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask_posted) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskPostedBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.ivShare.setOnClickListener {
					var text = "Hey @${data.senseiUsername}, I just asked a burning question to you @OpenAskMe! Can't wait to hear your perspective on this. Your insights will be truly invaluable to me. #inspiretoask https://openask.me/question/:$questionId"
					context?.let { it1 -> ShareUtil.share(text, it1) }
				}
				
				binding.ivView.setOnClickListener {
					ToastUtils.show("View")
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun getWallet(data: SenseiListModel) {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet")
				.add("clientType", 7)
				.add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					showAskDialog(data, it)
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
					
					if (pageNo == 1) {
						list.clear()
					}
					
					this.list.addAll(it)
					adapter.notifyDataSetChanged()
					
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
}