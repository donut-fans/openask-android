package fans.openask.ui.fragment

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.os.Environment
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.fans.donut.listener.OnItemClickListener
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.gson.Gson
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.tencent.mmkv.MMKV
import fans.openask.R
import fans.openask.databinding.DialogBecomeSenseiBinding
import fans.openask.databinding.FragmentProfileBinding
import fans.openask.http.errorMsg
import fans.openask.model.AsksModel
import fans.openask.model.UserInfo
import fans.openask.model.WalletData
import fans.openask.model.twitter.TwitterExtInfoModel
import fans.openask.ui.activity.BaseActivity
import fans.openask.ui.activity.MainActivity
import fans.openask.ui.adapter.AsksAdapter
import fans.openask.utils.LogUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse
import java.io.IOException


/**
 *
 * Created by Irving
 */
class ProfileFragment : BaseFragment() {
	private val TAG = "ProfileFragment"
	
	lateinit var mBinding: FragmentProfileBinding
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	private var pageSize = 20
	private var pageNo = 1
	
	lateinit var asksAdapter: AsksAdapter
	var list = mutableListOf<AsksModel>()
	
	var mediaPlayer: MediaPlayer? = null
	
	override fun getResId(): Int {
		return R.layout.fragment_profile
	}
	
	override fun initView(contentView: View) {
	}
	
	override fun initData() {
		var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
		if (userInfo != null) {
			setProfileInfo(userInfo)
		}
		
		firebaseAuth = FirebaseAuth.getInstance()
		
		asksAdapter = AsksAdapter(list)
		mBinding.recyclerView.adapter = asksAdapter
		
		lifecycleScope.launch {
			getWallet()
			getAskedList()
		}
	}
	
	override fun initEvent() {
		mBinding.tvAsks.setOnClickListener {
			mBinding.tvAsks.isEnabled = false
			mBinding.tvEavesdrop.isEnabled = true
			
			pageNo = 1
			lifecycleScope.launch { getAskedList() }
		}
		
		mBinding.tvEavesdrop.setOnClickListener {
			mBinding.tvAsks.isEnabled = true
			mBinding.tvEavesdrop.isEnabled = false
			
			pageNo = 1
			lifecycleScope.launch { getEavesdroppedList() }
		}
		
		mBinding.refreshLayout.setOnRefreshListener {
			pageNo = 1
			if (!mBinding.tvAsks.isEnabled) {
				lifecycleScope.launch { getAskedList() }
			} else {
				lifecycleScope.launch { getEavesdroppedList() }
			}
		}
		
		mBinding.refreshLayout.setOnLoadMoreListener {
			pageNo += 1
			if (!mBinding.tvAsks.isEnabled) {
				lifecycleScope.launch { getAskedList() }
			} else {
				lifecycleScope.launch { getEavesdroppedList() }
			}
		}
		
		asksAdapter.onItemPlayClickListener = object : OnItemClickListener {
			override fun onItemClick(position: Int) {
				list[position].answerContent?.let { play(it) }
			}
		}
		
		mBinding.ivBtnBecome.setOnClickListener {
			showBecomeDialog()
		}
		
	}
	
	override fun setDataBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	override fun onDestroy() {
		mediaPlayer?.release()
		mediaPlayer = null
		super.onDestroy()
	}
	
	override fun onHiddenChanged(hidden: Boolean) {
		super.onHiddenChanged(hidden)
		if (!hidden) setStatusBarColor("#FFFFFF", true)
	}
	
	override fun onResume() {
		super.onResume()
		setStatusBarColor("#FFFFFF", true)
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
						1 -> {
							getTwitterInfo()
						}
						
						2 -> {
							showSetMinPriceDialog()
						}
						
						3 -> {
							showSetIntroDialog()
						}
					}
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
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
				LogUtils.e(TAG, "pendingResultTask addOnFailureListener "+ Gson().toJson(it))
			}
		} else {
		
		}
		
		firebaseAuth.startActivityForSignInWithProvider(requireActivity(), provider.build())
				.addOnSuccessListener {
					LogUtils.e(TAG, "firebaseAuth addOnSuccessListener " + Gson().toJson(it))
					
					var msg = Gson().toJson(it)
					if (msg.length > 4000){
						for (i in 0..msg.length/4000){
							if ((i * 4000 + 4000) > msg.length){
								LogUtils.e(TAG,msg.substring(i * 4000,msg.length))
							}else{
								LogUtils.e(TAG,msg.substring(i * 4000,i*4000 + 4000))
							}
							
						}
					}

					
					lifecycleScope.launch { bindTwitter(it) }
				}.addOnFailureListener {                // Handle failure.
					LogUtils.e(TAG, "firebaseAuth addOnFailureListener " + Gson().toJson(it))
				}
	}
	
	private suspend fun bindTwitter(result:AuthResult) {
		
		var extInfo = TwitterExtInfoModel(result.user?.uid
			,result.user?.providerId
			,result.user?.photoUrl.toString()
			,result.user?.displayName
		,result.user?.displayName
		,null
		,"100000")
		
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/user/tripartite-account/bind-user")
				.add("openId", 1613901158325551000)
				.add("type", 1)
				.add("extInfo", extInfo)
				.toAwaitResponse<List<Any>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
				}
	}
	
	private fun showSetMinPriceDialog() {
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
	
	private fun showSetIntroDialog() {
	
	}
	
	private fun play(url: String) {
		if (mediaPlayer == null) {
			mediaPlayer = MediaPlayer()
			mediaPlayer?.setAudioStreamType(AudioManager.STREAM_MUSIC)
		}
		mediaPlayer?.stop()
		
		try {
			mediaPlayer?.setDataSource(url)
			mediaPlayer?.prepare()
		} catch (e: Exception) {
			e.printStackTrace()
		}
		
		mediaPlayer?.start()
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
					asksAdapter.notifyDataSetChanged()
					mBinding.tvAsks.text = "Your Asks(${list.size})"
					
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
	
	private suspend fun getEavesdroppedList() {
		(activity as BaseActivity).showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/feed/my-eavesdropped").add("clientType", 7).add("clientId", 7)
				.add("pageSize", pageSize).add("pageNo", pageNo).toAwaitResponse<List<Any>>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					(activity as BaseActivity).dismissLoadingDialog()
					
					
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					(activity as BaseActivity).showFailedDialog(it.errorMsg)
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
	
}