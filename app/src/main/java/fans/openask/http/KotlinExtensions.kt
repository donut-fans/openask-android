package fans.openask.http

import android.content.Context
import android.net.ConnectivityManager
import com.google.gson.JsonSyntaxException
import fans.openask.R
import fans.openask.OpenAskApplication
import fans.openask.utils.ToastUtils
import kotlinx.coroutines.TimeoutCancellationException
import rxhttp.wrapper.exception.HttpStatusCodeException
import rxhttp.wrapper.exception.ParseException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeoutException

fun Throwable.show() {
    errorMsg.show()
}

fun String.show() {
    ToastUtils.show(this)
}

val Throwable.errorCode: Int
    get() =
        when (this) {
            is HttpStatusCodeException -> this.statusCode //Http状态码异常
            is ParseException -> this.errorCode.toIntOrNull() ?: -1     //业务code异常
            else -> -1
        }

val Throwable.errorMsg: String
    get() {
        return if (this is UnknownHostException) { //网络异常
            if (!isNetworkConnected(fans.openask.OpenAskApplication.instance.applicationContext))
                fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_nonetwork)
            else
                fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_net_unavailable)
        } else if (
            this is SocketTimeoutException  //okhttp全局设置超时
            || this is TimeoutException     //rxjava中的timeout方法超时
            || this is TimeoutCancellationException  //协程超时
        ) {
            fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_timeout)
        } else if (this is ConnectException) {
            fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_net_slow)
        } else if (this is HttpStatusCodeException) {               //请求失败异常
            fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_state_error)+errorCode
        } else if (this is JsonSyntaxException) {  //请求成功，但Json语法异常,导致解析失败
            fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_parse_error)
        } else if (this is ParseException) {       // ParseException异常表明请求成功，但是数据不正确
            message+""   //msg为空，显示code
        } else {
            fans.openask.OpenAskApplication.instance.applicationContext.getString(R.string.errmsg_state_error)
        }
    }

private fun isNetworkConnected(context: Context): Boolean {
    val mConnectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    val mNetworkInfo = mConnectivityManager.activeNetworkInfo
    if (mNetworkInfo != null) {
        return mNetworkInfo.isAvailable
    }
    return false
}