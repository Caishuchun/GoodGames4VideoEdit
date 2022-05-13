package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.view.View
import androidx.recyclerview.widget.OrientationHelper
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.UserHomeBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_user_detail.*
import kotlinx.android.synthetic.main.layout_game_item.view.*
import java.util.concurrent.TimeUnit

class UserDetailActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: UserDetailActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val VIDEO_ID = "videoId"
    }

    private var videoId = -1
    private var isFocus = false

    private var userHomeObservable: Disposable? = null
    private var followObservable: Disposable? = null
    private var mData = mutableListOf<UserHomeBean.Data.Video>()
    private var mAdapter: BaseAdapterWithPosition<UserHomeBean.Data.Video>? = null

    override fun getLayoutId() = R.layout.activity_user_detail

    override fun doSomething() {
        instance = this
        videoId = intent.getIntExtra(VIDEO_ID, -1)

        initView()
        getInfo()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_userDetail_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(tv_userDetail_focus)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe {
                if (MyApp.getInstance().isHaveToken()) {
                    toFollow(isFocus)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }
        tv_userDetail_name.text = "$videoId"

        mAdapter = BaseAdapterWithPosition.Builder<UserHomeBean.Data.Video>()
            .setLayoutId(R.layout.layout_game_item)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                itemView.ll_item_game_normal?.visibility = View.GONE
                itemView.ll_item_game_live?.visibility = View.GONE
                itemView.rl_item_game_mv?.visibility = View.VISIBLE
                itemView.tv_item_game_mv_des.text = itemData.video_name

                val layoutParams = itemView.civ_item_game_mv_icon.layoutParams
                val videoCoverHeight = itemData.video_cover_height
                val videoCoverWidth = itemData.video_cover_width
                val screenWidth = PhoneInfoUtils.getWidth(this)
                layoutParams.width = (120f / 360 * screenWidth).toInt()
                val realHeight =
                    (110f / 360 * screenWidth / videoCoverWidth * videoCoverHeight).toInt()
                val maxHeight = (190f / 360 * screenWidth).toInt()
                layoutParams.height = Math.min(realHeight, maxHeight)
                itemView.civ_item_game_mv_icon.layoutParams = layoutParams

                val rootLayoutParams = itemView.rl_item_game_mv.layoutParams
                rootLayoutParams.width = (120f / 360 * screenWidth).toInt()
                rootLayoutParams.height =
                    if (realHeight < maxHeight * 3 / 4) maxHeight * 9 / 10
                    else maxHeight
                itemView.rl_item_game_mv.layoutParams = rootLayoutParams

                Glide.with(this)
                    .load(itemData.video_cover)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.civ_item_game_mv_icon)

                itemView.civ_item_game_mv_icon.isNeedRadius(realHeight >= maxHeight)

                itemView.tv_item_game_mv_share.text = "${itemData.total_share}"
                itemView.tv_item_game_mv_msg.text = "${itemData.total_comment}"
                itemView.tv_item_game_mv_good.text = "${itemData.total_like}"
                itemView.tv_item_game_mv_look.text = "${itemData.total_view}"

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(this, VideoActivity::class.java)
                        intent.putExtra(VideoActivity.VIDEO_TYPE, 1)
                        intent.putExtra(
                            VideoActivity.VIDEO_ID,
                            itemData.video_id
                        )
                        intent.putExtra(VideoActivity.VIDEO_COVER, itemData.video_cover)
                        intent.putExtra(VideoActivity.VIDEO_COVER_WIDTH, itemData.video_cover_width)
                        intent.putExtra(
                            VideoActivity.VIDEO_COVER_HEIGHT,
                            itemData.video_cover_height
                        )
                        intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
                        startActivity(intent)
                    }
            }
            .create()
        rv_userDetail_video.adapter = mAdapter
        rv_userDetail_video.layoutManager =
            SafeStaggeredGridLayoutManager(3, OrientationHelper.VERTICAL)
    }

    /**
     * 关注或取消关注
     */
    private fun toFollow(focus: Boolean) {
        val isCancel = if (focus) 0 else 1
        val followUser = RetrofitUtils.builder().followUser(videoId, isCancel)
        followObservable = followUser.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val data = UserInfoBean.getData()!!
                            var userFollow = data.user_follow!!
                            if (isFocus) {
                                ToastUtils.show("取消关注")
                                tv_userDetail_focus.text = "＋ 关注"
                                userFollow--
                                tv_userDetail_focus.setBackgroundResource(R.drawable.bg_focus_un)
                            } else {
                                ToastUtils.show("关注成功")
                                tv_userDetail_focus.text = "√ 已关注"
                                userFollow++
                                tv_userDetail_focus.setBackgroundResource(R.drawable.bg_focus_on)
                            }
                            data.user_follow = userFollow
                            UserInfoBean.setData(data)
                            isFocus = !isFocus
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 获取数据
     */
    private fun getInfo() {
        val userHome = RetrofitUtils.builder().userHome(videoId)
        userHomeObservable = userHome.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            toSetInfo(it.data)
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 设置获取到的信息
     */
    @SuppressLint("SetTextI18n", "NotifyDataSetChanged", "CheckResult")
    private fun toSetInfo(data: UserHomeBean.Data) {
        Glide.with(this)
            .load(data.user_avatar)
            .placeholder(R.mipmap.icon)
            .into(riv_userDetail_head)
        tv_userDetail_name.text = if (data.user_name.isEmpty()) "好服多多" else data.user_name
        tv_userDetail_des.text = data.user_desc
        tv_userDetail_likes.text = "${data.user_video_like} 获赞"
        tv_userDetail_focuses.text = "${data.user_follow} 关注"
        tv_userDetail_fans.text = "${data.user_fans} 粉丝"

        RxView.clicks(tv_userDetail_focuses)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe { itt ->
                if (MyApp.getInstance().isHaveToken()) {
                    val intent =
                        Intent(this, FansAndFollowListActivity::class.java)
                    intent.putExtra(FansAndFollowListActivity.TYPE, 0)
                    intent.putExtra(
                        FansAndFollowListActivity.USER_ID,
                        data.user_id
                    )
                    startActivity(intent)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        RxView.clicks(tv_userDetail_fans)
            .throttleFirst(
                if (MyApp.getInstance().isHaveToken()) 200 else 2000,
                TimeUnit.MILLISECONDS
            )
            .subscribe { itt ->
                if (MyApp.getInstance().isHaveToken()) {
                    val intent = Intent(
                        this,
                        FansAndFollowListActivity::class.java
                    )
                    intent.putExtra(FansAndFollowListActivity.TYPE, 1)
                    intent.putExtra(
                        FansAndFollowListActivity.USER_ID,
                        data.user_id
                    )
                    startActivity(intent)
                } else {
                    LoginUtils.toQuickLogin(this)
                }
            }

        isFocus = data.is_follow == 1
        if (isFocus) {
            tv_userDetail_focus.text = "√ 已关注"
            tv_userDetail_focus.setBackgroundResource(R.drawable.bg_focus_on)
        } else {
            tv_userDetail_focus.text = "＋ 关注"
            tv_userDetail_focus.setBackgroundResource(R.drawable.bg_focus_un)
        }

        val empty = data.video_list.isEmpty()
        if (empty) {
            rv_userDetail_video.visibility = View.GONE
            tv_userDetail_noVideo.visibility = View.VISIBLE
        } else {
            rv_userDetail_video.visibility = View.VISIBLE
            tv_userDetail_noVideo.visibility = View.GONE
            mData.clear()
            mData.addAll(data.video_list)
            mAdapter?.notifyDataSetChanged()
        }
    }

    override fun destroy() {
        userHomeObservable?.dispose()
        userHomeObservable = null

        followObservable?.dispose()
        followObservable = null
    }
}