package com.fans.donut.utils.oss

import android.content.Context
import androidx.lifecycle.lifecycleScope
import com.alibaba.sdk.android.oss.ClientConfiguration
import com.alibaba.sdk.android.oss.ClientException
import com.alibaba.sdk.android.oss.OSSClient
import com.alibaba.sdk.android.oss.ServiceException
import com.alibaba.sdk.android.oss.callback.OSSCompletedCallback
import com.alibaba.sdk.android.oss.common.auth.OSSCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSFederationCredentialProvider
import com.alibaba.sdk.android.oss.common.auth.OSSFederationToken
import com.alibaba.sdk.android.oss.common.auth.OSSStsTokenCredentialProvider
import com.alibaba.sdk.android.oss.model.PutObjectRequest
import com.alibaba.sdk.android.oss.model.PutObjectResult
import com.fans.donut.data.file.OSSTokenData
import com.google.gson.Gson
import fans.openask.http.errorMsg
import fans.openask.utils.LogUtils

/**
 *
 * Created by Irving
 */
class FileUploader {
    val TAG = "FileUploader"

    var oss: OSSClient? = null

    fun uploadFile(contxt: Context, data: OSSTokenData, filePath: String, listener: UploadListener) {
        listener.onStart()

//        val credentialProvider: OSSCredentialProvider =
//            object : OSSFederationCredentialProvider() {
//                override fun getFederationToken(): OSSFederationToken {
//                    return OSSFederationToken(data.accessKeyId, data.accessKeySecret, "", "")
//                }
//            }
        
        val  credentialProvider = OSSStsTokenCredentialProvider(data.accessKeyId, data.accessKeySecret, "")

        val conf = ClientConfiguration()
        conf.connectionTimeout = 15 * 1000 // 连接超时，默认15秒
        conf.socketTimeout = 15 * 1000 // socket超时，默认15秒
        conf.maxConcurrentRequest = 5 // 最大并发请求书，默认5个
        conf.maxErrorRetry = 2 // 失败后最大重试次数，默认2次
        
        oss = OSSClient(contxt, data.endpoint, credentialProvider, conf)

        var putRequest = PutObjectRequest(data.bucket, data.fileName, filePath)

        putRequest.setProgressCallback { request, currentSize, totalSize ->
            LogUtils.e(TAG,"onProgress $currentSize---$totalSize")
            listener.onProgress((100 * currentSize / totalSize).toInt())
        }

        oss?.asyncPutObject(putRequest,
            object : OSSCompletedCallback<PutObjectRequest, PutObjectResult> {
                override fun onSuccess(request: PutObjectRequest?, result: PutObjectResult?) {
                    LogUtils.e(TAG,"onSuccess request ${Gson().toJson(request)}")
                    LogUtils.e(TAG,"onSuccess result ${Gson().toJson(result)}")
                    listener.onSuccess(request)
                }

                override fun onFailure(
                    request: PutObjectRequest?,
                    clientException: ClientException?,
                    serviceException: ServiceException?
                ) {
                    LogUtils.e(TAG,"onFailure:"+clientException?.errorMsg+"---"+serviceException?.errorMsg)
                    listener.onFailure(clientException?.message.toString())
                }

            })

    }

    interface UploadListener {
        fun onStart()

        fun onProgress(progress: Int)

        fun onSuccess(request: PutObjectRequest?)

        fun onFailure(msg: String)
    }


}