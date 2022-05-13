package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.os.SystemClock
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_change_phone2.*
import java.util.concurrent.TimeUnit

class ChangePhone2Activity : BaseActivity() {

    private var timer: Disposable? = null

    private var sendCodeObservable: Disposable? = null
    private var changePhoneObservable: Disposable? = null
    private var lastTime = 59
    private var currentPhone = ""

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ChangePhone2Activity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val PHONE = "phone"
    }

    override fun getLayoutId() = R.layout.activity_change_phone2

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        currentPhone = intent.getStringExtra(PHONE)!!
        initView()
        val oldTimeMillis = SPUtils.getLong(SPArgument.CODE_TIME_4_CHANGE_PHONE, 0L)
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
        toShowTime()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        tv_changePhone2_currentPhone.text = "${getString(R.string.send_to)} +86 $currentPhone"

        RxView.clicks(iv_changePhone2_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        RxView.clicks(tv_changePhone2_reSend)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { sendCode() }

        RxTextView.textChanges(et_changePhone2_code)
            .skipInitialValue()
            .subscribe {
                changeCodeBg(it.toString())
                if (it.length == 6) {
                    toChangePhone(it.toString())
                }
            }
    }

    /**
     * 开始修改手机号
     */
    private fun toChangePhone(code: String) {
        DialogUtils.showBeautifulDialog(this)
        val changePhone = RetrofitUtils.builder().changePhone(currentPhone, code.toInt())
        changePhoneObservable = changePhone.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            ToastUtils.show(getString(R.string.change_phone_success))
                            toSplash()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 跳转到起始页面进行登录
     */
    private fun toSplash() {
        SPUtils.putValue(SPArgument.LOGIN_TOKEN, null)
        ActivityManager.toSplashActivity(this)
    }

    /**
     * 实时修改验证码界面
     */
    private fun changeCodeBg(code: String) {
        tv_code_1.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_2.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_3.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_4.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_5.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_6.setBackgroundResource(R.drawable.bg_code_enter_un)
        tv_code_1.text = ""
        tv_code_2.text = ""
        tv_code_3.text = ""
        tv_code_4.text = ""
        tv_code_5.text = ""
        tv_code_6.text = ""
        when (code.length) {
            0 -> {
                tv_code_1.setBackgroundResource(R.drawable.bg_code_enter)
            }
            1 -> {
                tv_code_2.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
            }
            2 -> {
                tv_code_3.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
            }
            3 -> {
                tv_code_4.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
            }
            4 -> {
                tv_code_5.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
            }
            5 -> {
                tv_code_6.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
                tv_code_5.text = code[4].toString()
            }
            6 -> {
                tv_code_6.setBackgroundResource(R.drawable.bg_code_enter)
                tv_code_1.text = code[0].toString()
                tv_code_2.text = code[1].toString()
                tv_code_3.text = code[2].toString()
                tv_code_4.text = code[3].toString()
                tv_code_5.text = code[4].toString()
                tv_code_6.text = code[5].toString()
            }
        }
    }

    /**
     * 发送短信验证码
     */
    private fun sendCode() {
        DialogUtils.showBeautifulDialog(this)
        val sendCode4changePhone = RetrofitUtils.builder().sendCode4changePhone(currentPhone)
        sendCodeObservable = sendCode4changePhone
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            toShowTime()
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
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
                LogUtils.d("fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }


    /**
     * 倒计时
     */
    private fun toShowTime() {
        timer?.dispose()
        timer = Observable.interval(0, 1, TimeUnit.SECONDS)
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                if (lastTime > 0) {
                    tv_changePhone2_reSend.isEnabled = false
                    tv_changePhone2_reSend.text =
                        "${MyApp.getInstance().getString(R.string.resend)}(${lastTime}s)"
                    tv_changePhone2_reSend.setTextColor(
                        MyApp.getInstance().resources.getColor(
                            R.color.black_1A241F
                        )
                    )
                    lastTime--
                } else {
                    lastTime = 59
                    timer?.dispose()
                    tv_changePhone2_reSend.isEnabled = true
                    tv_changePhone2_reSend.text =
                        MyApp.getInstance().getString(R.string.resend)
                    tv_changePhone2_reSend.setTextColor(
                        MyApp.getInstance().resources.getColor(
                            R.color.orange_FFC273
                        )
                    )
                }
            }
    }

    override fun destroy() {
        timer?.dispose()
        changePhoneObservable?.dispose()
        sendCodeObservable?.dispose()

        timer = null
        changePhoneObservable = null
        sendCodeObservable = null
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
    }
}