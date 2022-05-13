package com.fortune.zg.newVideo

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.RecyclerView
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.MyJzvd4Mv
import com.google.gson.Gson
import com.scwang.smart.refresh.footer.ClassicsFooter
import com.scwang.smart.refresh.header.MaterialHeader
import com.scwang.smart.refresh.layout.api.RefreshLayout
import com.scwang.smart.refresh.layout.listener.OnRefreshLoadMoreListener
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_video_lists.view.*

private const val TYPE = "type"

class VideoListsFragment : Fragment() {

    // 1_all,3_pc,2_phone
    private var videoType: Int = 1
    private var mView: View? = null
    private var currentPage = 1
    private var countPage = 1
    private var getVideoListsObservable: Disposable? = null
    private var mData = mutableListOf<NewVideoListsBean.Data.VideoBean>()
    private var mAdapter: NewVideoAdapter? = null
    private var currentJzvd: MyJzvd4Mv? = null
    private var currentPosition = 0 //当前位置
    private var isHttp = false

    companion object {
        @JvmStatic
        fun newInstance(videoType: Int) =
            VideoListsFragment().apply {
                arguments = Bundle().apply {
                    putInt(TYPE, videoType)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            videoType = it.getInt(TYPE)
        }
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            mAdapter?.onPause()
        } else {
            currentJzvd?.startVideo()
        }
    }

    /**
     * 退出的时候随机跳转到一个视频
     */
    fun toJump() {
        if (mData.size > 0 && mView != null) {
            val random = if (currentPosition + 1 > mData.size - 1) {
                (0 until mData.size).random()
            } else {
                (currentPosition + 1 until mData.size).random()
            }
            mView?.rv_videoLists?.scrollToPosition(random)
        }
    }

    /**
     * 获取当前播放器
     */
    fun getCurrentJzvd() = currentJzvd

    /**
     * 刷新
     */
    fun refresh() {
        mView?.rv_videoLists?.scrollToPosition(0)
        currentPage = 1
        getInfo(true)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_video_lists, container, false)
        initView()
        getInfo(true)
        return mView
    }

    private fun initView() {
        mView?.refresh_videoLists?.setEnableRefresh(true)
        mView?.refresh_videoLists?.setEnableLoadMore(true)
        mView?.refresh_videoLists?.setEnableLoadMoreWhenContentNotFull(false)
        mView?.refresh_videoLists?.setRefreshHeader(MaterialHeader(activity))
        mView?.refresh_videoLists?.setRefreshFooter(ClassicsFooter(activity))
        mView?.refresh_videoLists?.setOnRefreshLoadMoreListener(object : OnRefreshLoadMoreListener {
            override fun onRefresh(refreshLayout: RefreshLayout) {
                currentPage = 1
                getInfo(false)
            }

            override fun onLoadMore(refreshLayout: RefreshLayout) {
                if (currentPage < countPage) {
                    currentPage++
                    getInfo(false)
                } else {
                    mView?.refresh_videoLists?.finishLoadMoreWithNoMoreData()
                }
            }
        })
        val pageLayoutManager =
            NewViewPagerLayoutManager(activity as MainActivityV5, OrientationHelper.VERTICAL, false)
        pageLayoutManager.setOnViewPagerListener(object : OnNewViewPagerListener {
            override fun onInitComplete(itemView: View, position: Int) {
                currentJzvd = mAdapter?.ViewHolder(itemView)?.videoView
                mAdapter?.setVideoInfo(mData[position], itemView, position)
            }

            override fun onPageRelease(itemView: View, isNext: Boolean, position: Int) {
                val holder = mAdapter?.ViewHolder(itemView)!!
                OtherUtils.hindKeyboard(activity as MainActivityV5, holder.etDanmuContent)
                mAdapter?.releaseView(itemView)
            }

            override fun onPageSelected(itemView: View, position: Int, isBottom: Boolean) {
                currentJzvd = mAdapter?.ViewHolder(itemView)?.videoView
                mAdapter?.setVideoInfo(mData[position], itemView, position)
            }
        })

        mView?.rv_videoLists?.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
            }

            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                LogUtils.d("+++++++mData.size=>$newState")
                //滑动的时候
                if (newState == 1) {
                    val layoutManager = recyclerView.layoutManager as LinearLayoutManager
                    currentPosition = layoutManager.findFirstVisibleItemPosition()
                    LogUtils.d("+++++++mData.size=${mData.size},currentPosition=$currentPosition")
                    if (mData.size - currentPosition < 5 && mData.size > 10) {
                        currentPage++
                        getInfo(false)
                    }
                }
            }
        })

        mAdapter = NewVideoAdapter(activity as MainActivityV5, mData, videoType)
        mView?.rv_videoLists?.layoutManager = pageLayoutManager
        mView?.rv_videoLists?.adapter = mAdapter
        //Recycle绘制结束之后再获取当前控件
        mView?.rv_videoLists?.viewTreeObserver?.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                //操作结束移除
                mView?.rv_videoLists?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
            }
        })
    }

    /**
     * 获取数据
     */
    @SuppressLint("NotifyDataSetChanged")
    private fun getInfo(needLoading: Boolean = true) {
        if (isHttp) {
            return
        } else {
            isHttp = true
        }
        if (needLoading) {
            DialogUtils.showBeautifulDialog(activity as MainActivityV5)
        }
        val videoLists = RetrofitUtils.builder().getVideoLists(videoType, currentPage)
        getVideoListsObservable = videoLists.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                isHttp = false
                if (needLoading) {
                    DialogUtils.dismissLoading()
                }
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                mView?.refresh_videoLists?.finishRefresh()
                mView?.refresh_videoLists?.finishLoadMore()
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            if (currentPage == 1) {
                                mData.clear()
                                mAdapter?.notifyDataSetChanged()
                            }
                            val count = it.data?.paging?.count!!
                            val limit = it.data.paging.limit!!
                            countPage = count / limit
                            if (count % limit != 0) {
                                countPage++
                            }
                            if (countPage == 0) {
                                countPage = 1
                            }
                            if (it.data.list != null) {
                                it.data.list.let { list ->
                                    mData.addAll(list)
                                }
                                mAdapter?.notifyDataSetChanged()
                            }
                        }
                        -1 -> {
                            ToastUtils.show(it.msg)
                            ActivityManager.toSplashActivity(activity as MainActivityV5)
                        }
                        else -> {
                            ToastUtils.show(it.msg)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                isHttp = false
                mView?.refresh_videoLists?.finishRefresh()
                mView?.refresh_videoLists?.finishLoadMore()
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
            })
    }

    override fun onResume() {
        super.onResume()
        mAdapter?.onResume()
    }

    override fun onPause() {
        super.onPause()
        mAdapter?.onPause()
    }

    override fun onDestroy() {
        super.onDestroy()
        mAdapter?.exit()
    }
}