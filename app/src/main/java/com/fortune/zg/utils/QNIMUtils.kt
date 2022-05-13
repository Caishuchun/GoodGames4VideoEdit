package com.fortune.zg.utils

import com.qiniu.droid.imsdk.QNIMClient
import im.floo.BMXCallBack
import im.floo.BMXDataCallBack
import im.floo.floolib.*

object QNIMUtils {
    private var mChatListener: BMXChatServiceListener? = null

    var userName = "" //用户名
    var userPass = "" //用户密码
    var groupIdList = mutableListOf<Long>()

    /**
     * 用户注册
     * @param userName 用户名
     * @param userPass 密码
     * @param callBack 监听
     */
    fun toSignUpNewUser(
        userName: String,
        userPass: String,
        callBack: BMXDataCallBack<BMXUserProfile>
    ) {
        QNIMClient.getUserManager().signUpNewUser(userName, userPass, callBack)
    }


    /**
     * 用户登录
     * @param userName 用户名
     * @param userPass 密码
     * @param callBack 监听
     */
    fun toSignInByName(userName: String, userPass: String, callBack: BMXCallBack) {
        QNIMClient.getUserManager().signInByName(userName, userPass, callBack)
    }

    /**
     * 设置一下用户信息
     * @param nickName 昵称
     * @param callBack 获取用户Id之类的
     */
    fun toSetUserInfo(nickName: String, callBack: BMXDataCallBack<BMXUserProfile>) {
        QNIMClient.getUserManager().setNickname(nickName, null)
        QNIMClient.getUserManager().getProfile(true, callBack)
    }

    /**
     * 用户退出登录
     */
    fun toSignOut(needRemoveListener: Boolean = true) {
        QNIMClient.getUserManager().signOut(null)
        if (needRemoveListener) {
            removeAllListener()
        }
    }

    /**
     * 创建聊天室
     * @param roomName 聊天室名称
     * @param callBack 监听
     */
    fun toCreateChatRoom(roomName: String, callBack: BMXDataCallBack<BMXGroup>) {
        QNIMClient.getChatRoomManager().create(roomName, callBack)
    }

    /**
     * 加入聊天室
     * @param groupId 聊天室Id
     * @param callBack 监听
     */
    fun toJoinChatRoom(groupId: Long, callBack: BMXCallBack) {
        QNIMClient.getChatRoomManager().join(groupId, callBack)
    }

    /**
     * 退出聊天室
     * @param groupId 聊天室Id
     * @param callBack 监听
     */
    fun toLeaveChatRoom(groupId: Long, callBack: BMXCallBack?) {
        QNIMClient.getChatRoomManager().leave(groupId, callBack)
    }

    /**
     * 退出聊天室
     */
    fun toLeaveChatRoom(callBack: BMXCallBack?) {
        if (groupIdList.size > 0) {
            for (index in 0 until groupIdList.size) {
                if (index != groupIdList.size - 1) {
                    QNIMClient.getChatRoomManager().leave(groupIdList[index], null)
                } else {
                    QNIMClient.getChatRoomManager().leave(groupIdList[index], callBack)
                    groupIdList.clear()
                }
            }
        }
    }

    /**
     * 解散聊天室
     * @param groupId 聊天室Id
     * @param callBack 监听
     */
    fun toDestroyChatRoom(groupId: Long, callBack: BMXCallBack) {
        QNIMClient.getChatRoomManager().destroy(groupId, callBack)
    }

    /**
     * 发送信息
     */
    fun toSendMessage(
        fromId: Long,
        toId: Long,
        nickName: String,
        content: String,
        isFistJoin: Boolean = false
    ) {
        val message =
            BMXMessage.createMessage(fromId, toId, BMXMessage.MessageType.Group, toId, content)
        message.setSenderName(nickName)
        if (isFistJoin) {
            message.setExtension("1")
        }
        QNIMClient.getChatManager().sendMessage(message)
    }

    /**
     * 设置聊天消息的监听
     * 主要监听自己发送消息和接收消息
     */
    fun setChatListener(chatListener: OnChatListener) {
        mChatListener = object : BMXChatServiceListener() {
            //自己发送信息的状态更新
            override fun onStatusChanged(msg: BMXMessage?, error: BMXErrorCode?) {
                super.onStatusChanged(msg, error)
                chatListener.onStatusChanged(msg, error)
            }

            //接收到信息
            override fun onReceive(list: BMXMessageList?) {
                super.onReceive(list)
                chatListener.onReceive(list)
            }
        }
        QNIMClient.getChatManager().addChatListener(mChatListener)
    }

    interface OnChatListener {
        fun onStatusChanged(msg: BMXMessage?, error: BMXErrorCode?)
        fun onReceive(list: BMXMessageList?)
    }

    /**
     * 移除所有监听
     */
    fun removeAllListener() {
        QNIMClient.getChatManager().removeChatListener(mChatListener)
    }
}