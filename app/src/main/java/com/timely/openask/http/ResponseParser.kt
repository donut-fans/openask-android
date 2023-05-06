package com.timely.openask.http

import rxhttp.wrapper.annotation.Parser
import rxhttp.wrapper.exception.ParseException
import rxhttp.wrapper.parse.TypeParser
import rxhttp.wrapper.utils.convertTo
import java.io.IOException
import java.lang.reflect.Type
import com.tencent.mmkv.MMKV
import com.timely.openask.OpenAskApplication
import com.timely.openask.model.BaseRep
import com.timely.openask.model.UserInfo
import com.timely.openask.utils.LogUtils


/**
 * 输入T,输出T,并对code统一判断
 * User: ljx
 * Date: 2018/10/23
 * Time: 13:49
 *
 * 如果使用协程发送请求，wrappers属性可不设置，设置了也无效
 */
@Parser(name = "Response")
open class ResponseParser<T> : TypeParser<T> {
    val TAG = "ResponseParser"
    
    /**
     * 此构造方法适用于任意Class对象，但更多用于带泛型的Class对象，如：List<Student>
     *
     * 用法:
     * Java: .asParser(new ResponseParser<List<Student>>(){})
     * Kotlin: .asParser(object : ResponseParser<List<Student>>() {})
     *
     * 注：此构造方法一定要用protected关键字修饰，否则调用此构造方法将拿不到泛型类型
     */
    protected constructor() : super()

    /**
     * 此构造方法仅适用于不带泛型的Class对象，如: Student.class
     *
     * 用法
     * Java: .asParser(new ResponseParser<>(Student.class))   或者  .asResponse(Student.class)
     * Kotlin: .asParser(ResponseParser(Student::class.java)) 或者  .asResponse<Student>()
     */
    constructor(type: Type) : super(type)

    @Throws(IOException::class)
    override fun onParse(response: okhttp3.Response): T {
        LogUtils.e(TAG,"onParse = "+response.body)
        
        val data: BaseRep<T> = response.convertTo(BaseRep::class, *types)
        var t = data.data //获取data字段

        LogUtils.e(TAG, "onParse = t = $t T = ")

        if (t == null && types[0] === String::class.java) {
            /*
             * 考虑到有些时候服务端会返回：{"errorCode":0,"errorMsg":"关注成功"}  类似没有data的数据
             * 此时code正确，但是data字段为空，直接返回data的话，会报空指针错误，
             * 所以，判断泛型为String类型时，重新赋值，并确保赋值不为null
             */
            t = data.message as T
        } else if (t == null && types[0] === Any::class.java) {
            t = Any() as T
        } else if (t == null && types[0] === MutableList::class.java) {
            t = mutableListOf<Any>() as T
        } else if (t == null && types[0] === List::class.java) {
            t = listOf<Any>() as T
        } else if (t == null && types[0] === ArrayList::class.java) {
            t = arrayListOf<Any>() as T
        }

        if (data.code != "0" || t == null) { //code不等于0，说明数据不正确，抛出异常
            if (data.code == "0200002") {
                OpenAskApplication.instance.startReLogin()
            } else if (data.code == "0200001") {
                var userInfo = MMKV.defaultMMKV().decodeParcelable("userInfo", UserInfo::class.java)
                if (userInfo == null) {
                    OpenAskApplication.instance.startReLogin()
                } else {
                    userInfo.token?.let { OpenAskApplication.instance.initRxHttp(it) }
                }
            }
            throw ParseException(data.code.toString(), data.message, response)
        }
        return t
    }
}