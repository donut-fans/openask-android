package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.OAuthProvider
import com.google.gson.Gson
import com.tencent.mmkv.MMKV
import com.tokenpocket.opensdk.base.TPListener
import com.tokenpocket.opensdk.base.TPManager
import com.tokenpocket.opensdk.simple.model.Authorize
import com.tokenpocket.opensdk.simple.model.Blockchain
import com.tokenpocket.opensdk.simple.model.Signature
import fans.openask.OpenAskApplication
import fans.openask.R
import fans.openask.databinding.ActivityLoginBinding
import fans.openask.http.errorMsg
import fans.openask.model.NonceData
import fans.openask.model.TPWalletLoginData
import fans.openask.model.TPWalletSignData
import fans.openask.model.UserInfo
import fans.openask.utils.LogUtils
import kotlinx.coroutines.launch
import rxhttp.awaitResult
import rxhttp.wrapper.param.RxHttp
import rxhttp.wrapper.param.toAwaitResponse


/**
 *
 * Created by Irving
 */
class LoginActivity : BaseActivity() {
	var TAG = "LoginActivity"
	
	private lateinit var firebaseAuth: FirebaseAuth
	
	lateinit var mBinding: ActivityLoginBinding
	
	companion object {
		fun launch(activity: BaseActivity) {
			activity.startActivity(Intent(activity, LoginActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_login
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF", true)
	}
	
	override fun initData() {
		firebaseAuth = FirebaseAuth.getInstance()
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.ivBtnSigninWallet.setOnClickListener {
			getWalletAddress()
		}
		
		mBinding.ivBtnSigninTwitter.setOnClickListener {
			twitterLogin()
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private fun twitterLogin() {
		val provider =
			OAuthProvider.newBuilder("twitter.com") //		provider.addCustomParameter("lang",'')
		val pendingResultTask = firebaseAuth.pendingAuthResult
		if (pendingResultTask != null) {			// There's something already here! Finish the sign-in for your user.
			pendingResultTask.addOnSuccessListener {					// User is signed in.
					// IdP data available in
					// authResult.getAdditionalUserInfo().getProfile().
					// The OAuth access token can also be retrieved:
					// ((OAuthCredential)authResult.getCredential()).getAccessToken().
					// The OAuth secret can be retrieved by calling:
					// ((OAuthCredential)authResult.getCredential()).getSecret().
					
					LogUtils.e(TAG, "pendingResultTask addOnSuccessListener" + it.toString())
				}.addOnFailureListener {					// Handle failure.
					LogUtils.e(TAG, "pendingResultTask addOnFailureListener")
				}
		} else {			// There's no pending result so you need to start the sign-in flow.
			// See below.
		}
		
		firebaseAuth.startActivityForSignInWithProvider( /* activity = */this, provider.build())
			.addOnSuccessListener {				// User is signed in.
				// IdP data available in
				// authResult.getAdditionalUserInfo().getProfile().
				// The OAuth access token can also be retrieved:
				// ((OAuthCredential)authResult.getCredential()).getAccessToken().
				// The OAuth secret can be retrieved by calling:
				// ((OAuthCredential)authResult.getCredential()).getSecret().
				LogUtils.e(TAG, "firebaseAuth addOnSuccessListener" + it.toString())
			}.addOnFailureListener {				// Handle failure.
				LogUtils.e(TAG, "firebaseAuth addOnFailureListener " + it.toString())
			}
	}
	
	private suspend fun getNonce(walletAddress: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/user/wallet-login/get-sign-nonce").add("clientType", 7)
			.add("walletAddress", walletAddress).toAwaitResponse<NonceData>().awaitResult {
				LogUtils.e(TAG, "getNonce awaitResult = " + Gson().toJson(it))
				dismissLoadingDialog()
				startSign(walletAddress, it.nonce!!)
			}.onFailure {
				LogUtils.e(TAG, "getNonce onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
	private fun startSign(walletAddress: String, nonce: String) {
		
		val signature = Signature() //标识链		//标识链
		val blockchains: MutableList<Blockchain> = ArrayList()
		blockchains.add(Blockchain("ethereum", "1"))
		signature.setBlockchains(blockchains)
		
		signature.setDappName(resources.getString(R.string.app_name))
		signature.setDappIcon("https://eosknights.io/img/icon.png") //开发者自己定义的业务ID，用于标识操作，在授权登录中，需要设置该字段
		//开发者自己定义的业务ID，用于标识操作，在授权登录中，需要设置该字段
		signature.setActionId("web-db4c5466-1a03-438c-90c9-2172e8becea5") //签名类型 从Android 1.6.8版本开始，EVM网络支持 ethPersonalSign ethSignTypedDataLegacy ethSignTypedData
		//ethSignTypedData_v4 四种签名类型
		//签名类型 从Android 1.6.8版本开始，EVM网络支持 ethPersonalSign ethSignTypedDataLegacy ethSignTypedData
		//ethSignTypedData_v4 四种签名类型
		signature.setSignType("ethPersonalSign") //ethSign类型，签名的数据是16进制字符串
		//ethSign类型，签名的数据是16进制字符串
		signature.setMessage(nonce) //开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		//开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		//		signature.setCallbackUrl("http://115.205.0.178:9011/taaBizApi/taaInitData")
		TPManager.getInstance().signature(this, signature, object : TPListener {
			override fun onSuccess(s: String) {
				LogUtils.e(TAG, "startSign onSuccess " + s)
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
				
				var data = Gson().fromJson(s, TPWalletSignData::class.java)
				
				lifecycleScope.launch {
					login(walletAddress, nonce, data.sign!!)
				}
			}
			
			override fun onError(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onCancel(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
		})
	}
	
	private suspend fun login(walletAddress: String, nonce: String, signature: String) {
		showLoadingDialog("Loading...")
		RxHttp.postJson("/user/wallet-login/verify-sign").add("clientType", 7)
			.add("walletAddress", walletAddress).add("nonce", nonce).add("signature", signature)
			.toAwaitResponse<UserInfo>().awaitResult {
				LogUtils.e(TAG, "awaitResult = " + it.toString())
				dismissLoadingDialog()
				
				MMKV.defaultMMKV().encode("userInfo", it)
				it.token?.let { it1 ->
					{
						OpenAskApplication.instance.initRxHttp(it1)
						OpenAskApplication.instance.userInfo = it
					}
				}
				
				MainActivity.launch(this)
				finish()
			}.onFailure {
				LogUtils.e(TAG, "onFailure = " + it.message.toString())
				showFailedDialog(it.errorMsg)
			}
	}
	
	private fun getWalletAddress() {
		val authorize = Authorize()
		val blockchains: MutableList<Blockchain> = ArrayList()
		blockchains.add(Blockchain("ethereum", "1"))
		authorize.blockchains = blockchains
		
		authorize.dappName = resources.getString(R.string.app_name)
		authorize.dappIcon = "https://eosknights.io/img/icon.png" //开发者自己定义的业务id
		//开发者自己定义的业务id
		authorize.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		TPManager.getInstance().authorize(this, authorize, object : TPListener {
			override fun onSuccess(s: String) {
				LogUtils.e(TAG,
					"onSuccess " + s) //				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
				var data = Gson().fromJson(s, TPWalletLoginData::class.java)
				
				runOnUiThread {
					lifecycleScope.launch {
						data.wallet?.let { getNonce(it) }
					}
				}
				
				
			}
			
			override fun onError(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onCancel(s: String) {
				Toast.makeText(this@LoginActivity, s, Toast.LENGTH_LONG).show()
			}
		})
	}
	
}