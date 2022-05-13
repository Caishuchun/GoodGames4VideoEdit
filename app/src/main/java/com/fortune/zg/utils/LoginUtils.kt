package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.view.Gravity
import android.view.LayoutInflater
import com.fortune.zg.R
import com.fortune.zg.activity.LoginActivity
import com.fortune.zg.constants.FilesArgument
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.http.RetrofitUtils
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.mobile.auth.gatewayauth.AuthRegisterViewConfig
import com.mobile.auth.gatewayauth.AuthUIConfig
import com.mobile.auth.gatewayauth.PhoneNumberAuthHelper
import com.mobile.auth.gatewayauth.TokenResultListener
import com.mobile.auth.gatewayauth.model.TokenRet
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.layout_quick_login_body.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

object LoginUtils {

    private var quickLogin4AliObservable: Disposable? = null
    private var helper: PhoneNumberAuthHelper? = null

    /**
     * 阿里云一键登录
     */
    @SuppressLint("CheckResult")
    fun toQuickLogin(activity: Activity) {
        val mTokenResultListener = object : TokenResultListener {
            override fun onTokenSuccess(result: String?) {
                LogUtils.d("Ali=>onTokenSuccess:${Gson().toJson(result)}")
                if (result != null) {
                    val tokenRet = Gson().fromJson<TokenRet>(
                        result,
                        TokenRet::class.java
                    )
                    toDealAliListener(activity, tokenRet)
                }
            }

            override fun onTokenFailed(result: String?) {
                LogUtils.d("Ali=>onTokenFailed:${Gson().toJson(result)}")
                val tokenRet = Gson().fromJson<TokenRet>(
                    result,
                    TokenRet::class.java
                )
                toDealAliListener(activity, tokenRet)
            }
        }
        helper = PhoneNumberAuthHelper.getInstance(activity, mTokenResultListener)
        helper?.setAuthSDKInfo("i83yX6wHzJaLnbIGXS6HkoWWkEn2Ya3GDXYoKcUC/JVAX1+fAPyow/dSTAng2wE9XOK9FKqSILSlbmz6ZY+tN3UWEPhmLPV1yDwET5IgZ0w2MXQAFRrTF7pTEMLeFoD1qizf9ik5BMvK0yR5neJLI4Sxf1cClDk8wpnnnEQPXsc4vR/9ZNyHlb1tgfaXyGF+Wzn9QUbVaei9CCuSdux2chY9azyPh7J6TiwbxKpTbGTq1iaFADtaU/UxtsoMDFziO/NsVBbvZ6fs297xBw/R48iWc/r/IIBBrBirT1EK+dGGUyd7k8Kc0w==")
        helper?.setAuthUIConfig(
            AuthUIConfig.Builder()
                //背景
                .setPageBackgroundPath("bg_login")
                //状态栏
                .setStatusBarColor(Color.parseColor("#2EA992"))
                .setWebViewStatusBarColor(Color.parseColor("#2EA992"))
                .setWebNavColor(Color.parseColor("#2EA992"))
                .setWebNavTextColor(Color.parseColor("#2EA992"))
                .setWebSupportedJavascript(true)
                //标题
                .setNavHidden(true)
                //服务商
                .setSloganTextSizeDp(14)
                .setSloganText(" ")
                .setSloganTextColor(Color.parseColor("#1A241F"))
                //掩码
                .setNumberColor(Color.WHITE)
                .setNumberSizeDp(16)
                .setNumberLayoutGravity(Gravity.CENTER)
                .setNumberColor(Color.parseColor("#FFFFFF"))
                //一键登录按钮
                .setLogBtnWidth(
                    OtherUtils.px2dp(
                        activity,
                        (Math.min(
                            PhoneInfoUtils.getWidth(activity).toFloat(),
                            PhoneInfoUtils.getHeight(activity).toFloat()
                        )) / 360.0f * 296.0f
                    )
                )
                .setLogBtnHeight(
                    OtherUtils.px2dp(
                        activity,
                        (Math.min(
                            PhoneInfoUtils.getWidth(activity).toFloat(),
                            PhoneInfoUtils.getHeight(activity).toFloat()
                        )) / 360.0f * 48.0f
                    )
                )
                .setLogBtnBackgroundPath("bg_login_enter_296_48")
                .setLogBtnText(activity.getString(R.string.login_quick))
                .setLogBtnTextColor(Color.parseColor("#63C5AD"))
                .setLogBtnTextSizeDp(14)
                //切换登录方式
                .setSwitchAccText(activity.getString(R.string.login_other_phone))
                .setSwitchAccTextColor(Color.parseColor("#FFFFFF"))
                .setSwitchAccTextSizeDp(14)
                //协议
                .setAppPrivacyOne(
                    activity.getString(R.string.user_agreement),
                    FilesArgument.PROTOCOL_SERVICE
                )
                .setAppPrivacyTwo(
                    activity.getString(R.string.privacy_agreement),
                    FilesArgument.PROTOCOL_PRIVACY
                )
                .setPrivacyBefore(activity.getString(R.string.login_tips))
                .setCheckboxHidden(true)
                .setVendorPrivacyPrefix("《")
                .setVendorPrivacySuffix("》")
                .setAppPrivacyColor(Color.parseColor("#FF0000"), Color.parseColor("#00FF00"))
                .setPrivacyState(true)
                .create()
        )
        val numView = LayoutInflater.from(activity).inflate(R.layout.layout_quick_login_num, null)
        helper?.addAuthRegistViewConfig(
            "num", AuthRegisterViewConfig.Builder()
                .setView(numView)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_NUMBER)
                .build()
        )
        val bodyView = LayoutInflater.from(activity).inflate(R.layout.layout_quick_login_body, null)
        RxView.clicks(bodyView.iv_login4ali_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                helper?.quitLoginPage()
                helper?.hideLoginLoading()
            }
        helper?.addAuthRegistViewConfig(
            "body", AuthRegisterViewConfig.Builder()
                .setView(bodyView)
                .setRootViewId(AuthRegisterViewConfig.RootViewId.ROOT_VIEW_ID_BODY)
                .build()
        )
        helper?.checkEnvAvailable(PhoneNumberAuthHelper.SERVICE_TYPE_LOGIN)
    }

    /**
     * 处理阿里云一键登录回调的返回
     */
    private fun toDealAliListener(activity: Activity, tokenRet: TokenRet) {
        LogUtils.d("Ali=>toDealAliListener==code:${tokenRet.code}")
        when (tokenRet.code) {
            "600000" -> {
                //获取token成功
                toRealLogin4Ail(activity, tokenRet.token)
            }
            "600001" -> {
                //唤起授权⻚成功
            }
            "600002" -> {
                //唤起授权⻚成功,建议切换到其他登录⽅式
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600004" -> {
                //获取运营商配置信息失败,创建⼯单联系⼯程师
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600005" -> {
                //⼿机终端不安全,切换到其他登录⽅式
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600007" -> {
                //未检测到sim卡,提示⽤户检查 SIM 卡后重试
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600008" -> {
                //蜂窝⽹络未开启,提示⽤户开启移动⽹络后重试
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600009" -> {
                //⽆法判断运营商,创建⼯单联系⼯程师
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600010" -> {
                //未知异常,创建⼯单联系⼯程师
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600011" -> {
                //创建⼯单联系⼯程师,切换到其他登录⽅式
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600012" -> {
                //预取号失败
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600013" -> {
                //运营商维护升级,该功能不可⽤,创建⼯单联系⼯程师
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600014" -> {
                //运营商维护升级，该功能已达最⼤调⽤次数,创建⼯单联系⼯程师
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600015" -> {
                //接⼝超时,切换到其他登录⽅式
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600017" -> {
                //AppID、Appkey解析失败
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600021" -> {
                //点击登录时检测到运营商已切换,提示⽤户退出授权⻚，重新登录
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600023" -> {
                //加载⾃定义控件异常
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600024" -> {
                //终端环境检查⽀持认证
                helper?.getLoginToken(activity, 2000)
            }
            "600025" -> {
                //终端检测参数错误,检查传⼊参数类型与范围是否正确
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
            "600026" -> {
                //授权⻚已加载时不允许调⽤加速或预取号接⼝, 检查是否有授权⻚拉起后，去调⽤preLogin 或者accelerateAuthPage的接⼝，该⾏为不 允许
            }
            "700000" -> {
                //点击返回，⽤户取消免密登录
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
            }
            "700001" -> {
                //点击切换按钮，⽤户取消免密登录
                helper?.hideLoginLoading()
                helper?.quitLoginPage()
                toLogin(activity)
            }
        }
    }

    /**
     * 跳转到登录界面
     */
    private fun toLogin(activity: Activity) {
        clear()
        LogUtils.d("toLogin..................")
        activity.startActivity(Intent(activity, LoginActivity::class.java))
    }

    /**
     * 真的就去登录
     */
    @SuppressLint("CheckResult")
    private fun toRealLogin4Ail(activity: Activity, accessCode: String) {
        val quickLogin4Ali = RetrofitUtils.builder().quickLogin4Ali(accessCode)
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        quickLogin4AliObservable = quickLogin4Ali.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("Result==>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                            EventBus.getDefault().postSticky(LoginStatusChange(true))
                            helper?.hideLoginLoading()
                            helper?.quitLoginPage()
                        }
                        else -> {
                            helper?.hideLoginLoading()
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                        }
                    }
                } else {
                    helper?.hideLoginLoading()
                    ToastUtils.show(activity.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                helper?.hideLoginLoading()
                LogUtils.d("Fail==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                ToastUtils.show(it.message.toString())
            })
    }

    /**
     * 专业清理数据
     */
    private fun clear() {
        quickLogin4AliObservable?.dispose()
        quickLogin4AliObservable = null
        helper?.clearPreInfo()
        helper = null
    }
}