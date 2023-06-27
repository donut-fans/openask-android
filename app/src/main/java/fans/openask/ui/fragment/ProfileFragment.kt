package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.MediaMetadataRetriever
import android.media.MediaRecorder
import android.os.Build
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.bumptech.glide.Glide
import com.fans.donut.data.file.OSSTokenData
import com.fans.donut.utils.oss.FileUploader
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.tencent.mmkv.MMKV
import com.ywl5320.wlmedia.WlMedia
import com.ywl5320.wlmedia.enums.WlComplete
import com.ywl5320.wlmedia.enums.WlPlayModel
import com.ywl5320.wlmedia.listener.WlOnMediaInfoListener
import fans.openask.R
import fans.openask.databinding.DialogBecomeSenseiBinding
import fans.openask.databinding.DialogBecomeSenseiStep2Binding
import fans.openask.databinding.DialogBecomeSenseiStep4Binding
import fans.openask.databinding.DialogBecomeSenseiStepEmailInputBinding
import fans.openask.databinding.FragmentProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.SenseiProfileSettingRepData
import fans.openask.model.UserInfo
import fans.openask.model.WalletData
import fans.openask.model.twitter.TwitterExtInfoModel
import fans.openask.ui.activity.AddFundActivity
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.utils.LogUtils
import fans.openask.utils.TimeUtils
import fans.openask.utils.ToastUtils
import fans.openask.utils.isEmail
import kotlinx.coroutines.launch
import me.linjw.demo.lame.Encoder
import me.linjw.demo.lame.Recorder
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.io.File

/**
 *
 * Created by Irving
 */
class ProfileFragment : BaseFragment() {
	private val TAG = "ProfileFragment"
	
	lateinit var mBinding: FragmentProfileBinding
	
	private val REQUEST_RECORD_AUDIO_PERMISSION = 200
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	private var outputFilePath: String? = null
	private var timeDuration: Int = 0
	
	var wlMedia: WlMedia? = null
	
	private val recorder = Recorder(object : Recorder.IRecordListener {
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
	
	override fun getResId(): Int {
		return R.layout.fragment_profile
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		if (userInfo != null) {
			setProfileInfo(userInfo)
			lifecycleScope.launch { setStepStatus() }
		}
		
		firebaseAuth = FirebaseAuth.getInstance()
		
		lifecycleScope.launch {
			getWallet()
		}
	}
	
	override fun initEvent() {
		mBinding.ivAddFund.setOnClickListener {
			AddFundActivity.launch(activity as BaseActivity)
		}
		
		mBinding.ivBtnBecome.setOnClickListener {
			showBecomeDialog()
		}
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		wlMedia?.stop()
		wlMedia?.release()
		wlMedia = null
		super.onDestroy()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden){
			setStatusBarColor("#FFFFFF", true)
			lifecycleScope.launch {
				getWallet()
			}
		}
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
		lifecycleScope.launch {
			getWallet()
		}
	}
	
	private fun showBecomeDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					lifecycleScope.launch { getStep() }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	/**
	 * 1.获取用户成为师傅步骤
	 */
	private suspend fun getStep() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/user/sensei/step")
				.toAwaitResponse<Int>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					//1:绑定twitter；2填写min price 3：intro
					when (it) {
						1 -> {//twitter
							getTwitterInfo()
						}
						
						2 -> {//minPrice
							showSetMinPriceDialog()
						}
						
						3 -> {//intro
							showSetIntroDialog()
						}
						
						5 -> {//email
							showInputEmailDialog()
						}
					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	/**
	 * 1.获取用户成为师傅步骤
	 */
	private suspend fun setStepStatus() {
		RxHttp.get("/open-ask/user/sensei/step")
				.toAwaitResponse<Int>()
				.awaitResult {
					//1:绑定twitter；2填写min price 3：intro
					when (it) {
						1 -> {//twitter
							mBinding.ivBtnBecome.visibility = View.VISIBLE
						}
						
						2 -> {//minPrice
							mBinding.ivBtnBecome.visibility = View.VISIBLE
						}
						
						5 -> {//email
							mBinding.ivBtnBecome.visibility = View.VISIBLE
						}
						
						else -> {
							mBinding.ivBtnBecome.visibility = View.GONE
						}
						
					}
					
				}.onFailure {
				}
	}
	
	
	private fun getTwitterInfo() {
		val provider =
			OAuthProvider.newBuilder("twitter.com") //		provider.addCustomParameter("lang",'')
		val pendingResultTask = firebaseAuth.pendingAuthResult
		if (pendingResultTask != null) {            // There's something already here! Finish the sign-in for your user.
			pendingResultTask.addOnSuccessListener {                    // User is signed in.
				LogUtils.e(TAG, "pendingResultTask addOnSuccessListener" + Gson().toJson(it))
			}.addOnFailureListener {                    // Handle failure.
				LogUtils.e(TAG, "pendingResultTask addOnFailureListener " + Gson().toJson(it))
			}
		} else {
		
		}
		
		firebaseAuth.startActivityForSignInWithProvider(requireActivity(), provider.build())
				.addOnSuccessListener {
					LogUtils.e(TAG, "firebaseAuth addOnSuccessListener " + Gson().toJson(it))
					
					var msg = Gson().toJson(it)
					if (msg.length > 4000) {
						for (i in 0..msg.length / 4000) {
							if ((i * 4000 + 4000) > msg.length) {
								LogUtils.e(TAG, msg.substring(i * 4000, msg.length))
							} else {
								LogUtils.e(TAG, msg.substring(i * 4000, i * 4000 + 4000))
							}
							
						}
					}
					
					
					lifecycleScope.launch { bindTwitter(it) }
				}.addOnFailureListener {                // Handle failure.
					LogUtils.e(TAG, "firebaseAuth addOnFailureListener " + Gson().toJson(it))
				}
	}
	
	private fun showInputEmailDialog() {
		CustomDialog.show(object :
			OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_email_input) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStepEmailInputBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
					val email = binding.etEmail.text.toString()
					if (isEmail(email)) {
//						showEmailVerification()
						lifecycleScope.launch {
							setEmail(email,dialog)
						}
					} else {
						ToastUtils.show("Please enter the correct email address")
					}
				}
			}
		}).maskColor = resources.getColor(R.color.black_50)
	}
	
	private fun showEmailVerification() {
		CustomDialog.show(object :
			OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_email_input) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStepEmailInputBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					
				}
			}
		}).maskColor = resources.getColor(R.color.black_50)
	}
	
	private suspend fun setEmail(email: String,dialog: CustomDialog) {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		
		var extInfo = SenseiProfileSettingRepData()
		extInfo.email = email
		
		RxHttp.postJson("open-ask/user/sensei/add-profile")
				.add("type", 1)//1、设置最小金额  2、设置自我介绍语音
				.add("extInfo", Gson().toJson(extInfo))
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					dialog.dismiss()
					showSetIntroDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	
	private suspend fun bindTwitter(result: AuthResult) {
		
		var extInfo = TwitterExtInfoModel(result.user?.uid,
			result.user?.providerId,
			result.user?.photoUrl.toString(),
			result.user?.displayName,
			result.user?.displayName,
			result.additionalUserInfo?.profile?.get("description").toString(),
			result.additionalUserInfo?.profile?.get("followers_count").toString().toInt())
		
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/user/tripartite-account/bind-user")
				.add("openId", 1454368045896540200)
				.add("type", 1)
				.add("extInfo", Gson().toJson(extInfo))
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					showSetMinPriceDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showSetMinPriceDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_2) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStep2Binding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvPrice1.setOnClickListener {
					binding.etPrice.setText("9")
				}
				
				binding.tvPrice2.setOnClickListener {
					binding.etPrice.setText("29")
				}
				
				binding.tvPrice3.setOnClickListener {
					binding.etPrice.setText("49")
				}
				
				binding.tvBtn.setOnClickListener {
					if (binding.etPrice.text.isNullOrEmpty()) {
						ToastUtils.show("Input your price please")
						return@setOnClickListener
					}
					dialog.dismiss()
					lifecycleScope.launch { setMinPrice(binding.etPrice.text.toString()) }
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun setMinPrice(price: String) {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		
		var extInfo = SenseiProfileSettingRepData()
		extInfo.minPrice = price
		
		RxHttp.postJson("/open-ask/user/sensei/update-profile")
				.add("type", 1)//1、设置最小金额  2、设置自我介绍语音
				.add("extInfo", Gson().toJson(extInfo))
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					showInputEmailDialog()
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showSetIntroDialog() {
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_become_sensei_step_4) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogBecomeSenseiStep4Binding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvSkip.setOnClickListener {
					dialog.dismiss()
				}
				
				binding.tvBtnSubmit.setOnClickListener {
					if (!outputFilePath.isNullOrEmpty()) {
						lifecycleScope.launch {
							val array = outputFilePath!!.split("/")
							getToken(array[array.size - 1], outputFilePath!!, dialog)
						}
					} else {
						ToastUtils.show("Please record your introduce")
					}
				}
				
				binding.ivPlay.setOnClickListener {
					outputFilePath?.let { it1 -> play(it1) }
				}
				
				binding.tvBtnRecord.setOnTouchListener(object : View.OnTouchListener {
					override fun onTouch(p0: View?, p1: MotionEvent): Boolean {
						if (ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
								Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED ||
							ActivityCompat.checkSelfPermission(activity as AppCompatActivity,
								Manifest.permission.MANAGE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
							if (p1.action == MotionEvent.ACTION_DOWN) {
								startRecording()
								binding.tvBtnRecord.setBackgroundResource(R.drawable.bg_btn_black)
								binding.tvBtnRecord.text = "Release to stop"
							} else if (p1.action == MotionEvent.ACTION_UP) {
								binding.tvTime.visibility = View.VISIBLE
								binding.ivPlay.visibility = View.VISIBLE
								
								var mmr = MediaMetadataRetriever()
								try {
									mmr.setDataSource(outputFilePath)
									var time =
										mmr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
									
									timeDuration = time?.toInt()?.div(1000)!!
									
									binding.tvTime.text = TimeUtils.timeConversion2(timeDuration)
									
								} catch (e: Exception) {
									e.printStackTrace()
								}
								
								stopRecording()
								binding.tvBtnRecord.setBackgroundResource(R.drawable.icon_btn_bg2)
								binding.tvBtnRecord.text = "Press to start"
							}
						} else {
							ActivityCompat.requestPermissions(activity as Activity,
								arrayOf(Manifest.permission.RECORD_AUDIO,
									Manifest.permission.MANAGE_EXTERNAL_STORAGE),
								REQUEST_RECORD_AUDIO_PERMISSION)
						}
						return true
					}
				})
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
	private suspend fun getToken(fileName: String,
	                             filePath: String,
	                             dialog: CustomDialog) {
		(activity as MainActivity).showLoadingDialog("Loading...")
		RxHttp.get("/oss/get-token")
				.add("fileName", fileName)
				.add("temp", 1)
				.toAwaitResponse<OSSTokenData>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as MainActivity).dismissLoadingDialog()
					
					uploadFile(it, filePath, dialog)
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as MainActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun uploadFile(data: OSSTokenData,
	                       filePath: String,
	                       dialog: CustomDialog) {
		(activity as MainActivity).showLoadingDialog("Loading...")
		context?.let {
			FileUploader().uploadFile(it, data, filePath, object : FileUploader.UploadListener {
				override fun onStart() {
					(activity as MainActivity).showLoadingDialog("Uploading...")
				}
				
				override fun onProgress(progress: Int) {
					(activity as MainActivity).showLoadingDialog("Uploading($progress/100)")
				}
				
				override fun onSuccess(request: PutObjectRequest?) {
					(activity as MainActivity).dismissLoadingDialog()
					
					lifecycleScope.launch {
						setIntro(data.fileName!!, timeDuration, dialog)
					}
				}
				
				override fun onFailure(msg: String) {
					(activity as MainActivity).showFailedDialog(msg)
				}
				
			})
		}
	}
	
	private suspend fun setIntro(url: String, duration: Int, dialog: CustomDialog) {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		
		var extInfo = SenseiProfileSettingRepData()
		extInfo.audioUrl = url
		extInfo.audioDuration = duration.toString()
		
		RxHttp.postJson("/open-ask/user/sensei/update-profile")
				.add("openId", 1613901158325551000)
				.add("type", 2)//1、设置最小金额  2、设置自我介绍语音
				.add("extInfo", Gson().toJson(extInfo))
				.toAwaitResponse<Boolean>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					dialog.dismiss()
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
	
	
	private fun setProfileInfo(userInfo: UserInfo) {
		Glide.with(this).load(userInfo.headIcon).placeholder(R.drawable.icon_avator)
				.error(R.drawable.icon_avator).circleCrop().into(mBinding.ivAvator)
		
		mBinding.tvUsername.text = userInfo.nickname
		
		if (userInfo.isSensei == true) {
			mBinding.ivBtnBecome.visibility = View.GONE
		} else {
			mBinding.ivBtnBecome.visibility = View.VISIBLE
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
	
	private fun startRecording() {
		LogUtils.e(TAG, "startRecording")
		//设置输出文件路径
		outputFilePath =
			context?.getExternalFilesDir(Environment.DIRECTORY_MUSIC)?.absolutePath + "/" + System.currentTimeMillis() + "_android_audio.mp3"
		
		encoder.start(File(outputFilePath),
			SAMPLE_RATE,
			CHANNEL_COUNT)
		recorder.start(AUDIO_SOURCE,
			SAMPLE_RATE,
			CHANNEL_CONFIG,
			AUDIO_FORMAT)
	}
	
	private fun stopRecording() {
		LogUtils.e(TAG, "stopRecording")
		
		recorder.stop()
		encoder.stop()
	}
	
}