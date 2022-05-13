package com.fortune.zg.utils

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.text.Html
import android.text.Spanned
import android.view.View
import androidx.core.content.ContextCompat.getColor
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.GiftCodeRecordsBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.dialog_gift_num.*
import kotlinx.android.synthetic.main.item_gift_code_records.view.*
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

/**
 * 礼包码专用工具类
 * 1. 根据countdown来判断是否需要获取礼包码
 * 2. 倒计时要在首页一直进行,需要的时候直接传过去
 */
object GiftNumDialogUtils {
    private var mDialog: Dialog? = null
    private var videoGiftRecordsObservable: Disposable? = null
    private var timer: Disposable? = null
    private var mCountDown = 0

    @SuppressLint("SimpleDateFormat")
    private val dateDf = SimpleDateFormat("yyyy/MM/dd HH:mm")
    private var status: Select = Select.TO_ROB
    private var type = 1

    enum class Select {
        TO_ROB, CODE, HIS_LIST
    }

    /**
     * 取消加载框
     */
    fun dismiss() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
            mDialog = null
        }
        mCountDown = 0
        videoGiftRecordsObservable?.dispose()
        videoGiftRecordsObservable = null
        timer?.dispose()
        timer = null
    }


    /**
     * 显示
     * @param videoPos 视频id
     * @param countDown 倒计时
     * @param code 礼包码
     */
    @SuppressLint("CheckResult")
    fun show(
        context: Context,
        videoPos: Int,
        countDown: Int,
        code: String?,
        giftDes: String? = null
    ) {
        if (mDialog != null) {
            dismiss()
        }
        mDialog = Dialog(context, R.style.new_circle_progress)
        mDialog?.setContentView(R.layout.dialog_gift_num)

        if (giftDes != null) {
            //是礼包描述信息
            mDialog?.ll_gift_select?.visibility = View.GONE
            mDialog?.tv_gift_des_title?.visibility = View.VISIBLE
            mDialog?.tv_gift_giftDes?.text = formatGiftDes(giftDes)

            mDialog?.ll_gift_num_toRob!!.visibility = View.GONE
            mDialog?.ll_gift_num_code!!.visibility = View.GONE
            mDialog?.ll_gift_num_hisList!!.visibility = View.GONE
            mDialog?.ll_gift_gift_des?.visibility = View.VISIBLE
        } else {
            //是正常抢礼包
            mCountDown = countDown
            if (mCountDown != 0) {
                timer = Observable.interval(0, 1, TimeUnit.SECONDS)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe {
                        if (mCountDown == 0) {
                            toShowCountDown()
                            timer?.dispose()
                            timer = null
                        } else {
                            mCountDown--
                            toShowCountDown()
                        }
                    }
            }
            //首次进来选择在礼包码
            toSelect(context, 1)
            //再根据礼包码选择是开抢还是抢成功了
            toSelectCodeStatus(code)
            type = 1
        }

        RxView.clicks(mDialog?.tv_gift_num_giftNum!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (type == 2) {
                    type = 1
                    toSelect(context, 1)
                    toSelectCodeStatus(code)
                }
            }

        RxView.clicks(mDialog?.tv_gift_num_hisList!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (type == 1) {
                    type = 2
                    toSelect(context, 2)
                    status = Select.HIS_LIST
                    mDialog?.ll_gift_num_toRob!!.visibility = View.GONE
                    mDialog?.ll_gift_num_code!!.visibility = View.GONE
                    mDialog?.ll_gift_num_hisList!!.visibility = View.VISIBLE
                    toGetHisList(context, videoPos)
                }
            }

        mDialog?.setCancelable(true)
        mDialog?.setCanceledOnTouchOutside(true)
        mDialog?.setOnCancelListener {
            dismiss()
        }
        mDialog?.show()
    }

    /**
     * 格式化礼包描述
     */
    private fun formatGiftDes(giftDes: String): Spanned? {
        var result = ""
        val giftDesList = giftDes.split("||")
        for (index in giftDesList.indices) {
            val num = when (index) {
                0 -> {
                    "一"
                }
                1 -> {
                    "二"
                }
                else -> {
                    "三"
                }
            }
            result += "<font color=\"#00bbaa\"><b>礼包$num:</b></font>&nbsp;${giftDesList[index]}"
            if (index != giftDesList.size - 1) {
                result += "<br />"
            }
        }
        result += ""
        return Html.fromHtml(result)
    }

    /**
     * 展示倒计时
     */
    private fun toShowCountDown() {
        val formatMs = TimeUtils.formatMs(mCountDown)
        val split = formatMs.split(":")
        mDialog?.tv_gift_num_timeM!!.text = split[0]
        mDialog?.tv_gift_num_timeS!!.text = split[1]
    }

    /**
     * 根据礼包码判断当前界面时开抢还是礼包码展示
     */
    private fun toSelectCodeStatus(code: String?) {
        if (null == code) {
            //code为null,说明在倒计时,则显示倒计时界面
            status = Select.TO_ROB
            mDialog?.ll_gift_num_toRob!!.visibility = View.VISIBLE
            mDialog?.ll_gift_num_code!!.visibility = View.GONE
            mDialog?.ll_gift_num_hisList!!.visibility = View.GONE
        } else {
            //code不为null,则展示code
            status = Select.CODE
            mDialog?.ll_gift_num_toRob!!.visibility = View.GONE
            mDialog?.ll_gift_num_code!!.visibility = View.VISIBLE
            mDialog?.ll_gift_num_hisList!!.visibility = View.GONE
            toShowCode(code)
        }
    }

    /**
     * 展示礼包码
     */
    private fun toShowCode(code: String) {
        mDialog?.tv_gift_num_code1!!.text = code[0].toString()
        mDialog?.tv_gift_num_code2!!.text = code[1].toString()
        mDialog?.tv_gift_num_code3!!.text = code[2].toString()
        mDialog?.tv_gift_num_code4!!.text = code[3].toString()
        mDialog?.tv_gift_num_code5!!.text = code[4].toString()
        mDialog?.tv_gift_num_code6!!.text = code[5].toString()
    }

    /**
     * 获取历史记录
     */
    private fun toGetHisList(context: Context, videoPos: Int) {
        val videoGiftRecords = RetrofitUtils.builder().videoGiftRecords(videoPos)
        videoGiftRecordsObservable = videoGiftRecords.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                val codeRecordsListAdapter =
                                    BaseAdapterWithPosition.Builder<GiftCodeRecordsBean.DataBean>()
                                        .setData(it.getData() as List<GiftCodeRecordsBean.DataBean>)
                                        .setLayoutId(R.layout.item_gift_code_records)
                                        .addBindView { itemView, itemData, position ->
                                            itemView.tv_item_gift_code_records_time.text =
                                                dateDf.format(itemData.create_time!! * 1000L)
                                            itemView.tv_item_gift_code_records_code.text =
                                                itemData.cdkey?.toString()
                                        }
                                        .create()
                                mDialog?.rv_gift_num_hisList!!.adapter = codeRecordsListAdapter
                                mDialog?.rv_gift_num_hisList!!.layoutManager =
                                    SafeLinearLayoutManager(context)
                            }
                        }
                    }
                }
            }, {
            })
    }

    /**
     * 礼包码和历史记录选择
     * @param type 1 礼包码 2 历史记录
     */
    private fun toSelect(context: Context, type: Int) {
        when (type) {
            1 -> {
                //礼包码
                mDialog?.tv_gift_num_giftNum!!.setTextColor(
                    getColor(
                        context,
                        R.color.orange_EB5032
                    )
                )
                val giftNumPaint = mDialog?.tv_gift_num_giftNum!!.paint
                giftNumPaint.isFakeBoldText = true
                mDialog?.tv_gift_num_giftNum!!.setBackgroundResource(R.drawable.bg_login_enter)
                //历史记录
                mDialog?.tv_gift_num_hisList!!.setTextColor(
                    getColor(
                        context,
                        R.color.white_FFFFFF
                    )
                )
                val hisListPaint = mDialog?.tv_gift_num_hisList!!.paint
                hisListPaint.isFakeBoldText = false
                mDialog?.tv_gift_num_hisList!!.setBackgroundResource(R.drawable.transparent)
            }
            2 -> {
                //礼包码
                mDialog?.tv_gift_num_giftNum!!.setTextColor(
                    getColor(
                        context,
                        R.color.white_FFFFFF
                    )
                )
                val giftNumPaint = mDialog?.tv_gift_num_giftNum!!.paint
                giftNumPaint.isFakeBoldText = false
                mDialog?.tv_gift_num_giftNum!!.setBackgroundResource(R.drawable.transparent)
                //历史记录
                mDialog?.tv_gift_num_hisList!!.setTextColor(
                    getColor(
                        context,
                        R.color.orange_EB5032
                    )
                )
                val hisListPaint = mDialog?.tv_gift_num_hisList!!.paint
                hisListPaint.isFakeBoldText = true
                mDialog?.tv_gift_num_hisList!!.setBackgroundResource(R.drawable.bg_login_enter)
            }
        }
    }
}