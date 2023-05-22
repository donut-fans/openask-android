package fans.openask.ui.activity

import android.content.Intent
import android.view.View
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.kongzue.dialogx.dialogs.BottomMenu
import com.tokenpocket.opensdk.base.TPListener
import com.tokenpocket.opensdk.base.TPManager
import com.tokenpocket.opensdk.simple.model.Blockchain
import com.tokenpocket.opensdk.simple.model.Transfer
import fans.openask.BuildConfig
import fans.openask.OpenAskApplication
import fans.openask.R
import fans.openask.databinding.ActivityFundAddBinding
import fans.openask.http.errorMsg
import fans.openask.model.USDChargeModel
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
class AddFundActivity : BaseActivity() {
	var TAG = "LoginActivity"
	
	lateinit var mBinding: ActivityFundAddBinding
	
	companion object {
		fun launch(activity: BaseActivity) {
			activity.startActivity(Intent(activity, AddFundActivity::class.java))
		}
	}
	
	override fun getResId(): Int {
		return R.layout.activity_fund_add
	}
	
	override fun initView() {
		setStatusBarColor("#FFFFFF", true)
	}
	
	override fun initData() {
	}
	
	override fun initEvent() {
		mBinding.ivBack.setOnClickListener { finish() }
		
		mBinding.tvPrice1.setOnClickListener {
			mBinding.etPrice.setText("9")
		}
		
		mBinding.tvPrice2.setOnClickListener {
			mBinding.etPrice.setText("99")
		}
		
		mBinding.tvPrice3.setOnClickListener {
			mBinding.etPrice.setText("999")
		}
		
		mBinding.tvType.setOnClickListener {
			BottomMenu.show(arrayOf<String>("USD", "USDC", "USDT"))
					.setOnMenuItemClickListener { dialog, text, index ->
						when(index){
							0 -> mBinding.tvType.text = "USD"
							1 -> mBinding.tvType.text = "USDC"
							2 -> mBinding.tvType.text = "USDT"
						}
						false
					}
		}
		
		mBinding.tvBtn.setOnClickListener {
			if (mBinding.etPrice.text.isNullOrEmpty()){
				ToastUtils.show("Enter Amount Please")
				return@setOnClickListener
			}
			
			when (mBinding.tvType.text){
				"USD" -> lifecycleScope.launch { usd(mBinding.etPrice.text.toString().toInt()) }
				
				"USDT" -> lifecycleScope.launch { usdt(mBinding.etPrice.text.toString().toDouble()) }
				
				"USDC" -> lifecycleScope.launch { usdc(mBinding.etPrice.text.toString().toDouble()) }
			}
		}
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	private suspend fun usd(value:Int){
		showLoadingDialog("Loading...")
		RxHttp.postJson("/open-ask/acc/charge")
				.add("amount", value)
				.add("cancelUrl", BuildConfig.BASE_URL+"/purchase-result#fail")
				.add("successUrl", BuildConfig.BASE_URL+"/purchase-result#success")
				.add("payMethodId", 1)
				.toAwaitResponse<USDChargeModel>()
				.awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					
					it.prePayResult?.url?.let { it1 -> WebActivity.launch(this,"USD Charge", it1) }
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun usdc(value:Double){
		val transfer = Transfer()
		//标识链
		val blockchains: MutableList<Blockchain> = ArrayList()
		//evm系列，第一个参数是ethereum,第二个参数是网络的id，这里1是eth网络链上id
		if (BuildConfig.DEBUG){
			blockchains.add(Blockchain("Sepolia", "2357"))
		}else {
			blockchains.add(Blockchain("ethereum", "1"))
		}
		transfer.blockchains = blockchains
		
		transfer.protocol = "TokenPocket"
		transfer.version = "1.0"
		transfer.dappName = "OpenAsk"
		transfer.dappIcon = "https://eosknights.io/img/icon.png"
		//开发者自己定义的业务Id,用来标识这次操作
		transfer.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		//data，如果是转原生代币，可以添加上链数据
		transfer.action = "transfer"
		//发送者
		transfer.from = "0x9Cb12550F94e06F2322FCd5F084cD786290f8EF9"
		//接受者
		transfer.to = "0x74Fbf23CC80693431106Ab56Cb7c66201864c1cF"//我的测试钱包地址
		//代币合约地址，如果是转ETH，可以不设置这个参数
		if (BuildConfig.DEBUG){
			transfer.contract = "0x5D15322081C1026790406862BEeC59C762b15d3e"
		}else{
			transfer.contract = "0xa0b86991c6218b36c1d19d4a2e9eb0ce3606eb48"
		}
		
		//转账数量，比如这里demo是转0.01个USDT，就是传入0.01
		transfer.amount = value
		//必须设置
		transfer.decimal = 6
		transfer.symbol = "USDC"
		transfer.desc = "USDC Transfer"
		//开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		transfer.callbackUrl = "http://115.205.0.178:9011/taaBizApi/taaInitData"
		TPManager.getInstance().transfer(this, transfer, object : TPListener {
			override fun onSuccess(s: String) {
				//转账操作结果，注意，这里只是将交易发送后的hash返回，并不保证交易一定成功，需要开发者根据hash自行确认最终链上结果
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
				LogUtils.e(TAG,"onSuccess $s")
			}
			
			override fun onError(s: String) {
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
				LogUtils.e(TAG,"onError $s")
			}
			
			override fun onCancel(s: String) {
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
				LogUtils.e(TAG,"onCancel $s")
			}
		})
	}
	
	private fun usdt(value:Double){
		val transfer = Transfer()
		//标识链
		val blockchains: MutableList<Blockchain> = ArrayList()
		//evm系列，第一个参数是ethereum,第二个参数是网络的id，这里1是eth网络链上id
		if (BuildConfig.DEBUG){
			blockchains.add(Blockchain("Sepolia", "2357"))
		}else {
			blockchains.add(Blockchain("ethereum", "1"))
		}
		transfer.blockchains = blockchains
		
		transfer.protocol = "TokenPocket"
		transfer.version = "1.0"
		transfer.dappName = "OpenAsk"
		transfer.dappIcon = "https://eosknights.io/img/icon.png"
		//开发者自己定义的业务Id,用来标识这次操作
		transfer.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		//data，如果是转原生代币，可以添加上链数据
		transfer.action = "transfer"
		//发送者
//		transfer.from = OpenAskApplication.instance.userInfo?.
		//接受者
		transfer.to = "0x74Fbf23CC80693431106Ab56Cb7c66201864c1cF"//我的测试钱包地址
		//代币合约地址，如果是转ETH，可以不设置这个参数
		if (BuildConfig.DEBUG){
			transfer.contract = "0x5D15322081C1026790406862BEeC59C762b15d3e"
		}else{
			transfer.contract = "0xdac17f958d2ee523a2206206994597c13d831ec7"
		}
		
		//转账数量，比如这里demo是转0.01个USDT，就是传入0.01
		transfer.amount = value
		//必须设置
		transfer.decimal = 6
		transfer.symbol = "USDT"
		transfer.desc = "USDT Transfer"
		//开发者服务端提供的接受调用登录结果的接口，如果设置该参数，钱包操作完成后，会将结果通过post application json方式将结果回调给callbackurl
		transfer.callbackUrl = "http://115.205.0.178:9011/taaBizApi/taaInitData"
		TPManager.getInstance().transfer(this, transfer, object : TPListener {
			override fun onSuccess(s: String) {
				//转账操作结果，注意，这里只是将交易发送后的hash返回，并不保证交易一定成功，需要开发者根据hash自行确认最终链上结果
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onError(s: String) {
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
			}
			
			override fun onCancel(s: String) {
				Toast.makeText(this@AddFundActivity, s, Toast.LENGTH_LONG).show()
			}
		})
	}
	
}