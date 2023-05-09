package fans.openask.ui.activity

import android.content.Context
import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.akexorcist.localizationactivity.core.LocalizationActivityDelegate
import com.akexorcist.localizationactivity.core.OnLocaleChangedListener
import com.gyf.immersionbar.ImmersionBar
import com.kongzue.dialogx.dialogs.TipDialog
import com.kongzue.dialogx.dialogs.WaitDialog
import fans.openask.R
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.util.*

/**
 *
 * Created by Irving
 */
abstract class BaseActivity : AppCompatActivity(),OnLocaleChangedListener {

    private val localizationDelegate = LocalizationActivityDelegate(this)

    override fun onCreate(savedInstanceState: Bundle?) {
        localizationDelegate.addOnLocaleChangedListener(this)
        localizationDelegate.onCreate()

        super.onCreate(savedInstanceState)
        val contentView = LayoutInflater.from(this).inflate(getResId(), null)

        setContentView(contentView)
        setBindingView(contentView)
        initView()
        initData()
        initEvent()

        EventBus.getDefault().register(this)
    }

    abstract fun getResId(): Int

    abstract fun initView()

    abstract fun initData()

    abstract fun initEvent()

    abstract fun setBindingView(view: View)

    override fun onStart() {
        super.onStart()
    }

    override fun onResume() {
        super.onResume()
        localizationDelegate.onResume(this)
    }

    override fun onStop() {
        super.onStop()
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(any: Any) {

    }
    
    override fun attachBaseContext(newBase: Context?) {
        applyOverrideConfiguration(newBase?.let { localizationDelegate.updateConfigurationLocale(it) })
        super.attachBaseContext(newBase)
    }

//    override fun attachBaseContext(base: Context?) {
//        applyOverrideConfiguration(base?.let { localizationDelegate.updateConfigurationLocale(it) })
//        super.attachBaseContext(base?.let { ViewPumpContextWrapper.wrap(it) })
//    }

    override fun getApplicationContext(): Context {
        return localizationDelegate.getApplicationContext(super.getApplicationContext())
    }

    override fun getResources(): Resources {
        return localizationDelegate.getResources(super.getResources())
    }

    fun setLanguage(language:String?){
        localizationDelegate.setLanguage(this,language!!)
    }

    fun setLanguage(locale: Locale?) {
        localizationDelegate.setLanguage(this, locale!!)
    }

    val currentLanguage: Locale
        get() = localizationDelegate.getLanguage(this)

    override fun onBeforeLocaleChanged() {
    }

    override fun onAfterLocaleChanged() {
    }

    /**
     * 设置状态栏主题色
     */
    protected open fun setStatusBarColorPrimary() {
        ImmersionBar
            .with(this)
            .statusBarColor(R.color.title_home)
            .statusBarDarkFont(false)
            .fitsSystemWindows(true)
            .init()
    }

    fun showLoadingDialog(content: String) {
        WaitDialog.show(content)
    }

    fun dismissLoadingDialog() {
        WaitDialog.dismiss()
    }

    fun showSuccessDialog(content: String) {
        TipDialog.show(content, WaitDialog.TYPE.SUCCESS)
    }

    fun showFailedDialog(content: String) {
        TipDialog.show(content, WaitDialog.TYPE.ERROR)
    }

    /**
     * 设置状态栏透明并且全屏
     */
    protected open fun setStatusBarTranParent() {
        ImmersionBar
            .with(this)
            .transparentStatusBar()
            .statusBarDarkFont(true)
            .init()
    }

    protected open fun setStatusBarColor(statusBarColor: String, darkFont: Boolean) {
        ImmersionBar
            .with(this)
            .statusBarColor(statusBarColor)
            .statusBarDarkFont(darkFont)
            .fitsSystemWindows(true)
            .init()
    }

    enum class FontType {
        BALSAMIQSANS_BOLD, BALSAMIQSANS_BOLDLTALIC, BALSAMIQSANS_LTALIC, BALSAMIQSANS_REGULAR
    }
}