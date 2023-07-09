package fans.openask.ui.activity

import android.content.Intent
import android.graphics.Typeface
import android.os.Handler
import android.text.Editable
import android.text.SpannableStringBuilder
import android.text.TextWatcher
import android.text.style.StyleSpan
import android.view.View
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.lifecycle.lifecycleScope
import com.kongzue.dialogx.dialogs.BottomMenu
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import com.tokenpocket.opensdk.base.TPListener
import com.tokenpocket.opensdk.base.TPManager
import com.tokenpocket.opensdk.simple.model.Blockchain
import com.tokenpocket.opensdk.simple.model.Transfer
import fans.openask.BuildConfig
import fans.openask.R
import fans.openask.databinding.ActivityFundAddBinding
import fans.openask.databinding.DialogFundAddedBinding
import fans.openask.http.errorMsg
import fans.openask.model.USDChargeModel
import fans.openask.model.WalletData
import fans.openask.model.event.BecomeSenseiEvent
import fans.openask.model.event.FundAddedEvent
import fans.openask.utils.LogUtils
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
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
	
	var wallet:WalletData? = null
	
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
		lifecycleScope.launch {
			getWallet()
		}
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
						
						setValueText(wallet)
						false
					}
		}
		
		mBinding.tvBtn.setOnClickListener {
			if (mBinding.etPrice.text.isNullOrEmpty()){
				ToastUtils.show("Enter Amount Please")
				return@setOnClickListener
			}
			
			when (mBinding.tvType.text){
				"USD" -> lifecycleScope.launch {
					usd(mBinding.etPrice.text.toString().toInt())
				}
				
				"USDT" -> lifecycleScope.launch { usdt(mBinding.etPrice.text.toString().toDouble()) }
				
				"USDC" -> lifecycleScope.launch { usdc(mBinding.etPrice.text.toString().toDouble()) }
			}
		}
		
		mBinding.etPrice.addTextChangedListener(object :TextWatcher{
			override fun beforeTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			
			}
			
			override fun onTextChanged(p0: CharSequence?, p1: Int, p2: Int, p3: Int) {
			}
			
			override fun afterTextChanged(p0: Editable?) {
				setValueText(wallet)
			}
			
		})
	}
	
	override fun setBindingView(view: View) {
		mBinding = DataBindingUtil.bind(view)!!
	}
	
	@Subscribe(threadMode = ThreadMode.MAIN)
	fun onFundAddedEvent(event: FundAddedEvent) {
		Handler().postDelayed(Runnable {
			showFundAddDialog(event.amout)
		},500)
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
					
					it.prePayResult?.url?.let { it1 ->
						WebActivity.launch(this,"USD Charge", it1,value.toString())
					}
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private suspend fun getWallet(){
		showLoadingDialog("Loading...")
		RxHttp.get("/open-ask/acc/wallet")
				.add("clientType", 7)
				.add("clientId", 7)
				.toAwaitResponse<WalletData>().awaitResult {
					LogUtils.e(TAG, "awaitResult = " + it.toString())
					dismissLoadingDialog()
					wallet = it
					
					setValueText(wallet)
				}.onFailure {
					LogUtils.e(TAG, "onFailure = " + it.message.toString())
					showFailedDialog(it.errorMsg)
				}
	}
	
	private fun setValueText(data:WalletData?){
		if (data == null){
			return
		}
		if (data.accountDtos.isNullOrEmpty()){
			return
		}
		
		var model:WalletData.AccountCoinModel? = null
		for(i in 0..data.accountDtos!!.size){
			if (data.accountDtos!![i].currency == mBinding.tvType.text.toString()){
				model = data.accountDtos!![i]
				break
			}
		}
		
		if (model == null){
			return
		}
		
		var text1 = model.symbol+model.totalBalance
		// 创建加粗的字体样式
		val boldStyleSpan = StyleSpan(Typeface.BOLD)
		// 创建文本内容
		val builder = SpannableStringBuilder()
		builder.append("Your ")
		builder.append(model.currency)
		builder.append(" balance ")
		builder.append(text1, boldStyleSpan, SpannableStringBuilder.SPAN_INCLUSIVE_EXCLUSIVE)
		
		if (!mBinding.etPrice.text.isNullOrEmpty()){
			var text2 = model.symbol+mBinding.etPrice.text.toString()
			var text3 = model.symbol + (model.totalBalance?.plus(mBinding.etPrice.text.toString().toDouble()))
			
			builder.append(", by adding ")
			builder.append(text2, boldStyleSpan, SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE)
			builder.append(",\nit will be ")
			builder.append(text3, boldStyleSpan, SpannableStringBuilder.SPAN_INCLUSIVE_INCLUSIVE)
		}
		
		// 设置文本内容到TextView
		mBinding.tvValueDesc.setText(builder, TextView.BufferType.SPANNABLE)
	}
	
	private fun usdc(value:Double){
		val transfer = Transfer()
		//标识链
		val blockchains: MutableList<Blockchain> = ArrayList()
		//evm系列，第一个参数是ethereum,第二个参数是网络的id，这里1是eth网络链上id
//		if (BuildConfig.DEBUG){
//			blockchains.add(Blockchain("Sepolia", "2357"))
//		}else {
//			blockchains.add(Blockchain("ethereum", "1"))
//		}
		
		if (BuildConfig.DEBUG){
			blockchains.add(Blockchain("ethereum", "11155111"))
		}else{
			blockchains.add(Blockchain("ethereum", "1"))
		}
		transfer.blockchains = blockchains
		
		transfer.protocol = "TokenPocket"
		transfer.version = "1.0"
		transfer.dappName = "OpenAsk"
		transfer.dappIcon = "https://eosknights.io/img/icon.png"
		//开发者自己定义的业务Id,用来标识这次操作
//		transfer.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		//data，如果是转原生代币，可以添加上链数据
		transfer.action = "transfer"
		//发送者
//		transfer.from = "0x9Cb12550F94e06F2322FCd5F084cD786290f8EF9"
		//接受者
		transfer.to = "0xD67405E2Df127096fbf38De0FD0e81D97E2F8c42"//pp测试钱包地址
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
//		if (BuildConfig.DEBUG){
//			blockchains.add(Blockchain("Sepolia", "2357"))
//		}else {
//			blockchains.add(Blockchain("ethereum", "1"))
//		}
		
		if (BuildConfig.DEBUG){
			blockchains.add(Blockchain("ethereum", "11155111"))
		}else{
			blockchains.add(Blockchain("ethereum", "1"))
		}
		transfer.blockchains = blockchains
		
		transfer.protocol = "TokenPocket"
		transfer.version = "1.0"
		transfer.dappName = "OpenAsk"
		transfer.dappIcon = "https://eosknights.io/img/icon.png"
		//开发者自己定义的业务Id,用来标识这次操作
//		transfer.actionId = "web-db4c5466-1a03-438c-90c9-2172e8becea5"
		//data，如果是转原生代币，可以添加上链数据
		transfer.action = "transfer"
		//发送者
//		transfer.from = OpenAskApplication.instance.userInfo?.
		//接受者
		transfer.to = "0xD67405E2Df127096fbf38De0FD0e81D97E2F8c42"//pp测试钱包地址
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
	
	private fun showFundAddDialog(value:String?){
		CustomDialog.show(object : OnBindView<CustomDialog>(R.layout.dialog_fund_added) {
			override fun onBind(dialog: CustomDialog, v: View) {
				var binding = DataBindingUtil.bind<DialogFundAddedBinding>(v)!!
				binding.ivClose.setOnClickListener { dialog.dismiss() }
				
				binding.tvTitleSub.text = "Great, you have just added $$value to your wallet"
				
				binding.tvBtn.setOnClickListener {
					dialog.dismiss()
					finish()
				}
			}
		}).setMaskColor(resources.getColor(R.color.black_50))
	}
	
}