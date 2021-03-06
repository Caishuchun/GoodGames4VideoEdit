package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.net.ConnectivityManager
import android.os.Build
import android.os.Handler
import android.os.Message
import android.text.Html
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.ChatListBean
import com.fortune.zg.bean.LiveInfoBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.event.UserInfoChange
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.listener.SoftKeyBoardChangeListener
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.pili.pldroid.player.AVOptions
import com.pili.pldroid.player.PLOnErrorListener.ERROR_CODE_IO_ERROR
import com.pili.pldroid.player.widget.PLVideoView
import com.umeng.analytics.MobclickAgent
import im.floo.BMXCallBack
import im.floo.BMXDataCallBack
import im.floo.floolib.*
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_game_detail.*
import kotlinx.android.synthetic.main.activity_live.*
import kotlinx.android.synthetic.main.fragment_mv_detail.view.*
import kotlinx.android.synthetic.main.item_chat.view.*
import master.flame.danmaku.controller.DrawHandler
import master.flame.danmaku.danmaku.model.BaseDanmaku
import master.flame.danmaku.danmaku.model.DanmakuTimer
import master.flame.danmaku.danmaku.model.IDanmakus
import master.flame.danmaku.danmaku.model.android.DanmakuContext
import master.flame.danmaku.danmaku.model.android.Danmakus
import master.flame.danmaku.danmaku.parser.BaseDanmakuParser
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import java.lang.ref.WeakReference
import java.util.concurrent.TimeUnit


class LiveActivity : BaseActivity() {
    private var liveId = 0
    private var imGroupId = 0L
    private var mScreenWidth = 0
    private var mScreenHeight = 0

    private var isShow = false
    private var isLandscape = false
    private var isPause = false

    private var liveInfoObservable: Disposable? = null
    private var startTime = 0L

    private var mAdapter: BaseAdapter<ChatListBean>? = null
    private var tips =
        "????????????XXX????????????,?????????????????????~ ????????????????????????????????????????????????????????????????????????,????????????????????????????????????????????????????????????????????????????????????,?????????????????????????????????"
    private var mChatLists = mutableListOf<ChatListBean>()
    private var isBottom = true
    private var isShowKeyBroad = false
    private var keyBroadHeight = 0

    private var isShowDanmu = true
    private var gameDownloadStartObservable: Disposable? = null
    private var gameDownloadCompleteObservable: Disposable? = null
    private var gameInstallStartObservable: Disposable? = null
    private var downId: Int? = null
    private var isDownloading = false
    private var isFirstComing = true
    private var isPlayerPrepare = false
    private var isFirstLandscape = true

    private var isSignIn = false
    private var isJoinRoom = false
    private var justNeedOnce = true

    private var danmakuContext: DanmakuContext? = null
    private val parser = object : BaseDanmakuParser() {
        override fun parse(): IDanmakus {
            return Danmakus()
        }
    }

    private var userId = -1L
    private var userName = ""
    private var userPass = ""
    private var defaultNickName = ""
    private var nickName = ""

    companion object {
        lateinit var instance: LiveActivity
        const val LIVE_ID = "liveId"
        const val IM_GROUP_ID = "imGroupId"
    }

    override fun getLayoutId(): Int = R.layout.activity_live

    override fun doSomething() {
        MobclickAgent.onEvent(
            this,
            "live",
            "live_num"
        )
        EventBus.getDefault().register(this)
        userName = GetDeviceId.getDeviceId(this)
        userPass = userName
        defaultNickName = "${getString(R.string.string_008)}${(1000..9999).random()}"
        nickName = defaultNickName
        instance = this
        liveId = intent.getIntExtra(LIVE_ID, 0)
        imGroupId = intent.getLongExtra(IM_GROUP_ID, 0L)
        mScreenWidth = PhoneInfoUtils.getWidth(this)
        mScreenHeight = PhoneInfoUtils.getHeight(this)
        initView()
        initChatList()
        initQNIM()
        getInfo()
    }

    /**
     * ????????????????????????
     */
    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initChatList() {
        mAdapter = BaseAdapter.Builder<ChatListBean>()
            .setLayoutId(R.layout.item_chat)
            .setData(mChatLists)
            .addBindView { itemView, itemData ->
                if (itemData.isJoin) {
                    //????????????
                    itemView.tv_chat_content.text =
                        formatContent(itemData.nikeName, itemData.message, true)
                } else {
                    itemView.tv_chat_content.text =
                        formatContent(itemData.nikeName, itemData.message, false)
                }
            }
            .create()
        rv_live.adapter = mAdapter
        rv_live.layoutManager = SafeLinearLayoutManager(this)
    }

    /**
     * ????????????????????????
     */
    private fun formatContent(nickName: String, content: String, isJoin: Boolean): CharSequence {
        var charSequence: CharSequence = ""
        if (isJoin) {
            charSequence =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Html.fromHtml(
                        "<font color = '#1A241F'>$nickName</font> <font color = '#FF9C00'>${
                            getString(
                                R.string.string_009
                            )
                        }</font>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                } else {
                    Html.fromHtml(
                        "<font color = '#1A241F'>$nickName</font> <font color = '#FF9C00'>${
                            getString(
                                R.string.string_009
                            )
                        }</font>"
                    )
                }
        } else {
            charSequence =
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    Html.fromHtml(
                        "<font color = '#878787'>$nickName:</font> <font color = '#1A241F'>$content</font>",
                        Html.FROM_HTML_MODE_LEGACY
                    )
                } else {
                    Html.fromHtml(
                        "<font color = '#878787'>$nickName:</font> <font color = '#1A241F'>$content</font>"
                    )
                }
        }
        return charSequence
    }

    /**
     * ?????????????????????
     */
    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (ev?.action == MotionEvent.ACTION_DOWN) {
            val view = currentFocus
            if (isShouldHideKeyBoard(view, ev)) {
                OtherUtils.hindKeyboard(this, et_live)
            }
        }
        return super.dispatchTouchEvent(ev)
    }

    /**
     * ?????????????????????????????????????????????
     */
    private fun isShouldHideKeyBoard(view: View?, event: MotionEvent): Boolean {
        if (view is EditText) {
            val arrayOf = intArrayOf(0, 0)
            val parent = view.parent as View
            parent.getLocationInWindow(arrayOf)
            val left = 0
            val top = arrayOf[1]
            val screenWidth = PhoneInfoUtils.getWidth(this)
            val bottom = top + view.height
            val right = screenWidth
            //??????EditText
            return !(event.x > left && event.x < right && event.y > top && event.y < bottom)
        }
        //EditText????????????
        return false
    }

    /**
     * ???????????????IM
     */
    private fun initQNIM() {
        userName = if (MyApp.getInstance().isHaveToken())
            "hfdd_${UserInfoBean.getData()?.user_id}" else userName
        userPass = if (MyApp.getInstance().isHaveToken())
            "hfdd_${UserInfoBean.getData()?.user_id}" else userPass
        nickName = if (MyApp.getInstance().isHaveToken())
            UserInfoBean.getData()?.user_name ?: nickName else nickName
        if (nickName == getString(R.string.app_name)) {
            nickName = "${getString(R.string.app_name)}_${UserInfoBean.getData()?.user_id}"
        }
        if (QNIMUtils.userName.isNotEmpty() && QNIMUtils.userPass.isNotEmpty()
            && QNIMUtils.userName == userName && QNIMUtils.userPass == userPass
        ) {
            toSetUserInfo()
        } else {
            toSignUpNewUser()
        }
    }

    //1.??????
    private fun toSignUpNewUser() {
        QNIMUtils.toSignUpNewUser(userName, userPass, object : BMXDataCallBack<BMXUserProfile> {
            override fun onResult(error: BMXErrorCode?, userProfile: BMXUserProfile?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.toSignUpNewUser=>userName:$userName,userPass:$userPass,errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (error != null && error.swigValue() == 0) {
                    //????????????,????????????
                    toSignInByName()
                } else if (error != null && error.swigValue() == 9) {
                    //??????????????????,????????????
                    toSignInByName()
                } else {
                    //????????????
                }
            }
        })
    }

    //2.??????
    private fun toSignInByName() {
        QNIMUtils.toSignInByName(userName, userPass, object : BMXCallBack {
            override fun onResult(error: BMXErrorCode?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.toSignInByName=>userName:$userName,userPass:$userPass,errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (error != null && error.swigValue() == 0) {
                    //????????????
                    isSignIn = true
                    QNIMUtils.userName = userName
                    QNIMUtils.userPass = userPass
                    toSetUserInfo()
                } else {
                    //????????????
                }
            }
        })
    }

    /**
     * 3.??????/????????????????????????
     */
    private fun toSetUserInfo() {
        QNIMUtils.toSetUserInfo(nickName, object : BMXDataCallBack<BMXUserProfile> {
            override fun onResult(error: BMXErrorCode?, userProfile: BMXUserProfile?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.toSetUserInfo=>nickName:$nickName,errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (error != null && error.swigValue() == 0) {
                    //????????????????????????
                    userId = userProfile?.userId()!!
                    val username = userProfile.username()
                    LogUtils.d("${javaClass.simpleName}=QNIMUtils.toSetUserInfo=>username:$username")
                    toLeaveChatRoom()
                } else {
                    //????????????
                }
            }
        })
    }

    //4.???????????????_?????????????????????
    private fun toLeaveChatRoom() {
        QNIMUtils.groupIdList.add(imGroupId)
        QNIMUtils.toLeaveChatRoom(object : BMXCallBack {
            override fun onResult(error: BMXErrorCode?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.toLeaveChatRoom=>imGroupId:$imGroupId,errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (error != null && error.swigValue() == 0) {
                    toJoinChatRoom()
                } else {
                    //????????????
                }
            }
        })
    }

    //5.???????????????
    private fun toJoinChatRoom() {
        QNIMUtils.toJoinChatRoom(imGroupId, object : BMXCallBack {
            override fun onResult(error: BMXErrorCode?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.toJoinChatRoom=>imGroup:$imGroupId,errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (error != null && error.swigValue() == 0) {
                    //?????????????????????
                    MobclickAgent.onEvent(
                        this@LiveActivity,
                        "live",
                        hashMapOf(Pair("userName", userName))
                    )
                    QNIMUtils.groupIdList.add(imGroupId)
                    isJoinRoom = true
                    if (justNeedOnce) {
                        justNeedOnce = false
                        setQNListener()
                    }
                    toSendMessage("???????????????", true)
                } else {
                    //????????????
                }
            }
        })
    }

    //??????????????????????????????
    private fun setQNListener() {
        QNIMUtils.removeAllListener()
        QNIMUtils.setChatListener(object : QNIMUtils.OnChatListener {
            override fun onStatusChanged(msg: BMXMessage?, error: BMXErrorCode?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.onStatusChanged=>errorCode:${error?.swigValue()},errorMsg:${error?.name}")
                if (msg != null) {
                    LogUtils.d("${javaClass.simpleName}=QNIMUtils.onStatusChanged=>senderName:${msg.senderName()},content:${msg.content()}")
                }
            }

            @SuppressLint("SetTextI18n")
            override fun onReceive(list: BMXMessageList?) {
                LogUtils.d("${javaClass.simpleName}=QNIMUtils.onReceive=>")
                if (list != null && list.size() > 0) {
                    for (index in 0 until list.size()) {
                        val message = list[index.toInt()]
                        val senderName = message.senderName()
                        val content = message.content()
                        val extension = message.extension()
                        LogUtils.d("${javaClass.simpleName}=QNIMUtils.onReceive=>senderName:$senderName,content:$content,extension:$extension")
                        if (extension != null && extension.isNotEmpty() && extension == "1"
                        ) {
                            //????????????????????????
                            runOnUiThread {
                                val currentUser = tv_live_liveUsers.text.toString().trim()
                                    .replace(getString(R.string.string_010), "")
                                    .toInt() + (11..14).random()
                                tv_live_liveUsers.text =
                                    "${currentUser}${getString(R.string.string_010)}"
                                isBottom = !rv_live.canScrollVertically(1)
                                addChatList(ChatListBean(senderName, content, true))
                                if (isBottom) {
                                    rv_live.smoothScrollToPosition(mChatLists.size - 1)
                                    tv_live_haveNews.visibility = View.GONE
                                } else {
                                    tv_live_haveNews.visibility = View.VISIBLE
                                }
                                mAdapter?.notifyDataSetChanged()
                            }
                        } else {
                            //??????????????????
                            runOnUiThread {
                                isBottom = !rv_live.canScrollVertically(1)
                                addDanmaku(content)
                                addChatList(
                                    ChatListBean(
                                        senderName,
                                        content,
                                        false
                                    )
                                )
                                if (isBottom) {
                                    rv_live.smoothScrollToPosition(mChatLists.size - 1)
                                    tv_live_haveNews.visibility = View.GONE
                                } else {
                                    tv_live_haveNews.visibility = View.VISIBLE
                                }
                                mAdapter?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }
        })
    }

    //6.??????
    private fun toSendMessage(message: String, isFirstJoin: Boolean = false) {
        isBottom = !rv_live.canScrollVertically(1)
        if (!isJoinRoom) {
            addChatList(ChatListBean(nickName, message, isFirstJoin))
            if (isBottom) {
                rv_live.smoothScrollToPosition(mChatLists.size - 1)
                tv_live_haveNews.visibility = View.GONE
            } else {
                tv_live_haveNews.visibility = View.VISIBLE
            }
            mAdapter?.notifyDataSetChanged()
        } else {
            if (OtherUtils.isGotOutOfLine(this, message) == 1) {
                ToastUtils.show("????????????${getString(R.string.string_057)}")
                return
            } else if (OtherUtils.isGotOutOfLine(this, message) == 2) {
                ToastUtils.show("????????????${getString(R.string.string_053)}")
                return
            }
            if (isLandscape) {
                //?????????????????????,??????????????????????????????
                if (!isFirstJoin) {
                    addDanmaku(message, true)
                }
                addChatList(ChatListBean(nickName, message, isFirstJoin))
                if (isBottom) {
                    rv_live.smoothScrollToPosition(mChatLists.size - 1)
                    tv_live_haveNews.visibility = View.GONE
                } else {
                    tv_live_haveNews.visibility = View.VISIBLE
                }
                mAdapter?.notifyDataSetChanged()
            } else {
                //???????????????,?????????????????????
                addChatList(ChatListBean(nickName, message, isFirstJoin))
                if (isBottom) {
                    rv_live.smoothScrollToPosition(mChatLists.size - 1)
                    tv_live_haveNews.visibility = View.GONE
                } else {
                    tv_live_haveNews.visibility = View.VISIBLE
                }
                mAdapter?.notifyDataSetChanged()
            }
            QNIMUtils.toSendMessage(userId, imGroupId, nickName, message, isFirstJoin)
        }
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        initOrientation()
    }

    /**
     * ??????????????????
     */
    private fun addChatList(chatListBean: ChatListBean) {
        mChatLists.add(chatListBean)
        if (mChatLists.size > 100) {
            mChatLists.removeAt(0)
        }
        mAdapter?.notifyDataSetChanged()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        //???????????????
        danmaku_live.enableDanmakuDrawingCache(true)
        danmaku_live.setCallback(object : DrawHandler.Callback {
            override fun prepared() {
                startTime = System.currentTimeMillis()
                danmaku_live.start()
            }

            override fun updateTimer(timer: DanmakuTimer?) {
            }

            override fun danmakuShown(danmaku: BaseDanmaku?) {
            }

            override fun drawingFinished() {
            }
        })
        danmakuContext = DanmakuContext.create()
        danmaku_live.prepare(parser, danmakuContext)
        //????????????
        setListeners()

        //????????????????????????
//        player_live.displayAspectRatio = PLVideoView.ASPECT_RATIO_ORIGIN//????????????
//        player_live.displayAspectRatio = PLVideoView.ASPECT_RATIO_FIT_PARENT//????????????
//        player_live.displayAspectRatio = PLVideoView.ASPECT_RATIO_PAVED_PARENT//????????????
        player_live.displayAspectRatio = PLVideoView.ASPECT_RATIO_16_9//16:9
//        player_live.displayAspectRatio = PLVideoView.ASPECT_RATIO_4_3//4.3

        val options = AVOptions()
        //????????????,???????????????,??????????????????
        options.setInteger(AVOptions.KEY_MEDIACODEC, AVOptions.MEDIA_CODEC_AUTO)
        //??????????????????????????????????????????,????????????????????????????????????
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1)
        //????????????,??????????????????????????????????????????????????????????????????
        options.setInteger(AVOptions.KEY_FAST_OPEN, 1)
        //??????????????????
        options.setInteger(AVOptions.KEY_OPEN_RETRY_TIMES, 5)
        //debug???????????????,????????????
        options.setInteger(AVOptions.KEY_LOG_LEVEL, 1)
        //?????????????????????http??????????????????
        options.setInteger(AVOptions.KEY_PREPARE_TIMEOUT, 10 * 1000)
        //????????????,????????????????????????????????????
        options.setInteger(AVOptions.KEY_LIVE_STREAMING, 1)
        player_live.setAVOptions(options)

        initOrientation()

        RxView.clicks(iv_live_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                onBackPressed()
            }

        RxView.clicks(tv_live_haveNews)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                rv_live.smoothScrollToPosition(mChatLists.size - 1)
                tv_live_haveNews.visibility = View.GONE
            }

        var mHeight = 0
        var mHeightLandscape = 0
        WeakReference(this).get()?.let {
            SoftKeyBoardChangeListener(it).init()
                .setHeightListener(object : SoftKeyBoardChangeListener.HeightListener {
                    override fun onHeightChanged(height: Int) {
                        if (!isLandscape) {
                            LogUtils.d("SoftKeyBoardChangeListener:portrait_height=$height")
                            if (height != mHeight) {
                                mHeight = height
                                if (height == 0) {
                                    isShowKeyBroad = false
                                    keyBroadHeight = 0
                                    ll_chat_edit.translationY = 0f
                                } else {
                                    isShowKeyBroad = true
                                    keyBroadHeight = height
                                    ll_chat_edit.translationY = -height.toFloat()
                                }
                            }
                        } else {
                            LogUtils.d("SoftKeyBoardChangeListener:landscape_height=$height")
                            if (isFirstLandscape) {
                                mHeightLandscape = height
                                isFirstLandscape = false
                            }
                            if (height != mHeight) {
                                mHeight = height
                                if (height == mHeightLandscape) {
                                    //?????????????????????,??????????????????????????????
                                    isShowKeyBroad = false
                                    keyBroadHeight = 0
                                    ll_live_bottom.translationY = 0f
                                    iv_live_pause.visibility = View.VISIBLE
                                    iv_live_fullscreen.visibility = View.VISIBLE
                                } else if (height > mHeightLandscape) {
                                    //????????????,?????????????????????
                                    isShowKeyBroad = true
                                    keyBroadHeight = height
                                    ll_live_bottom.translationY =
                                        -(height - mHeightLandscape).toFloat()
                                    mHandler.removeMessages(0)
                                    iv_live_pause.visibility = View.GONE
                                    iv_live_fullscreen.visibility = View.GONE
                                }
                            }
                        }
                    }
                })
        }
        RxView.clicks(iv_live_message_status)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //?????????????????????
                if (isShowDanmu) {
                    iv_live_message_status.setImageResource(R.mipmap.message_close)
                    iv_live_message_status_landscape.setImageResource(R.mipmap.message_close)
                } else {
                    iv_live_message_status.setImageResource(R.mipmap.message_open)
                    iv_live_message_status_landscape.setImageResource(R.mipmap.message_open)
                }
                isShowDanmu = !isShowDanmu
                danmaku_live.visibility = if (isShowDanmu) View.VISIBLE else View.GONE
            }
        RxView.clicks(iv_live_send_message)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val message = et_live.text.toString().trim()
                if (message.isNotEmpty()) {
                    toSendMessage(message, false)
                    et_live.setText("")
                    et_live_landscape.setText("")
                    OtherUtils.hindKeyboard(this, et_live)
                } else {
                    ToastUtils.show(getString(R.string.string_011))
                }
            }
        RxView.clicks(iv_live_message_status_landscape)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //?????????????????????
                if (isShowDanmu) {
                    iv_live_message_status.setImageResource(R.mipmap.message_close)
                    iv_live_message_status_landscape.setImageResource(R.mipmap.message_close)
                } else {
                    iv_live_message_status.setImageResource(R.mipmap.message_open)
                    iv_live_message_status_landscape.setImageResource(R.mipmap.message_open)
                }
                isShowDanmu = !isShowDanmu
                danmaku_live.visibility = if (isShowDanmu) View.VISIBLE else View.GONE
            }
        RxView.clicks(iv_live_send_message_landscape)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val message = et_live_landscape.text.toString().trim()
                if (message.isNotEmpty()) {
                    toSendMessage(message, false)
                    et_live.setText("")
                    et_live_landscape.setText("")
                    OtherUtils.hindKeyboard(this, et_live)

                    isShow = false
                    view_live_top_landscape.visibility = View.GONE
                    LiveTranslateUtils.topExit(ll_live_top)
                    LiveTranslateUtils.bottomExit(ll_live_bottom)
                    showOrHideStatusBar(false)
                } else {
                    ToastUtils.show(getString(R.string.string_011))
                }
            }
        RxTextView.textChanges(et_live)
            .skipInitialValue()
            .subscribe {
                if (et_live.hasFocus())
                    et_live_landscape.setText(it)
            }
        RxTextView.textChanges(et_live_landscape)
            .skipInitialValue()
            .subscribe {
                if (et_live_landscape.hasFocus())
                    et_live.setText(it)
            }
        et_live.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                if (MyApp.getInstance().isHaveToken()) {
                    val message = et_live.text.toString().trim()
                    if (message.isNotEmpty()) {
                        toSendMessage(message, false)
                        et_live.setText("")
                        et_live_landscape.setText("")
                    }
                } else {
                    LoginUtils.toQuickLogin(this)
                }
                OtherUtils.hindKeyboard(this, et_live)
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        et_live_landscape.imeOptions = EditorInfo.IME_FLAG_NO_EXTRACT_UI
    }

    /**
     * ?????????????????????????????????
     */
    @SuppressLint("CheckResult")
    private fun initOrientation() {
        if (resources.configuration.orientation == Configuration.ORIENTATION_PORTRAIT) {
            //??????
            isLandscape = false
            isShow = false
            Log.d(javaClass.simpleName, "??????")
            if (mChatLists.size > 0) {
                rv_live.smoothScrollToPosition(mChatLists.size - 1)
            }
            view_live_top.visibility = View.VISIBLE
            view_live_top_landscape.visibility = View.GONE
            ll_live_enter_landscape_landscape.visibility = View.GONE
            view_live_enter_landscape_landscape.visibility = View.VISIBLE
            iv_live_enter_game_landscape.visibility = View.GONE
            StatusBarUtils.setTransparent(this)
            StatusBarUtils.setTextDark(this, false)
            mHandler.removeMessages(0)
            LiveTranslateUtils.topExit(ll_live_top)
            LiveTranslateUtils.bottomExit(ll_live_bottom)
            danmaku_live.visibility = View.GONE
            RxView.clicks(player_live_view)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (!isLandscape) {
                        if (isShowKeyBroad) {
                            OtherUtils.hindKeyboard(this, et_live_landscape)
                        }
                        if (isShow) {
                            LiveTranslateUtils.topExit(ll_live_top)
                            LiveTranslateUtils.bottomExit(ll_live_bottom)
                            mHandler.removeMessages(0)
                        } else {
                            ll_live_top.visibility = View.VISIBLE
                            ll_live_bottom.visibility = View.VISIBLE
                            LiveTranslateUtils.topEnter(ll_live_top)
                            LiveTranslateUtils.bottomEnter(ll_live_bottom)
                            mHandler.sendEmptyMessageDelayed(0, 3000)
                        }
                        isShow = !isShow
                    }
                }
            RxView.clicks(iv_live_pause)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isPause) {
                        player_live.start()
                        iv_live_pause.setImageResource(R.drawable.video_pause)
                    } else {
                        player_live.pause()
                        iv_live_pause.setImageResource(R.drawable.video_play)
                    }
                    isPause = !isPause
                }
            val layoutParams = fl_live_view.layoutParams
            layoutParams.width = mScreenWidth
            layoutParams.height = (mScreenWidth.toFloat() / 16 * 9).toInt()
            val layoutParams1 = rl_live_view.layoutParams
            layoutParams1.height = (mScreenWidth.toFloat() / 16 * 9).toInt()

            val layoutParams2 = rl_live_bottom.layoutParams
            layoutParams2.width = mScreenWidth
            RxView.clicks(iv_live_fullscreen)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    iv_live_fullscreen.setImageResource(R.mipmap.fullscreen_exit)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
                }
        } else if (resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            //??????
            isLandscape = true
            isShow = false
            Log.d(javaClass.simpleName, "??????")
            showOrHideStatusBar(false)
            view_live_top.visibility = View.GONE
            ll_live_enter_landscape_landscape.visibility = View.VISIBLE
            view_live_enter_landscape_landscape.visibility = View.GONE
            iv_live_enter_game_landscape.visibility = View.VISIBLE
            mHandler.removeMessages(0)
            LiveTranslateUtils.topExit(ll_live_top)
            LiveTranslateUtils.bottomExit(ll_live_bottom)
            danmaku_live.visibility = if (isShowDanmu) View.VISIBLE else View.GONE
            RxView.clicks(player_live_view)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isLandscape) {
                        if (isShow) {
                            showOrHideStatusBar(false)
                            view_live_top_landscape.visibility = View.GONE
                            LiveTranslateUtils.topExit(ll_live_top)
                            LiveTranslateUtils.bottomExit(ll_live_bottom)
                            mHandler.removeMessages(0)
                        } else {
                            StatusBarUtils.setTransparent(this)
                            StatusBarUtils.setTextDark(this, false)
                            ll_live_top.visibility = View.VISIBLE
                            ll_live_bottom.visibility = View.VISIBLE
                            LiveTranslateUtils.topEnter(ll_live_top)
                            LiveTranslateUtils.bottomEnter(ll_live_bottom)
                            view_live_top_landscape.visibility = View.VISIBLE
                            mHandler.sendEmptyMessageDelayed(0, 3000)
                        }
                        isShow = !isShow
                    }
                }
            RxView.clicks(iv_live_pause)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    if (isPause) {
                        player_live.start()
                        iv_live_pause.setImageResource(R.drawable.video_pause)
                    } else {
                        player_live.pause()
                        iv_live_pause.setImageResource(R.drawable.video_play)
                    }
                    isPause = !isPause
                }
            val layoutParams = fl_live_view.layoutParams
            layoutParams.height = (360 * (mScreenWidth.toFloat() / 360)).toInt()
            layoutParams.width = (360 * (mScreenWidth.toFloat() / 360) / 9 * 16).toInt()
            val layoutParams1 = rl_live_view.layoutParams
            layoutParams1.height = (360 * (mScreenWidth.toFloat() / 360)).toInt()
            val layoutParams2 = rl_live_bottom.layoutParams
            layoutParams2.width = (360 * (mScreenWidth.toFloat() / 360) / 9 * 16).toInt()
            RxView.clicks(iv_live_fullscreen)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    iv_live_fullscreen.setImageResource(R.mipmap.fullscreen_enter)
                    requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
                }
        }
    }

    private val mHandler = @SuppressLint("HandlerLeak")
    object : Handler() {
        override fun handleMessage(msg: Message) {
            super.handleMessage(msg)
            view_live_top_landscape.visibility = View.GONE
            LiveTranslateUtils.topExit(ll_live_top)
            LiveTranslateUtils.bottomExit(ll_live_bottom)
            if (isLandscape) {
                showOrHideStatusBar(false)
            }
            isShow = false
        }
    }

    private var justCanRequestTimes = 3
    private fun setListeners() {
        player_live.setOnPreparedListener { preparedTime ->
//            Log.d(javaClass.simpleName, "??????????????????,preparedTime=$preparedTime")
            player_live.start()
        }

        player_live.setOnInfoListener { what, extra ->
//            Log.d(javaClass.simpleName, "??????????????????,what=$what,extra=$extra")
        }

        player_live.setOnCompletionListener {
//            Log.d(javaClass.simpleName, "????????????????????????")
        }

        player_live.setOnVideoSizeChangedListener { width, height ->
//            Log.d(javaClass.simpleName, "????????????????????????,width=$width,height=$height")
        }

        player_live.setOnErrorListener { errorCode ->
//            Log.d(javaClass.simpleName, "??????????????????,errorCode=$errorCode")
            if (errorCode == ERROR_CODE_IO_ERROR) {
                //?????????
                if (!isNetworkAvailable()) {
                    //?????????????????????
                    ToastUtils.show(getString(R.string.string_012))
                } else {
                    runOnUiThread {
                        if (justCanRequestTimes > 0) {
                            justCanRequestTimes--
                            getInfo(true)
                        }
                    }
                }
            }
            return@setOnErrorListener false
        }
    }

    private fun isNetworkAvailable(): Boolean {
        val cm = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        val netInfo = cm.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }

    override fun onBackPressed() {
        if (isLandscape) {
            iv_live_fullscreen.setImageResource(R.mipmap.fullscreen_enter)
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT
        } else {
            super.onBackPressed()
        }
    }

    /**
     * ??????????????????
     */
    @SuppressLint("CheckResult")
    private fun getInfo(isToCheckLive: Boolean = false) {
        if (!isToCheckLive) {
            DialogUtils.showBeautifulDialog(this)
        }
        val liveInfo = RetrofitUtils.builder().liveInfo(liveId)
        liveInfoObservable = liveInfo.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (!isToCheckLive) {
                    DialogUtils.dismissLoading()
                }
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (!isToCheckLive) {
                                toSetView(it.data)
                            } else {
                                if (it.data.status == 2) {
                                    //???????????????
                                    ToastUtils.show(getString(R.string.string_013))
                                }
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            if (!isToCheckLive) {
                                ToastUtils.show(it.msg)
                            } else {
                                if (getString(R.string.string_014).contains(it.msg)) {
                                    //???????????????
                                    ToastUtils.show(getString(R.string.string_013))
                                }
                            }
                        }
                    }
                } else {
                    if (!isToCheckLive) {
                        ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                    }
                }
            }, {
                if (!isToCheckLive) {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
                }
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }

    /**
     * ??????????????????
     */
    @SuppressLint("SetTextI18n", "CheckResult")
    private fun toSetView(data: LiveInfoBean.Data) {
        tv_live_name.text = data.anchor_name
        val random = (11..14).random()
        tv_live_liveUsers.text =
            "${(data.online_user + 2) * random}${getString(R.string.string_010)}"
        tv_live_tag.text = data.tag
        tv_live_homeId.text = data.live_id.toString()
        tv_live_tip.text = tips.replace("XXX", data.anchor_name)

        iv_live_share.visibility = View.VISIBLE
        RxView.clicks(iv_live_share)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                //??????
                BottomDialog.shareLive(
                    this,
                    data.play_url,
                    data.intro,
                    data.anchor_name,
                    data.cover
                )
            }

        RxView.clicks(tv_live_download)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (data.game_system == 1) {
                    MobclickAgent.onEvent(
                        this,
                        "live",
                        "live_to_pc"
                    )
                } else {
                    MobclickAgent.onEvent(
                        this,
                        "live",
                        "live_to_phone"
                    )
                }
                val intent = Intent(this, GameDetailActivity::class.java)
                intent.putExtra(GameDetailActivity.GAME_ID, data.game_id)
                intent.putExtra(GameDetailActivity.IS_FROM_LIVE, true)
                intent.putExtra(GameDetailActivity.GAME_COVER, data.game_cover)
                intent.putExtra(GameDetailActivity.GAME_BADGE, data.game_badge)
                startActivity(intent)
            }

        RxView.clicks(iv_live_enter_game_landscape)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent(this, GameDetailActivity::class.java)
                intent.putExtra(GameDetailActivity.GAME_ID, data.game_id)
                intent.putExtra(GameDetailActivity.IS_FROM_LIVE, true)
                intent.putExtra(GameDetailActivity.GAME_COVER, data.game_cover)
                intent.putExtra(GameDetailActivity.GAME_BADGE, data.game_badge)
                startActivity(intent)
            }

        //????????????
        player_live.setVideoPath(data.play_url)
        isPlayerPrepare = true
    }

    /**
     * ????????????
     */
    private fun addDanmaku(
        content: String,
        withBorder: Boolean = false,
        textColor: Int = resources.getColor(R.color.white_FFFFFF),
        time: Long = 0L
    ) {
        LogUtils.d("${javaClass.simpleName}=content:$content,time:$time")
        val screenWidth = Math.min(
            PhoneInfoUtils.getWidth(this).toFloat(),
            PhoneInfoUtils.getHeight(this).toFloat()
        )
        var danmaku = danmakuContext?.mDanmakuFactory?.createDanmaku(BaseDanmaku.TYPE_SCROLL_RL)
        danmaku?.text = content
        danmaku?.padding = (3 * (screenWidth / 360)).toInt()
        danmaku?.textSize = 14 * (screenWidth / 360)
        danmaku?.textColor = textColor
        danmaku?.time = System.currentTimeMillis() - startTime
        if (withBorder) {
            danmaku?.borderColor = resources.getColor(R.color.green_2EC8AC)
        }
        danmaku_live.addDanmaku(danmaku)
    }

    override fun destroy() {
        val currentTimeMillis = System.currentTimeMillis()
        if (currentTimeMillis - startTime > 10 * 60 * 1000) {
            MobclickAgent.onEvent(
                this,
                "live",
                "live_10_num"
            )
        }
        EventBus.getDefault().unregister(this)
        danmaku_live.clear()

        liveInfoObservable?.dispose()
        liveInfoObservable = null
        gameDownloadStartObservable?.dispose()
        gameDownloadStartObservable = null
        gameDownloadCompleteObservable?.dispose()
        gameDownloadCompleteObservable = null
        gameInstallStartObservable?.dispose()
        gameInstallStartObservable = null

        if (isPlayerPrepare) {
            player_live.stopPlayback()
        }

        QNIMUtils.toLeaveChatRoom(null)
    }

    @Subscribe
    fun onUserStatusChange(userInfoChange: UserInfoChange) {
        if (!userInfoChange.name.isNullOrEmpty()) {
            if (isSignIn) {
                isSignIn = false
                QNIMUtils.toSignOut(false)
            }
            QNIMUtils.toLeaveChatRoom(null)
            initQNIM()
        }
    }

    /**
     * ?????????????????????
     */
    private fun showOrHideStatusBar(isShowStatusBar: Boolean) {
        if (Build.VERSION.SDK_INT in 12..18) {
            val decorView = window.decorView
            decorView.systemUiVisibility = if (isShowStatusBar) View.VISIBLE else View.GONE
        } else if (Build.VERSION.SDK_INT >= 19) {
            val decorView = window.decorView
            val uiOptions = (View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    or View.SYSTEM_UI_FLAG_FULLSCREEN
                    or View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION)
            decorView.systemUiVisibility = if (isShowStatusBar) View.VISIBLE else uiOptions
        }
    }

    override fun onResume() {
        super.onResume()
        MobclickAgent.onResume(this)
        if (isPlayerPrepare) {
            player_live.start()
        }
    }

    override fun onPause() {
        super.onPause()
        MobclickAgent.onPause(this)
        if (isPlayerPrepare) {
            player_live.pause()
        }
    }
}