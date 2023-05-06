package com.timely.openask.utils

import android.content.Context
import android.widget.Toast

/**
 *
 * Created by Irving
 */
class ToastUtils {

    companion object{
        private var mContext: Context? = null
        private var toastList: MutableList<Toast>? = null

        fun init(context: Context?) {
            mContext = context
            toastList = ArrayList()
        }

        fun show(str: String?) {
            if (mContext == null) {
                return
            }
            val toast = Toast.makeText(mContext, str, Toast.LENGTH_SHORT)
            toastList!!.add(toast)
            if (toastList!!.size > 1) {
                var toast1: Toast? = toastList!![0]
                toastList!!.removeAt(0)
                toast1!!.cancel()
                toast1 = null
            }
            toast.show()
        }
    }
}