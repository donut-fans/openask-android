package com.timely.openask

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.annotation.RequiresApi
import cn.hutool.crypto.SecureUtil
import cn.hutool.json.JSONUtil
import com.akexorcist.localizationactivity.ui.LocalizationApplication
import com.tencent.mmkv.MMKV
import com.timely.openask.encode.QSV2
import com.timely.openask.ui.activity.LoginActivity
import com.timely.openask.utils.LogUtils
import com.timely.openask.utils.ToastUtils
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import rxhttp.RxHttpPlugins
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.Locale
import java.util.Random

/**
 *
 * Created by Irving
 */
class OpenAskApplication:LocalizationApplication() {
	private val TAG = "TimelyApplication"
	
	var timestamp = System.currentTimeMillis()
	
	var activityList = mutableListOf<Activity>()
	
	companion object {
		lateinit var instance: OpenAskApplication
	}
	
	override fun onCreate() {
		super.onCreate()
		instance = this
		
		MMKV.initialize(this)
		initRxHttp("")
		ToastUtils.init(this)
		
		registerActivityLifecycleCallbacks(object :ActivityLifecycleCallbacks{
			override fun onActivityCreated(p0: Activity, p1: Bundle?) {
				activityList.add(p0)
			}
			
			override fun onActivityStarted(p0: Activity) {
			}
			
			override fun onActivityResumed(p0: Activity) {
			}
			
			override fun onActivityPaused(p0: Activity) {
			}
			
			override fun onActivityStopped(p0: Activity) {
			}
			
			override fun onActivitySaveInstanceState(p0: Activity, p1: Bundle) {
			}
			
			override fun onActivityDestroyed(p0: Activity) {
				activityList.remove(p0)
			}
			
		})
	}
	
	fun initRxHttp(token:String){
		var builder = OkHttpClient().newBuilder()
		builder.addInterceptor(object :Interceptor{
			@RequiresApi(Build.VERSION_CODES.N)
			override fun intercept(chain: Interceptor.Chain): Response {
				val salt = "123456"
				
				val nonce = getNonce(8)
				LogUtils.e(TAG, "step0 salt=$salt,timestamp=$timestamp,nonce = $nonce")
				
				var request = chain.request()
				var body = request.body
				var charset = Charset.forName("UTF-8")
				
				var mediaType = body?.contentType()
				charset = mediaType?.charset(charset)
				LogUtils.e(TAG, "intercept")
				
				if (mediaType?.type?.toLowerCase().equals("multipart")) {
					return chain.proceed(request)
				}
				
				//获取请求的数据
				var buffer = okio.Buffer()
				body?.writeTo(buffer)
				
				if (charset != null) {
					//获取body里面的数据，并将其转换成map
					var requestData = URLDecoder.decode(buffer.readString(charset).trim(), "utf-8")
					LogUtils.e(TAG, "step1 requestData = $requestData")
					
					var map = JSONUtil.parseObj(requestData)
					var jsonStr = QSV2.en().stringify(map)
					
					//获取sign
					LogUtils.e(TAG, "step2 jsonStr = $jsonStr")
					var str = jsonStr + salt + timestamp + nonce
					
					LogUtils.e(TAG, "step3 str = $str")
					
					var sign = SecureUtil.md5(str)
					
					LogUtils.e(TAG, "step4 sign = $sign")
					
					var newRequest = request.newBuilder()
						.addHeader("X-Signature", sign)
						.addHeader("X-Nonce", nonce)
						.addHeader("X-Timestamp", timestamp.toString())//1523619204809
						.build()
					
					return chain.proceed(newRequest)
				}else{
					var str = salt + timestamp + nonce
					var sign = SecureUtil.md5(str)
					var newRequest = request.newBuilder()
						.addHeader("X-Signature", sign)
						.addHeader("X-Nonce", nonce)
						.addHeader("X-Timestamp", timestamp.toString())//1523619204809
						.build()
					return chain.proceed(newRequest)
				}
			}
		})
		
		var okHttpClient = builder.build()
		
		RxHttpPlugins.init(okHttpClient) //自定义OkHttpClient对象
			.setDebug(BuildConfig.DEBUG,true) //调试模式/分段打印/json数据缩进空间
			.setOnParamAssembly { p: rxhttp.wrapper.param.Param<*> ->
				p.addHeader("versionName", BuildConfig.VERSION_NAME)
					.addHeader("Content-Type", "application/json")
					.addHeader("Agent-Type", "ANDROID")
					.addHeader("X-Token", token)
					.addHeader("X-token", token)
					.addHeader("X-Version", BuildConfig.VERSION_NAME)
					.addHeader("X-Product", "timely")
					.addHeader("X-Language", getDefaultLanguage(this).language)
					.addHeader("X-VersionCode", BuildConfig.VERSION_CODE.toString())
			}
	}
	
	fun getNonce(sizeOfRandomString: Int): String {
		val ALLOWED_CHARACTERS = "0123456789qwertyuiopasdfghjklzxcvbnm"
		val random = Random()
		val sb = StringBuilder(sizeOfRandomString)
		for (i in 0 until sizeOfRandomString) sb.append(
			ALLOWED_CHARACTERS[random.nextInt(
				ALLOWED_CHARACTERS.length
			)]
		)
		return sb.toString()
	}
	
	override fun getDefaultLanguage(context: Context): Locale {
		return Locale.ENGLISH
	}
	
	fun startReLogin() {
		MMKV.defaultMMKV().clearAll()
		initRxHttp("")
		var activity = activityList[activityList.size - 1]
		var intent = Intent(activity, LoginActivity::class.java)
		
		intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
		
		startActivity(intent)
	}
	
}