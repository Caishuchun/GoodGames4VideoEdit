package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.activity.LoginActivity
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.bean.VersionBean
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.event.LoginChangePage
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_login_second.*
import kotlinx.android.synthetic.main.fragment_login_second.view.*
import org.greenrobot.eventbus.EventBus
import java.util.concurrent.TimeUnit

class LoginSecondFragment() : Fragment() {

    private var areaCode: String? = null
    private var phone: String? = null

    private var sendCodeObservable: Disposable? = null
    private var loginObservable: Disposable? = null
    private var timer: Disposable? = null
    private var checkVersionObservable: Disposable? = null

    private var lastTime = 59

    companion object {
        fun newInstance(areaCode: String, phone: String) = LoginSecondFragment().apply {
            arguments = Bundle().apply {
                putString(AREA_CODE, areaCode)
                putString(PHONE, phone)
            }
        }

        const val AREA_CODE = "area_code"
        const val PHONE = "phone"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            areaCode = it.getString(AREA_CODE)
            phone = it.getString(PHONE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_login_second, container, false)
        initView(view)
        val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME, 0L)
        val currentTimeMillis = SystemClock.uptimeMillis()
        if (oldTimeMillis == 0L) {
            //历史时间没有的话,就要重新倒计时
        } else {
            when {
                currentTimeMillis - oldTimeMillis > 60 * 1000 -> {
                    //当前时间超过历史时间1分钟,重新倒计时
                }
                currentTimeMillis < oldTimeMillis -> {
                    //当前时间小于历史时间,说明重新开机过,重新倒计时
                }
                else -> {
                    //直接获取剩余时间,倒计时
                    lastTime -= ((currentTimeMillis - oldTimeMillis) / 1000).toInt()
                }
            }
        }
        toShowTime(view)
        return view
    }

    /**
     * 短信倒计时显示
     */
    @SuppressLint("SetTextI18n")
    private fun toShowTime(view: View) {
        timer?.dispose()
        timer = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                if (!MyApp.isBackground) {
                    if (lastTime > 0) {
                        view.tv_login_second_reSend.isEnabled = false
                        view.tv_login_second_reSend.text =
                            "${MyApp.getInstance().getString(R.string.resend)}(${lastTime}s)"
                        view.tv_login_second_reSend.setTextColor(
                            MyApp.getInstance().resources.getColor(
                                R.color.white_FFFFFF
                            )
                        )
                        lastTime--
                    } else {
                        lastTime = 59
                        timer?.dispose()
                        view.tv_login_second_reSend.isEnabled = true
                        view.tv_login_second_reSend.text =
                            MyApp.getInstance().getString(R.string.resend)
                        view.tv_login_second_reSend.setTextColor(
                            MyApp.getInstance().resources.getColor(
                                R.color.orange_FFC273
                            )
                        )
                    }
                }
            }, {})
    }


    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView(view: View) {
        view.tv_login_second_phone.text =
            "${getString(R.string.send_to)} $areaCode $phone"

        RxView.clicks(view.iv_login_second_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                EventBus.getDefault().postSticky(LoginChangePage(2, null, null))
            }
        RxView.clicks(view.tv_login_second_reSend)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                toGetCode(view)
            }

        RxTextView.textChanges(view.et_login_second_code)
            .skipInitialValue()
            .subscribe {
                changeCodeBg(it.toString(), view)
                if (it.length == 6) {
                    toLogin(it.toString())
                }
            }
    }

    /**
     * 短信开始登录
     */
    private fun toLogin(code: String) {
        DialogUtils.showBeautifulDialog(activity as LoginActivity)
        val login = RetrofitUtils.builder().login(phone!!, code.toInt())
        loginObservable = login.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success==>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            SPUtils.putValue(SPArgument.LOGIN_TOKEN, it.data?.token)
                            EventBus.getDefault().postSticky(LoginStatusChange(true))
                            (activity as LoginActivity).finish()
                        }
                        else -> {
                            it.msg?.let { it1 -> ToastUtils.show(it1) }
                            et_login_second_code.setText("")
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as LoginActivity, it))
            })
    }

    /**
     * 跳转到主界面前,先检查版本更新状态
     */
    @SuppressLint("CheckResult")
    private fun toMain4CheckVersion() {
        val checkVersion = RetrofitUtils.builder().checkVersion()
        checkVersionObservable = checkVersion.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    if (it.getCode() == 1) {
                        VersionBean.setData(it.getData()!!)
                        toMain()
                    } else if (it.getCode() == -1) {
                        ToastUtils.show(it.getMsg()!!)
                        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
                        ActivityManager.toSplashActivity(activity as LoginActivity)
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }

    /**
     * 跳转到主界面
     */
    private fun toMain() {
        SPUtils.putValue(SPArgument.IS_LOGIN, true)
        startActivity(Intent(activity, MainActivityV5::class.java))
        activity?.finish()
    }

    /**
     * 实时修改验证码界面
     */
    private fun changeCodeBg(code: String, view: View) {
        view.tv_code_1.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_2.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_3.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_4.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_5.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_6.setBackgroundResource(R.drawable.bg_code_enter_un)
        view.tv_code_1.text = ""
        view.tv_code_2.text = ""
        view.tv_code_3.text = ""
        view.tv_code_4.text = ""
        view.tv_code_5.text = ""
        view.tv_code_6.text = ""
        when (code.length) {
            0 -> {
                view.tv_code_1.setBackgroundResource(R.drawable.bg_code_enter)
            }
            1 -> {
                view.tv_code_2.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
            }
            2 -> {
                view.tv_code_3.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
            }
            3 -> {
                view.tv_code_4.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
            }
            4 -> {
                view.tv_code_5.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
            }
            5 -> {
                view.tv_code_6.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
                view.tv_code_5.text = code[4].toString()
            }
            6 -> {
                view.tv_code_6.setBackgroundResource(R.drawable.bg_code_enter)
                view.tv_code_1.text = code[0].toString()
                view.tv_code_2.text = code[1].toString()
                view.tv_code_3.text = code[2].toString()
                view.tv_code_4.text = code[3].toString()
                view.tv_code_5.text = code[4].toString()
                view.tv_code_6.text = code[5].toString()
            }
        }
    }

    /**
     * 获取短信验证码
     */
    private fun toGetCode(view: View) {
        DialogUtils.showBeautifulDialog(activity as LoginActivity)
        val sendCode = RetrofitUtils.builder().sendCode(phone!!)
        sendCodeObservable = sendCode.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            toShowTime(view)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as LoginActivity, it))
            })
    }

    override fun onDestroy() {
        timer?.dispose()
        sendCodeObservable?.dispose()
        loginObservable?.dispose()
        checkVersionObservable?.dispose()

        timer = null
        sendCodeObservable = null
        loginObservable = null
        checkVersionObservable = null
        super.onDestroy()
    }
}