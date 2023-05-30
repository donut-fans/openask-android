package fans.openask.ui.activity

import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaPlayer.OnBufferingUpdateListener
import android.media.MediaPlayer.OnErrorListener
import android.view.View
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.ActivitySenseiProfileBinding
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.DialogEavesdropBinding
import fans.openask.http.errorMsg
import fans.openask.model.AnswerStateModel
import fans.openask.model.AsksModel
import fans.openask.model.EavesdropModel
import fans.openask.model.SenseiAnswerModel
import fans.openask.model.SenseiListModel
import fans.openask.model.SenseiProifileData
import fans.openask.model.WalletData
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.ui.adapter.SenseiAnswerAdapter
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import fans.openask.utils.share.ShareUtil
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
	
	private var pageSize = 3
	private var pageNo = 1
	
	lateinit var asksAdapter: SenseiAnswerAdapter
	var list = mutableListOf<SenseiAnswerModel>()
	
	var mediaPlayer: MediaPlayer? = null
	
	lateinit var senseiModel:SenseiListModel
	
	companion object {
		fun launch(activity: BaseActivity, model:SenseiListModel) {
			var intent = Intent(activity, SenseiProfileActivity::class.java)
			intent.putExtra("model", model)
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
		senseiModel = intent.getParcelableExtra("model")!!
		
		lifecycleScope.launch {
			getSenseiProfile()
			senseiModel.senseiUid?.let { getAnswerList(it) }
		}
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvAnswers.setOnClickListener {
			mBinding.tvAnswers.isEnabled = false
			mBinding.tvAsks.isEnabled = true
			
			pageNo = 1
			lifecycleScope.launch { senseiModel.senseiUid?.let { it1 -> getAnswerList(it1) } }
		}
		
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAnswers.isEnabled = true
			mBinding.tvAsks.isEnabled = false
			
			pageNo = 1
			lifecycleScope.launch { senseiModel.senseiUid?.let { it1 -> getAsksList(it1) } }
		}
		
		mBinding.ivAsk.setOnClickListener {
			lifecycleScope.launch { getWallet(senseiModel) }
		}
		
		mBinding.ivShare.setOnClickListener {
			var text = "I just found out @${senseiModel.senseiUsername} can reply to your questions via voice @OpenAskMe! Try it out!  #inspiretoask https://openask.me/${senseiModel.senseiName}"
			ShareUtil.share(text, this)
		}
		
		asksAdapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				if (list[position].answerState != null) {
					if (list[position].answerState!!.answerContent.isNullOrEmpty()) {//未付费
						//付费
						list[position].answerState?.answerId?.let { showEavesdropDialog(it,list[position].questionId!!) }
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
	
	private suspend fun getWallet(data: SenseiListModel) {
		showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet")
				.add("clientType", 7)
				.add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					showAskDialog(data, it)
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showAskDialog(data: SenseiListModel, walletData: WalletData) {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskBinding>(v)!!
				
				Glide.with(this@SenseiProfileActivity).load(data.senseiAvatarUrl)
						.placeholder(R.drawable.icon_avator).error(R.drawable.icon_avator)
						.circleCrop().into(binding.ivAvator)
				binding.tvName.text = data.senseiName
				binding.tvUserName.text = data.senseiUsername
				binding.tvMinPrice.text = "$" + data.minPriceAmount
				
				var model: WalletData.AccountCoinModel? = null
				for (i in 0..walletData.accountDtos!!.size) {
					if (walletData.accountDtos!![i].currency == binding.tvPriceSymbol.text.toString()) {
						model = walletData.accountDtos!![i]
						break
					}
				}
				if (model != null)
					binding.tvBalanceValue.text = "$" +model.totalBalance
				
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
									}
								}
								false
							}
					
				}
				
				binding.tvAddFund.setOnClickListener {
					AddFundActivity.launch(this@SenseiProfileActivity)
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
					
					lifecycleScope.launch {
						dialog.dismiss()
						postAsk(data.senseiUid!!,
							binding.etContent.text.toString(),
							1,
							binding.etPrice.text.toString())
					}
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun postAsk(questioneeUid: String,
	                            questionContent: String,
	                            payMethodId: Int,
	                            payAmount: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/question/submit-question")
				.add("clientType", 7)
				.add("clientId", 7)
				.add("questioneeUid", questioneeUid)
				.add("questionContent", questionContent)
				.add("payMethodId", payMethodId)
				.add("payAmount", payAmount)
				.toAwaitResponse<Any>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					showAskPostedDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showAskPostedDialog() {
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
	
	private fun play(url: String) {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		
		showLoadingDialog("Voice Loading...")
		mediaPlayer?.setOnPreparedListener {
			dismissLoadingDialog()
			mediaPlayer?.start()
		}
		
		mediaPlayer?.setOnBufferingUpdateListener { p0, p1 ->
			LogUtils.e(TAG,
				"onBufferingUpdate $p1")
		}
		
		mediaPlayer?.setOnErrorListener { p0, p1, p2 ->
			showFailedDialog("voice load error")
			true
		}
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepareAsync()
		} catch (e: Exception) {
			e.printStackTrace()
		}
	}
	
	private suspend fun getSenseiProfile() {
		showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/user/user-page/${senseiModel.userNo}/${senseiModel.senseiUsername}").add("clientType", 7)
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
		mBinding.tvUsername.text = "@"+data.username
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
		RxHttp.postJson("/open-ask/feed/user-page/answers").add("userId", userId)
				.add("clientId", 7)
				.add("pageSize", pageSize)
				.add("pageNo", pageNo)
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
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/question/eavesdropped")
				.add("answerId", answerId)
				.add("payMethodId", 8)
				.toAwaitResponse<EavesdropModel>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					var list = mutableListOf<String>()
					list.add(questionId)
					getAnswerState(list)
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
}