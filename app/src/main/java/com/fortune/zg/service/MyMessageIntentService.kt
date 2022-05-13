package com.fortune.zg.service

import android.content.Context
import com.alibaba.sdk.android.push.AliyunMessageIntentService
import com.alibaba.sdk.android.push.notification.CPushMessage
import com.fortune.zg.bean.NewVideoBean
import com.fortune.zg.event.NewVideo
import com.fortune.zg.utils.LogUtils
import com.google.gson.Gson
import org.greenrobot.eventbus.EventBus

/**
 * Created by liyazhou on 17/8/22.
 * 为避免推送广播被系统拦截的小概率事件,我们推荐用户通过IntentService处理消息互调,接入步骤:
 * 1. 创建IntentService并继承AliyunMessageIntentService
 * 2. 覆写相关方法,并在Manifest的注册该Service
 * 3. 调用接口CloudPushService.setPushIntentService
 * 详细用户可参考:https://help.aliyun.com/document_detail/30066.html#h2-2-messagereceiver-aliyunmessageintentservice
 */
class MyMessageIntentService : AliyunMessageIntentService() {
    /**
     * 推送通知的回调方法
     *
     * @param context
     * @param title
     * @param summary
     * @param extraMap
     */
    override fun onNotification(
        context: Context,
        title: String,
        summary: String,
        extraMap: Map<String, String>
    ) {
        LogUtils.d("推送MyMessageIntentService=>onNotification:title=$title,summary:$summary")
    }

    /**
     * 推送消息的回调方法
     *
     * @param context
     * @param cPushMessage
     */
    override fun onMessage(context: Context, cPushMessage: CPushMessage) {
        LogUtils.d("推送MyMessageIntentService=>onMessage:title=${cPushMessage.title},content:${cPushMessage.content}")
        //处理新视频发布
        if (cPushMessage.content != null) {
            val newVideoBean = Gson().fromJson(cPushMessage.content, NewVideoBean::class.java)
            when (newVideoBean.data.platform) {
                "mobile" -> {
                    EventBus.getDefault().postSticky(NewVideo(1))
                }
                "pc" -> {
                    EventBus.getDefault().postSticky(NewVideo(2))
                }
                else -> {
                    EventBus.getDefault().postSticky(NewVideo(0))
                }
            }
        }
    }

    /**
     * 从通知栏打开通知的扩展处理
     *
     * @param context
     * @param title
     * @param summary
     * @param extraMap
     */
    override fun onNotificationOpened(
        context: Context,
        title: String,
        summary: String,
        extraMap: String
    ) {
        LogUtils.d("推送MyMessageIntentService=>onNotificationOpened:title=$title,summary=$summary,extraMap=$extraMap")
    }

    /**
     * 无动作通知点击回调。当在后台或阿里云控制台指定的通知动作为无逻辑跳转时,通知点击回调为onNotificationClickedWithNoAction而不是onNotificationOpened
     *
     * @param context
     * @param title
     * @param summary
     * @param extraMap
     */
    override fun onNotificationClickedWithNoAction(
        context: Context,
        title: String,
        summary: String,
        extraMap: String
    ) {
        LogUtils.d("推送MyMessageIntentService=>onNotificationClickedWithNoAction:title=$title,summary=$summary,extraMap=$extraMap")
    }

    /**
     * 通知删除回调
     *
     * @param context
     * @param messageId
     */
    override fun onNotificationRemoved(context: Context, messageId: String) {
        LogUtils.d("推送MyMessageIntentService=>onNotificationRemoved:messageId=$messageId")
    }

    /**
     * 应用处于前台时通知到达回调。注意:该方法仅对自定义样式通知有效,相关详情请参考https://help.aliyun.com/document_detail/30066.html#h3-3-4-basiccustompushnotification-api
     *
     * @param context
     * @param title
     * @param summary
     * @param extraMap
     * @param openType
     * @param openActivity
     * @param openUrl
     */
    override fun onNotificationReceivedInApp(
        context: Context,
        title: String,
        summary: String,
        extraMap: Map<String, String>,
        openType: Int,
        openActivity: String,
        openUrl: String
    ) {
        LogUtils.d("推送MyMessageIntentService=>onNotificationReceivedInApp:title=$title,summary=$summary,extraMap=$extraMap,openType=$openType,openActivity=$openActivity,openUrl=$openUrl")
    }
}