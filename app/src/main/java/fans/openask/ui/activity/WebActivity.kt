package fans.openask.ui.activity

import android.annotation.TargetApi
import android.app.Activity
import android.app.ProgressDialog
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.net.http.SslError
import android.os.Build
import android.view.View
import android.webkit.*
import androidx.databinding.DataBindingUtil
import com.kongzue.dialogx.dialogs.CustomDialog
import com.kongzue.dialogx.interfaces.OnBindView
import fans.openask.R
import fans.openask.databinding.ActivityWebBinding
import fans.openask.databinding.DialogEavesdropBinding
import fans.openask.databinding.DialogFundAddedBinding
import fans.openask.model.event.FundAddedEvent
import fans.openask.utils.LogUtils
import org.greenrobot.eventbus.EventBus

/**
 *
 * Created by Irving
 */
class WebActivity : BaseActivity() {
    val TAG = "WebActivity"

    lateinit var mBinding: ActivityWebBinding
    
    var mReffer:String? = null
    
    companion object {
        fun launch(activity: Activity, title: String, url: String) {
            launch(activity,title, url,"0")
        }
        
        fun launch(activity: Activity, title: String, url: String,value:String) {
            var intent = Intent(activity, WebActivity::class.java)
            intent.putExtra("title", title)
            intent.putExtra("url", url)
            intent.putExtra("value", value)
            activity.startActivity(intent)
        }
    }

    override fun getResId(): Int {
        return R.layout.activity_web
    }

    override fun initView() {
        setStatusBarColor("#F4F4F4", true)
        
        mBinding.tvTitle.text = intent.getStringExtra("title")
        
    }

    override fun initData() {
        mBinding.webView.getSettings().setDefaultTextEncodingName("utf-8")
        mBinding.webView.getSettings().setSupportZoom(true)
        // 设置是否支持执行JS，如果设置为true会存在XSS攻击风险
        mBinding.webView.getSettings().setJavaScriptEnabled(true)
        // mBinding.webView.addJavascriptInterface(new HTMLheaderJavaScriptInterface(), "local_obj");
        mBinding.webView.setScrollBarStyle(View.SCROLLBARS_INSIDE_OVERLAY)
        // 水平不显示
        mBinding.webView.setHorizontalScrollBarEnabled(false)
        // 垂直不显示
        mBinding.webView.setVerticalScrollBarEnabled(false)

        mBinding.webView.setWebViewClient(object : WebViewClient() {
            override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                LogUtils.e(TAG, "shouldOverrideUrlLoading")
                return try {
                    if (url.startsWith("http:") || url.startsWith("https:")) {
                        LogUtils.e(
                            TAG,
                            "url.startsWith(\"http:\") || url.startsWith(\"https:\")"
                        )
                        val lStringStringHashMap = HashMap<String, String>()
                        if (!mReffer.isNullOrEmpty()) {
                            lStringStringHashMap["referer"] = mReffer!!
                            view.loadUrl(url, lStringStringHashMap)
                        } else {
                            view.loadUrl(url, lStringStringHashMap)
                        }
                        false
                    } else {
                        LogUtils.e(
                            TAG,
                            "! url.startsWith(\"http:\") || url.startsWith(\"https:\")"
                        )
                        LogUtils.e(TAG, "url = $url")
                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                        startActivity(intent)
                        true
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    true
                }
            }

            override fun onReceivedSslError(
                view: WebView,
                handler: SslErrorHandler,
                error: SslError
            ) {
                LogUtils.e(TAG, "onReceivedSslError")
                handler.proceed()
            }

            // Handle API 21+
            @TargetApi(Build.VERSION_CODES.LOLLIPOP)
            override fun shouldInterceptRequest(
                view: WebView,
                request: WebResourceRequest
            ): WebResourceResponse? {
                LogUtils.e(TAG, "shouldInterceptRequest")
                ///获取请求uir
                val url = request.url.toString()
                ///获取RequestHeader中的所有 key value
                val lRequestHeaders = request.requestHeaders
                //                Log.e("测试URI", url);
                for ((key, value) in lRequestHeaders) {
//                    Log.d("测试header", lStringStringEntry.getKey() + "  " + lStringStringEntry.getValue());
                }
                if (lRequestHeaders.containsKey("Referer")) {
                    mReffer = lRequestHeaders["Referer"]
                }
                return super.shouldInterceptRequest(view, request)
            }

            override fun onPageStarted(view: WebView?, url: String, favicon: Bitmap?) {
                LogUtils.e(TAG, "onPageStarted "+url)
                if (url.contains("/purchase-result#success")){
                    //付款成功
                    EventBus.getDefault().post(FundAddedEvent(intent.getStringExtra("value")))
                    finish()
                }else if (url.contains("/purchase-result#fail")){
                    finish()
                }else{
                    super.onPageStarted(view, url, favicon)
                    showLoadingDialog("Loading...")
                }
            }

            override fun onPageFinished(view: WebView, url: String) {
                LogUtils.e(TAG, "onPageFinished")
                super.onPageFinished(view, url)
                dismissLoadingDialog()
            }
        })

        mBinding.webView.setWebChromeClient(WebChromeClient())
        mBinding.webView.getSettings().setUseWideViewPort(true)
        // 安全考虑，防止密码泄漏，尤其是root过的手机
        mBinding.webView.getSettings().setSavePassword(false)
        val ua: String = mBinding.webView.getSettings().getUserAgentString()
        val appUA = "$ua; MYAPP"
        mBinding.webView.getSettings().setUserAgentString(appUA)
        mBinding.webView.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.NARROW_COLUMNS)

        mBinding.webView.getSettings().setDatabaseEnabled(true)
        val dir = applicationContext.getDir("database", MODE_PRIVATE).path
        
        // 启用地理定位
        mBinding.webView.getSettings().setGeolocationEnabled(true)
        // 设置定位的数据库路径
        mBinding.webView.getSettings().setGeolocationDatabasePath(dir)

        // 最重要的方法，一定要设置，这就是出不来的主要原因
        mBinding.webView.getSettings().setDomStorageEnabled(true)
        mBinding.webView.getSettings().setUseWideViewPort(true)
        mBinding.webView.getSettings().setLoadWithOverviewMode(true)

        intent.getStringExtra("url")?.let { mBinding.webView.loadUrl(it) }
    }

    override fun initEvent() {
        mBinding.ivBack.setOnClickListener {
            onBackPressed()
        }
    }

    override fun setBindingView(view: View) {
        mBinding = DataBindingUtil.bind(view)!!
    }
    
}