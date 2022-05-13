package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.app.Dialog
import android.util.DisplayMetrics
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.item_select_more_music.view.*
import kotlinx.android.synthetic.main.layout_select_more_music.*
import java.util.concurrent.TimeUnit

@SuppressLint("StaticFieldLeak")
object SelectMoreMusicUtil {

    private var mDialog: Dialog? = null

    private var mAdapter: BaseAdapterWithPosition<MusicListBean.Data.MusicList>? = null
    private var musicListObservable: Disposable? = null
    private var musicList = mutableListOf<MusicListBean.Data.MusicList>()
    private var currentPage = 1
    private var countPage = 1

    private var searchList = mutableListOf<MusicListBean.Data.MusicList>()
    private var mData = mutableListOf<MusicListBean.Data.MusicList>()

    private var favMusicObservable: Disposable? = null

    //当前选择的配乐
    private var currentMusic: String? = null

    /**
     * 取消选择弹框
     */
    fun dismiss() {
        try {
            if (mDialog != null && mDialog?.isShowing == true) {
                mDialog?.dismiss()
            }
        } catch (e: Exception) {

        } finally {
//            MusicMediaPlayerUtil.release()
            mDialog = null
        }
    }

    interface SelectMoreMusicCallBack {
        fun finish()
    }

    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    fun showMoreMusic(context: AppCompatActivity, callback: SelectMoreMusicCallBack) {
        musicList.clear()
        currentPage = 1
        countPage = 1
        searchList.clear()
        mData.clear()
        currentMusic = null

        mDialog = Dialog(context, R.style.BottomDialog)
        val root = LayoutInflater.from(context)
            .inflate(R.layout.layout_select_more_music, null) as LinearLayout
        mDialog?.setContentView(root)

        RxView.clicks(mDialog?.iv_selectMoreMusic_cancel!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                dismiss()
            }

        initRecycleView(context,callback)
        getMusicList(context)

        RxTextView.textChanges(mDialog?.et_selectMoreMusic_search!!)
            .skipInitialValue()
            .subscribe {
                if (it.isEmpty()) {
                    mData.clear()
                    mData.addAll(musicList)
                    mAdapter?.notifyDataSetChanged()
                } else {
                    toGetSearchMusic(context, it.toString())
                }
            }

        mDialog?.setOnDismissListener {
            callback.finish()
        }

        //确定大小位置
        val displayMetrics = DisplayMetrics()
        context.windowManager.defaultDisplay.getMetrics(displayMetrics)
        val attributes = mDialog?.window?.attributes
        attributes?.width = displayMetrics.widthPixels
        attributes?.height = displayMetrics.heightPixels
        attributes?.gravity = Gravity.BOTTOM
        mDialog?.window?.attributes = attributes
        mDialog?.show()
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun toGetSearchMusic(context: AppCompatActivity, search: String) {
        val music = RetrofitUtils.builder().musicList(search, 1)
        musicListObservable = music
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mDialog?.refresh_selectMoreMusic?.finishRefresh()
                mDialog?.refresh_selectMoreMusic?.finishLoadMore()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            searchList.clear()
                            searchList.addAll(it.data.list)
                            mData.clear()
                            mData.addAll(searchList)
                            mAdapter?.notifyDataSetChanged()
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(context)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(context.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                mDialog?.refresh_selectMoreMusic?.finishRefresh()
                mDialog?.refresh_selectMoreMusic?.finishLoadMore()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(context, it))
            })
    }

    @SuppressLint("NotifyDataSetChanged")
    private fun getMusicList(context: AppCompatActivity) {
        val music = RetrofitUtils.builder().musicList("", currentPage)
        musicListObservable = music.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mDialog?.refresh_selectMoreMusic?.finishRefresh()
                mDialog?.refresh_selectMoreMusic?.finishLoadMore()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (it.data.list.isNotEmpty()) {
                                val count = it.data.paging.count
                                val limit = it.data.paging.limit
                                countPage = count / limit
                                if (count % limit != 0) {
                                    countPage++
                                }
                                if (countPage == 0) {
                                    countPage = 1
                                }
                                musicList.addAll(it.data.list)
                                mData.clear()
                                mData.addAll(musicList)
                                mAdapter?.notifyDataSetChanged()
                            }
                        }
                        -1 -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(context)
                        }
                        else -> {
                            DialogUtils.dismissLoading()
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    DialogUtils.dismissLoading()
                    ToastUtils.show(context.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                mDialog?.refresh_selectMoreMusic?.finishRefresh()
                mDialog?.refresh_selectMoreMusic?.finishLoadMore()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(context, it))
            })
    }

    /**
     * 初始化recycleView
     */
    @SuppressLint("CheckResult", "NotifyDataSetChanged")
    private fun initRecycleView(context: AppCompatActivity, callback: SelectMoreMusicCallBack) {
        mDialog?.refresh_selectMoreMusic?.setEnableRefresh(true)
        mDialog?.refresh_selectMoreMusic?.setEnableLoadMore(true)
        mDialog?.refresh_selectMoreMusic?.setEnableLoadMoreWhenContentNotFull(false)
        mDialog?.refresh_selectMoreMusic?.setRefreshHeader(MaterialHeader(context))
        mDialog?.refresh_selectMoreMusic?.setRefreshFooter(ClassicsFooter(context))

        mDialog?.refresh_selectMoreMusic?.setOnRefreshLoadMoreListener(object :
            OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                musicList.clear()
                mData.clear()
                getMusicList(context)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getMusicList(context)
                } else {
                    mDialog?.refresh_selectMoreMusic?.finishLoadMoreWithNoMoreData()
                }
            }
        })

        mAdapter = BaseAdapterWithPosition.Builder<MusicListBean.Data.MusicList>()
            .setLayoutId(R.layout.item_select_more_music)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                Glide.with(context)
                    .load(itemData.cover)
                    .into(itemView.iv_item_selectMoreMusic_cover)
                itemView.tv_item_selectMoreMusic_name.text = itemData.name
                itemView.tv_item_selectMoreMusic_author.text = itemData.author
                itemView.tv_item_selectMoreMusic_duration.text =
                    TimeFormat.format(itemData.duration.toLong())
                if (itemData.is_fav == 1) {
                    itemView.iv_item_selectMoreMusic_fav.setImageResource(R.mipmap.fav_en)
                } else {
                    itemView.iv_item_selectMoreMusic_fav.setImageResource(R.mipmap.fav_un)
                }
                RxView.clicks(itemView.iv_item_selectMoreMusic_fav)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toFavMusic(context, itemData, itemView)
                    }

                if (currentMusic == itemData.path) {
                    itemView.tv_item_selectMoreMusic_use.visibility = View.VISIBLE
                } else {
                    itemView.tv_item_selectMoreMusic_use.visibility = View.GONE
                }

                if (itemData == Argument.getMusicData()) {
                    itemView.tv_item_selectMoreMusic_use.visibility = View.VISIBLE
                    itemView.tv_item_selectMoreMusic_use.text = "√ 已使用"
                }

                RxView.clicks(itemView.tv_item_selectMoreMusic_use)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        Argument.setMusicData(itemData)
                        mDialog?.dismiss()
                    }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val mMediaPlayer = MusicMediaPlayerUtil.initMediaPlayer()
                        mMediaPlayer?.setDataSource(itemData.path)
                        mMediaPlayer?.prepareAsync()
                        itemView.pb_item_selectMoreMusic.visibility = View.VISIBLE
                        mMediaPlayer?.setOnPreparedListener {
                            itemView.pb_item_selectMoreMusic.visibility = View.GONE
                            mMediaPlayer.start()
                            itemView.tv_item_selectMoreMusic_use.visibility = View.VISIBLE
                            currentMusic = itemData.path
                            mAdapter?.notifyDataSetChanged()
                        }
                    }
            }
            .create()
        mDialog?.rv_selectMoreMusic?.adapter = mAdapter
        mDialog?.rv_selectMoreMusic?.layoutManager = SafeLinearLayoutManager(context)
    }

    /**
     * 收藏/取消收藏配乐
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun toFavMusic(
        context: AppCompatActivity,
        itemData: MusicListBean.Data.MusicList,
        itemView: View
    ) {
        val favMusic = RetrofitUtils.builder().favMusic(itemData.music_id, itemData.is_fav)
        favMusicObservable = favMusic.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                mDialog?.refresh_selectMoreMusic?.finishRefresh()
                mDialog?.refresh_selectMoreMusic?.finishLoadMore()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            var isFav = 0
                            if (itemData.is_fav == 1) {
                                isFav = 0
                                itemView.iv_item_selectMoreMusic_fav.setImageResource(R.mipmap.fav_un)
                                ToastUtils.show("取消收藏")
                            } else {
                                isFav = 1
                                itemView.iv_item_selectMoreMusic_fav.setImageResource(R.mipmap.fav_en)
                                ToastUtils.show("收藏成功")
                            }
                            for (music in mData) {
                                if (music == itemData) {
                                    music.is_fav = isFav
                                    mAdapter?.notifyDataSetChanged()
                                }
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(context)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(context.getString(R.string.network_fail_to_responseDate))
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(context, it))
            })
    }

}