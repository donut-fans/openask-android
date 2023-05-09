package fans.openask.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.gyf.immersionbar.ImmersionBar
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

/**
 *
 * Created by Irving
 */
abstract class BaseFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val contentView = inflater.inflate(getResId(), container, false)
        setDataBindingView(contentView)
        initView(contentView)
        initData()
        initEvent()

        EventBus.getDefault().register(this)
        return contentView
    }

    override fun onDestroyView() {
        EventBus.getDefault().unregister(this)
        super.onDestroyView()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessageEvent(any: Any) {

    }

    abstract fun getResId(): Int

    abstract fun initView(contentView:View)

    abstract fun initData()

    abstract fun initEvent()

    abstract fun setDataBindingView(view: View)

    /**
     * 设置状态栏透明并且全屏
     */
    protected open fun setStatusBarTranParent(darkFont: Boolean) {
        ImmersionBar
            .with(this)
            .keyboardEnable(false)
            .transparentStatusBar()
            .statusBarDarkFont(darkFont)
            .init()
    }

    /**
     * Set status bar color and
     *
     * @param colorRes
     * @param alpha
     * @param darkFont
     */
    protected open fun setStatusBarColor(color: String, darkFont: Boolean) {
        ImmersionBar
            .with(this)
            .keyboardEnable(false)
            .fitsSystemWindows(true)
            .statusBarColor(color)
            .statusBarDarkFont(darkFont)
            .init()
    }

}