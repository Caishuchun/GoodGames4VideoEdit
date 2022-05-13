package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.text.Html
import android.text.Spanned
import android.view.LayoutInflater
import android.view.View
import android.view.inputmethod.EditorInfo
import androidx.recyclerview.widget.OrientationHelper
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.bean.GameListBean
import com.fortune.zg.bean.GameListWithMvBean
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.room.*
import com.fortune.zg.utils.*
import com.fortune.zg.video.VideoActivity
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.jakewharton.rxbinding2.widget.RxTextView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.activity_search_game.*
import kotlinx.android.synthetic.main.layout_game_item.view.*
import kotlinx.android.synthetic.main.layout_item_hot_search.view.*
import kotlinx.android.synthetic.main.layout_item_search_his.view.*
import kotlinx.android.synthetic.main.layout_item_search_result.view.*
import kotlinx.android.synthetic.main.layout_item_search_sugrec.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import java.io.Serializable
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

class SearchGameActivity : BaseActivity() {
    private var getHotSearchHisObservable: Disposable? = null
    private var getSearchSugrecObservable: Disposable? = null
    private var searchObservable: Disposable? = null
    private var hotSearchList = arrayListOf<String>()
    private var searchSugrecList = arrayListOf<String>()
    private var searchHisList = mutableListOf<SearchHis>()
    private var searchList = mutableListOf<GameListWithMvBean.DataBean.ListBean>()
    private lateinit var hotSearchAdapter: BaseAdapterWithPosition<String>
    private lateinit var searchHisAdapter: BaseAdapterWithPosition<SearchHis>
    private lateinit var searchSugrecAdapter: BaseAdapterWithPosition<String>
    private lateinit var searchAdapter: BaseAdapterWithPosition<GameListWithMvBean.DataBean.ListBean>

    private var isNeedSugrec = true //是不是需要建议和历史记录的请求

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SearchGameActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null

        lateinit var searchHisDao: SearchHisDao
    }

    override fun getLayoutId() = R.layout.activity_search_game

    override fun doSomething() {
        instance = this
        StatusBarUtils.setTextDark(this, true)

        et_search_str.isFocusable = true
        et_search_str.isFocusableInTouchMode = true
        et_search_str.requestFocus()

        initSearchHis()
        initView()
        getHotSearch()
    }

    @SuppressLint("CheckResult")
    private fun initSearchHis() {
        val dataBase = SearchHisDataBase.getDataBase(this.applicationContext)
        searchHisDao = dataBase.searchHisDao()

        val stepAll = searchHisDao.all
        val all = mutableListOf<SearchHis>()
        if (stepAll.isNotEmpty()) {
            for (index in stepAll.indices) {
                all.add(stepAll[stepAll.size - 1 - index])
            }
        }
        if (all.isEmpty()) {
            //没有缓存的搜索记录
            ll_search_his_only2.visibility = View.GONE
            view_search_line.visibility = View.GONE
        } else {
            when (all.size) {
                1 -> {
                    //仅有一条搜索记录
                    //1.仅显示第一条搜索记录
                    //2.其他都不显示
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.GONE
                    ll_search_his_3.visibility = View.GONE
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteAll()
                            ll_search_his_1.visibility = View.GONE
                            view_search_line.visibility = View.GONE
                        }
                }
                2 -> {
                    //仅有两条搜索记录
                    //1.显示两条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.GONE
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                3 -> {
                    //仅有三条条搜索记录
                    //1.显示三条条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.GONE
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            ll_search_his_3.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                4 -> {
                    //仅有四条条搜索记录
                    //1.显示四条条搜索记录
                    //2.不显示加载更多
                    //3.并且可以删除记录
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.VISIBLE
                    tv_search_his_4.text = all[3].str
                    tv_search_showAll.visibility = View.GONE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            ll_search_his_1.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            ll_search_his_2.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            ll_search_his_3.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                    RxView.clicks(ll_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[3].str)
                        }
                    RxView.clicks(iv_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[3])
                            ll_search_his_4.visibility = View.GONE
                            if (searchHisDao.all.isEmpty()) {
                                view_search_line.visibility = View.GONE
                            }
                        }
                }
                else -> {
                    //搜索记录大于四条
                    //1.显示前四条记录
                    //2.显示加载更多
                    //3.点击加载更多显示recycleview来加载更多搜索记录
                    //4.前四条可以单独删除
                    ll_search_his_only2.visibility = View.VISIBLE
                    view_search_line.visibility = View.VISIBLE
                    rv_search_hot.visibility = View.VISIBLE
                    ll_search_his_1.visibility = View.VISIBLE
                    tv_search_his_1.text = all[0].str
                    ll_search_his_2.visibility = View.VISIBLE
                    tv_search_his_2.text = all[1].str
                    ll_search_his_3.visibility = View.VISIBLE
                    tv_search_his_3.text = all[2].str
                    ll_search_his_4.visibility = View.VISIBLE
                    tv_search_his_4.text = all[3].str
                    tv_search_showAll.visibility = View.VISIBLE
                    RxView.clicks(ll_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[0].str)
                        }
                    RxView.clicks(iv_search_his_1)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[0])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[1].str)
                        }
                    RxView.clicks(iv_search_his_2)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[1])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[2].str)
                        }
                    RxView.clicks(iv_search_his_3)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[2])
                            initSearchHis()
                        }
                    RxView.clicks(ll_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            toSearch(all[3].str)
                        }
                    RxView.clicks(iv_search_his_4)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            searchHisDao.deleteHis(all[3])
                            initSearchHis()
                        }
                    RxView.clicks(tv_search_showAll)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            ll_search_his_only2.visibility = View.GONE
                            ll_search_his_all.visibility = View.VISIBLE
                            val newAll = searchHisDao.all
                            searchHisList.clear()
                            searchHisAdapter.notifyDataSetChanged()
                            if (newAll.isNotEmpty()) {
                                for (index in newAll.indices) {
                                    searchHisList.add(newAll[newAll.size - 1 - index])
                                }
                                searchHisAdapter.notifyDataSetChanged()
                            }
                        }
                }
            }
        }
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        //取消即退出
        RxView.clicks(tv_search_cancel)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe { finish() }

        //热门搜索的adapter
        hotSearchAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.layout_item_hot_search)
            .setData(hotSearchList)
            .addBindView { itemView, itemData, position ->
                when (position) {
                    0 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.red_F03D3D))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.red_F03D3D))
                    }
                    1 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.orange_FF774E))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.orange_FF774E))
                    }
                    2 -> {
                        itemView.tv_hot_search_item_num.setTextColor(resources.getColor(R.color.orange_FFA855))
                        itemView.tv_hot_search_item_hot.setTextColor(resources.getColor(R.color.orange_FFA855))
                    }
                }
                itemView.tv_hot_search_item_hot.text = itemData
                itemView.tv_hot_search_item_num.text = "${position + 1}"
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toSearch(itemData)
                    }
            }
            .create()
        //热门搜索的recycle
        rv_search_hot.layoutManager =
            SafeStaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
        rv_search_hot.adapter = hotSearchAdapter

        //搜索历史的adapter
        searchHisAdapter = BaseAdapterWithPosition.Builder<SearchHis>()
            .setLayoutId(R.layout.layout_item_search_his)
            .setData(searchHisList)
            .addBindView { itemView, itemData, position ->
                if (searchHisDao.all.isNotEmpty()) {
                    tv_search_clearAll.visibility = View.VISIBLE
                }
                itemView.tv_item_search_his.text = itemData.str
                RxView.clicks(itemView.iv_item_search_his)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        searchHisList.remove(itemData)
                        searchHisDao.deleteHis(itemData)
                        searchHisAdapter.notifyDataSetChanged()
                        if (searchHisDao.all.isEmpty()) {
                            view_search_line.visibility = View.GONE
                            tv_search_clearAll.visibility = View.GONE
                        }
                    }
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        toSearch(itemData.str)
                    }
            }
            .create()
        //搜索历史的recycle
        rv_search_all.layoutManager = SafeLinearLayoutManager(this)
        rv_search_all.adapter = searchHisAdapter
        //点击清空搜索记录
        RxView.clicks(tv_search_clearAll)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                searchHisList.clear()
                searchHisAdapter.notifyDataSetChanged()
                searchHisDao.deleteAll()
                ll_search_his_all.visibility = View.GONE
                view_search_line.visibility = View.GONE
            }

        //监听输入框的输入
        var oldStr = ""
        RxTextView.textChanges(et_search_str)
            .skipInitialValue()
            .subscribe {
                if (it.isEmpty()) {
                    iv_search_delete.visibility = View.GONE
                    ll_search_noInput.visibility = View.VISIBLE
                    rv_search_input.visibility = View.GONE
                    rv_search_result.visibility = View.GONE
                } else {
                    iv_search_delete.visibility = View.VISIBLE
                    ll_search_noInput.visibility = View.GONE
                    rv_search_input.visibility = View.VISIBLE
                    if (oldStr.length > it.length) {
                        rv_search_result.visibility = View.GONE
                    }
                    if (oldStr != it.toString() && isNeedSugrec) {
                        toShowSugrec(it.toString())
                    } else {
                        isNeedSugrec = true
                    }
                }
                oldStr = it.toString()
            }
        //软键盘回车的监听
        et_search_str.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                toSearch(et_search_str.text.toString().trim())
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }

        //清除输入的文字
        RxView.clicks(iv_search_delete)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                et_search_str.setText("")
                iv_search_delete.visibility = View.GONE
                if (ll_search_his_only2.visibility == View.VISIBLE) {
                    initSearchHis()
                }
                if (ll_search_his_all.visibility == View.VISIBLE) {
                    val all = searchHisDao.all
                    if (all.isNotEmpty()) {
                        searchHisList.clear()
                        for (index in all.indices) {
                            searchHisList.add(all[all.size - 1 - index])
                        }
                    }
                    searchHisAdapter.notifyDataSetChanged()
                }
            }

        //搜索建议的adapter
        searchSugrecAdapter = BaseAdapterWithPosition.Builder<String>()
            .setLayoutId(R.layout.layout_item_search_sugrec)
            .setData(searchSugrecList)
            .addBindView { itemView, itemData, position ->
                var gameName = ""
                var gameID = ""
                if (itemData.contains("|")) {
                    val lastIndexOf = itemData.lastIndexOf("|")
                    gameName = itemData.substring(0, lastIndexOf)
                    gameID = itemData.substring(lastIndexOf + 1, itemData.length)
                    itemView.view_item_search_sugrec.visibility = View.VISIBLE

                    //上色
                    itemView.tv_item_search_sugrec_gameName.text =
                        redText(gameName, et_search_str.text.toString().trim())
                    itemView.tv_item_search_sugrec_gameId.text =
                        redText(
                            gameID,
                            et_search_str.text.toString().trim()
                        )
                } else {
                    itemView.view_item_search_sugrec.visibility = View.GONE
                    itemView.tv_item_search_sugrec_gameId.text = ""
                    gameName = itemData

                    //上色
                    itemView.tv_item_search_sugrec_gameName.text =
                        redText(gameName, et_search_str.text.toString().trim())
                }
                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (gameID.contains(et_search_str.text.toString().trim())) {
                            toSearch(gameID.substring(gameID.indexOf(":") + 1, gameID.length))
                        } else {
                            toSearch(gameName)
                        }
                    }
            }.create()
        rv_search_input.adapter = searchSugrecAdapter
        rv_search_input.layoutManager = SafeLinearLayoutManager(this)

        //搜索的adapter
        searchAdapter = BaseAdapterWithPosition.Builder<GameListWithMvBean.DataBean.ListBean>()
            .setLayoutId(R.layout.layout_game_item)
            .setData(searchList)
            .addBindView { itemView, itemData, position ->
                if (itemData.item_type == "video") {
                    itemView.ll_item_game_normal?.visibility = View.GONE
                    itemView.ll_item_game_live?.visibility = View.GONE
                    itemView.rl_item_game_mv?.visibility = View.VISIBLE
                    itemView.tv_item_game_mv_des.text = itemData.video_name

                    val layoutParams = itemView.civ_item_game_mv_icon.layoutParams
                    val videoCoverHeight = itemData.video_cover_height!!
                    val videoCoverWidth = itemData.video_cover_width!!
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
//                        .placeholder(R.mipmap.bg_gray_6)
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
                            MobclickAgent.onEvent(
                                this,
                                "video_view",
                                "video_from_search"
                            )
                            val intent = Intent(this, VideoActivity::class.java)
                            intent.putExtra(VideoActivity.VIDEO_TYPE, 1)
                            intent.putExtra(VideoActivity.VIDEO_ID, itemData.video_id)
                            intent.putExtra(
                                VideoActivity.VIDEO_COVER,
                                itemData.video_cover
                            )
                            intent.putExtra(
                                VideoActivity.VIDEO_COVER_WIDTH,
                                itemData.video_cover_width
                            )
                            intent.putExtra(
                                VideoActivity.VIDEO_COVER_HEIGHT,
                                itemData.video_cover_height
                            )
                            intent.putExtra(VideoActivity.IS_MAIN_VIDEO, false)
                            startActivity(intent)
                        }
                } else {
                    itemView.ll_item_game_normal?.visibility = View.VISIBLE
                    itemView.ll_item_game_live?.visibility = View.GONE
                    itemView.rl_item_game_mv?.visibility = View.GONE
                    Glide.with(this)
                        .load(itemData.game_cover)
                        .placeholder(R.mipmap.bg_gray_6)
                        .diskCacheStrategy(DiskCacheStrategy.ALL)
                        .into(itemView.civ_item_game_icon)
                    itemView.tv_item_game_hot.text = itemData.game_hits.toString()
                    itemView.tv_item_game_name.text = itemData.game_name
                    itemView.tv_item_game_type.text = getSystem(itemData.game_system!!)
                    itemView.tv_item_game_type.setBackgroundResource(
                        if (itemData.game_system == 1) R.drawable.bg_game_type_purple
                        else R.drawable.bg_game_type_blue
                    )
                    itemView.tv_item_game_updateTime.text =
                        "${df.format(itemData.game_update_time!! * 1000L)}${getString(R.string.update)}"

                    if (null == itemData.game_gift_last) {
                        itemView.iv_item_game_gift.visibility = View.GONE
                        itemView.tv_item_game_gift_num.visibility = View.GONE
                    } else {
                        itemView.iv_item_game_gift.visibility = View.VISIBLE
                        itemView.tv_item_game_gift_num.visibility = View.VISIBLE
                        itemView.tv_item_game_gift_num.text = "x${itemData.game_gift_last}"
                    }

                    itemView.fl_item_game.removeAllViews()
                    for (index in itemData.game_tag!!.indices) {
                        val view =
                            LayoutInflater.from(this)
                                .inflate(R.layout.layout_item_tag, null)
                        view.tv_tag.text = itemData.game_tag!![index]
                        itemView.fl_item_game.addView(view)
                    }

                    itemView.iv_item_game_badge.setImageResource(
                        if (itemData.game_badge == "new") R.mipmap.game_new
                        else R.mipmap.good
                    )
                    itemView.tv_item_game_des.text = itemData.game_desc

                    RxView.clicks(itemView.rootView)
                        .throttleFirst(200, TimeUnit.MILLISECONDS)
                        .subscribe {
                            val from =
                                if (itemData.game_id.toString() == et_search_str.text.toString()
                                        .trim()
                                ) {
                                    //如果id和输入文字一致
                                    "game_detail_from_search_id"
                                } else {
                                    //不一致
                                    "game_detail_from_search_name"
                                }
                            MobclickAgent.onEvent(
                                this@SearchGameActivity,
                                "game_detail",
                                from
                            )
                            val intent = Intent(this, GameDetailActivity::class.java)
                            intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id)
                            intent.putExtra(GameDetailActivity.GAME_COVER, itemData.game_cover)
                            intent.putExtra(GameDetailActivity.GAME_BADGE, itemData.game_badge)
                            startActivity(intent)
                        }
                }
            }
            .create()
        rv_search_result.adapter = searchAdapter
        rv_search_result.layoutManager =
            SafeStaggeredGridLayoutManager(3, OrientationHelper.VERTICAL)
    }

    /**
     * 格式化系统类型
     */
    private fun getSystem(type: Int) = when (type) {
        1 -> getString(R.string.pc)
        else -> getString(R.string.phone)
    }

    /**
     * 获取搜索建议
     */
    private fun toShowSugrec(str: String) {
        searchSugrecList.clear()
        searchSugrecAdapter.notifyDataSetChanged()
        val searchSugrec = RetrofitUtils.builder().searchSugrec(str)
        getSearchSugrecObservable = searchSugrec.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null && it.getCode() == 1 && it.getData() != null && it.getData()!!.size >= 0) {
                    for (sugrec in it.getData()!!) {
                        searchSugrecList.add(sugrec)
                    }
                    searchSugrecAdapter.notifyDataSetChanged()
                } else {
                    ll_search_noInput.visibility = View.VISIBLE
                    rv_search_input.visibility = View.GONE
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
//                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    /**
     * 正式搜索
     */
    private fun toSearch(str: String) {
        LogUtils.d("============search===$str")
        searchList.clear()
        searchAdapter.notifyDataSetChanged()
        isNeedSugrec = false
        searchHisDao.addHis(SearchHis((str)))
        val all = searchHisDao.all
        if (all.size > 30) {
            searchHisDao.deleteHis(all[0])
        }
        et_search_str.setText(str)
        et_search_str.setSelection(str.length)
        ll_search_noInput.visibility = View.GONE
        rv_search_input.visibility = View.GONE
        rv_search_result.visibility = View.VISIBLE
        DialogUtils.showBeautifulDialog(this)
        val search = RetrofitUtils.builder().search(str, 1)
        searchObservable = search.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (it.getData() != null) {
                                val list = it.getData()!!.list
                                if (!list.isNullOrEmpty()) {
                                    for (info in list) {
                                        searchList.add(info)
                                    }
                                    searchAdapter.notifyDataSetChanged()
                                }
                            }
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                        else -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
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
     * 获取热门搜索
     */
    private fun getHotSearch() {
        hotSearchList.clear()
        hotSearchAdapter.notifyDataSetChanged()
        DialogUtils.showBeautifulDialog(this)
        val hotSearch = RetrofitUtils.builder().hotSearch()
        getHotSearchHisObservable = hotSearch.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            ll_search_hot_search.visibility = View.VISIBLE
                            for (hot in it.getData()!!) {
                                hotSearchList.add(hot)
                            }
                            hotSearchAdapter.notifyDataSetChanged()
                        }
                        -1 -> {
                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
                            ActivityManager.toSplashActivity(this)
                        }
                    }
                } else {
                    ToastUtils.show(getString(R.string.network_fail_to_responseDate))
                }
            }, {
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
//                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(this, it))
            })
    }

    override fun destroy() {
        getHotSearchHisObservable?.dispose()
        getSearchSugrecObservable?.dispose()
        searchObservable?.dispose()

        getHotSearchHisObservable = null
        getSearchSugrecObservable = null
        searchObservable = null
    }

    /**
     * 关键字标红
     */
    @SuppressLint("SetTextI18n")
    private fun redText(str: String, key: String): Spanned? {
        return if (str.contains(key)) {
            val start = str.indexOf(key)
            val end = start + key.length
            Html.fromHtml(
                "${str.substring(0, start)}<font color='#FF0000'>$key</font>${
                    str.substring(
                        end,
                        str.length
                    )
                }"
            )
        } else {
            Html.fromHtml(str)
        }
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