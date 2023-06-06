package fans.openask.ui.activity

import android.content.Intent
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
import fans.openask.databinding.ActivitySenseiProfileBinding
import fans.openask.databinding.DialogAskBinding
import fans.openask.databinding.DialogAskPostedBinding
import fans.openask.databinding.DialogEavesdropBinding
import fans.openask.http.errorMsg
import fans.openask.model.AnswerStateModel
import fans.openask.model.EavesdropModel
import fans.openask.model.SenseiAnswerModel
import fans.openask.model.SenseiListModel
import fans.openask.model.SenseiProifileData
import fans.openask.model.WalletData
import fans.openask.model.event.UpdateNumEvent
import fans.openask.ui.adapter.SenseiAnswerAdapter
import fans.openask.ui.fragment.SenseiAnswerFragment
import fans.openask.ui.fragment.BaseFragment
import fans.openask.ui.fragment.CompletedFragment
import fans.openask.ui.fragment.SenseiAskFragment
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import fans.openask.utils.share.ShareUtil
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
	
	lateinit var asksAdapter: SenseiAnswerAdapter
	var list = mutableListOf<SenseiAnswerModel>()
	
	var wlMedia: WlMedia? = null
	
	lateinit var senseiModel:SenseiListModel
	
	private var mSenseiAnswerFragment: SenseiAnswerFragment? = null
	private var mSenseiAskFragment: SenseiAskFragment? = null
	private var mCurrentFragment: BaseFragment? = null
	
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
	}
	
	override fun initData() {
		senseiModel = intent.getParcelableExtra("model")!!
		
		lifecycleScope.launch {
			getSenseiProfile()
		}
		
		showAnswerFragment()
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvAnswers.setOnClickListener {
			mBinding.tvAnswers.isEnabled = false
			mBinding.tvAsks.isEnabled = true
			showAnswerFragment()
		}
		
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAnswers.isEnabled = true
			mBinding.tvAsks.isEnabled = false
			showAskFragment()
		}
		
		mBinding.ivAsk.setOnClickListener {
			lifecycleScope.launch { getWallet(senseiModel) }
		}
		
		mBinding.ivShare.setOnClickListener {
			val text = "I just found out @${senseiModel.senseiUsername} can voice-reply to your questions on @OpenAskMe! Take a look! https://openask.me/${senseiModel.senseiUsername}"
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
		wlMedia?.stop()
		wlMedia?.release()
		wlMedia = null
		
		super.onDestroy()
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onEvent(event: UpdateNumEvent){
		if (event.eventType == UpdateNumEvent.EVENT_TYPE_ANSWER){
			LogUtils.e(TAG,"EVENT_TYPE_ANSWER")
			mBinding.tvAnswers.text = "Answers(${event.eventValue})"
		}else if (event.eventType == UpdateNumEvent.EVENT_TYPE_ASKS){
			mBinding.tvAsks.text = "Asks(${event.eventValue})"
			LogUtils.e(TAG,"EVENT_TYPE_ASKS")
		}
	}
	
	private fun showAnswerFragment() {
		val transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mSenseiAnswerFragment == null) {
			mSenseiAnswerFragment = SenseiAnswerFragment.getInstance(senseiModel.senseiUid!!)
			transaction.add(R.id.frameLayout, mSenseiAnswerFragment!!)
		} else {
			transaction.show(mSenseiAnswerFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mSenseiAnswerFragment
	}
	
	private fun showAskFragment() {
		val transaction = supportFragmentManager.beginTransaction()
		
		mCurrentFragment?.let { transaction.hide(it) }
		
		if (mSenseiAskFragment == null) {
			mSenseiAskFragment = SenseiAskFragment.getInstance(senseiModel.senseiUid!!)
			transaction.add(R.id.frameLayout, mSenseiAskFragment!!)
		} else {
			transaction.show(mSenseiAskFragment!!)
		}
		
		transaction.commitAllowingStateLoss()
		mCurrentFragment = mSenseiAskFragment
	}
	
	private suspend fun getWallet(data: SenseiListModel) {
		showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet")
				.add("clientType", 7)
				.add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					showAskDialog(data, it,data.minPriceAmount!!)
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showAskDialog(data: SenseiListModel, walletData: WalletData,minPrice:String) {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_ask) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogAskBinding>(v)!!
				
				Glide.with(this@SenseiProfileActivity).load(data.senseiAvatarUrl)
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
										balance = model1.totalBalance!!
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
					
					if (balance < minPrice.toDouble()){
						ToastUtils.show("Balance not enough")
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
		if (wlMedia?.isPlaying == true) {
			wlMedia?.stop()
			wlMedia?.release()
		}
		
		if (wlMedia == null) {
			wlMedia = WlMedia()
			wlMedia?.setPlayModel(WlPlayModel.PLAYMODEL_ONLY_AUDIO)
		}
		
		wlMedia?.source = url
		
		showLoadingDialog("Voice Loading...")
		wlMedia?.setOnMediaInfoListener(object : WlOnMediaInfoListener {
			override fun onPrepared() {
				LogUtils.e(TAG, "onPrepared")
				dismissLoadingDialog()
				wlMedia?.start()
			}
			
			override fun onError(p0: Int, p1: String) {
				LogUtils.e(TAG, "onError $p1")
				showFailedDialog(p1)
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