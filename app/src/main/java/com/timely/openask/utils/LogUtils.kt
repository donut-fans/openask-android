package com.timely.openask.utils

import android.util.Log
import com.timely.openask.BuildConfig

/**
 *
 * Created by Irving
 */
class LogUtils {

    companion object {
        fun e(TAG: String, msg: String) {
            if (BuildConfig.DEBUG)
                Log.e(TAG, msg)
        }

        fun d(TAG: String, msg: String) {
            if (BuildConfig.DEBUG)
                Log.d(TAG, msg)
        }

        fun v(TAG: String, msg: String) {
            if (BuildConfig.DEBUG)
                Log.v(TAG, msg)
        }
    }

}