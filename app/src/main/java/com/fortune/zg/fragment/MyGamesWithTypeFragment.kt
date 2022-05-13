package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.activity.GameDetailActivity
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.adapter.BaseAdapter
import com.fortune.zg.bean.MyGamesBean
import com.fortune.zg.event.GetGiftCode
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.http.HttpUrls
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.room.LocalGameDateBase
import com.fortune.zg.room.LookHis
import com.fortune.zg.room.LookHisDataBase
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_my_games_with_type.view.*
import kotlinx.android.synthetic.main.layout_item_search_result.view.*
import kotlinx.android.synthetic.main.layout_item_tag.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.text.SimpleDateFormat
import java.util.concurrent.TimeUnit

private const val TYPE = "type"

class MyGamesWithTypeFragment : Fragment() {
    private var type: Int? = null
    private var mView: View? = null
    private var myGamesObservable: Disposable? = null
    private var adapter: BaseAdapter<MyGamesBean.DataBean?>? = null
    private var mData = mutableListOf<MyGamesBean.DataBean?>()

    @SuppressLint("SimpleDateFormat")
    private val df = SimpleDateFormat("MM.dd")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        EventBus.getDefault().register(this)
        arguments?.let {
            type = it.getInt(TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_my_games_with_type, container, false)

        initView()
        if (type == 1) {
            if (MyApp.getInstance().isHaveToken()) {
                toGetInfo(type!!)
            }
        } else {
            toGetLocalGames()
        }
        return mView
    }

    private fun toGetLocalGames() {
        val localGameDateBase = LocalGameDateBase.getDataBase(activity as MainActivityV5)
        val localGameDao = localGameDateBase.localGameDao()
        val all = localGameDao.all
        if (all.isNotEmpty()) {
            for (localGame in all) {
                val gameTag = localGame.game_tag
                val game_tag = mutableListOf<String>()
                val split = gameTag.split("、")
                for (tag in split) {
                    game_tag.add(tag)
                }
                val dataBean = MyGamesBean.DataBean()
                dataBean.game_id = localGame.game_id
                dataBean.game_name = localGame.game_name
                dataBean.game_desc = localGame.game_desc
                dataBean.game_cover = localGame.game_cover
                dataBean.game_tag = game_tag
                dataBean.game_badge = localGame.game_badge
                dataBean.game_hits = localGame.game_hits
                dataBean.game_system = localGame.game_system
                dataBean.game_update_time = localGame.game_update_time
                mData.add(dataBean)
            }
            adapter?.notifyDataSetChanged()
        }
    }

    /**
     * 获取数据
     */
    private fun toGetInfo(page: Int) {
        DialogUtils.showBeautifulDialog(activity as MainActivityV5)
        val url = if (page == 1) {
            HttpUrls.MY_GAMES_PC
        } else {
            HttpUrls.MY_GAMES_PHONE
        }
        val myGames = RetrofitUtils.builder().myGames(url)
        myGamesObservable = myGames.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                DialogUtils.dismissLoading()
                if (it != null) {
                    when (it.getCode()) {
                        1 -> {
                            if (null != it.getData()) {
                                if (mData.size == 0) {
                                    mData.addAll(it.getData()!!)
                                } else {
                                    for (data in it.getData()!!) {
                                        var isHave = false
                                        for (haveData in mData) {
                                            if (haveData?.game_id == data?.game_id) {
                                                isHave = true
                                                break
                                            }
                                        }
                                        if (!isHave) {
                                            mData.add(data)
                                        }
                                    }
                                }
                                adapter?.notifyDataSetChanged()
                            }
                        }
                        -1 -> {
//                            it.getMsg()?.let { it1 -> ToastUtils.show(it1) }
//                            ActivityManager.toSplashActivity(activity as MainActivityV5)
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
                ToastUtils.show(HttpExceptionUtils.getExceptionMsg(activity as MainActivityV5, it))
            })
    }

    @SuppressLint("SetTextI18n", "CheckResult")
    private fun initView() {
        adapter = BaseAdapter.Builder<MyGamesBean.DataBean?>()
            .setLayoutId(R.layout.layout_item_search_result)
            .setData(mData)
            .addBindView { itemView, itemData ->
                Glide.with(this)
                    .load(itemData?.game_cover)
                    .placeholder(R.mipmap.bg_gray_6)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.iv_item_result_icon)
                itemView.tv_item_result_name.text = itemData?.game_name
                itemView.tv_item_result_code.text =
                    "${getString(R.string.game_id)}${itemData?.game_id}"

                itemView.tv_item_result_type.text = getSystem(itemData?.game_system!!)
                itemView.tv_item_result_type.setBackgroundResource(
                    if (itemData.game_system == 1) R.drawable.bg_game_type_purple
                    else R.drawable.bg_game_type_blue
                )
                itemView.tv_item_result_hot.text = itemData.game_hits?.toString()
                itemView.tv_item_result_time.text =
                    "${df.format(itemData.game_update_time!! * 1000L)}${getString(R.string.update)}"

                itemView.fl_item_result_tag.removeAllViews()
                itemData.game_tag?.let {
                    for (index in it.indices) {
                        val view = LayoutInflater.from(activity)
                            .inflate(R.layout.layout_item_tag4search, null)
                        view.tv_tag.text = it[index]
                        itemView.fl_item_result_tag.addView(view)
                    }
                }

                itemView.iv_item_result_news.setImageResource(
                    if (itemData.game_badge == "new") R.mipmap.game_new
                    else R.mipmap.good
                )
                itemData.game_desc?.let {
                    OmitTextViewUtils.omitTextView(
                        itemView.tv_item_result_des,
                        it,
                        10
                    )
                }

                if (null == itemData.game_gift_last) {
                    itemView.tv_item_result_gift_num.visibility = View.GONE
                    itemView.iv_item_result_gift.visibility = View.GONE
                } else {
                    itemView.tv_item_result_gift_num.visibility = View.VISIBLE
                    itemView.iv_item_result_gift.visibility = View.VISIBLE
                    itemView.tv_item_result_gift_num.text = "x${itemData.game_gift_last}"
                }

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val lookHisDao =
                            LookHisDataBase.getDataBase(activity as MainActivityV5).lookHisDao()
                        val gameTag = itemData.game_tag!!
                        var tagStr = ""
                        for (tag in gameTag) {
                            tagStr += "$tag|"
                        }
                        tagStr = tagStr.substring(0, tagStr.length - 1)
                        val all = lookHisDao.all
                        var isHas = false
                        for (lookHis in all) {
                            if (lookHis.game_id == itemData.game_id) {
                                //已经存有,删除
                                isHas = true
                                lookHisDao.deleteHis(lookHis)
                            }
                        }
                        if (!isHas && all.size >= 30) {
                            lookHisDao.deleteHis(all[all.size - 1])
                        }

                        lookHisDao.addHis(
                            LookHis(
                                itemData.game_id!!,
                                itemData.game_name!!,
                                itemData.game_desc!!,
                                itemData.game_cover!!,
                                itemData.game_hits!!,
                                itemData.game_system!!,
                                itemData.game_badge!!,
                                itemData.game_update_time!!,
                                tagStr
                            )
                        )
                        val from = if (type == 1) "pc" else "phone"
                        MobclickAgent.onEvent(
                            activity as MainActivityV5,
                            "game_detail",
                            "game_detail_from_myGames_$from"
                        )
                        val intent =
                            Intent(activity as MainActivityV5, GameDetailActivity::class.java)
                        intent.putExtra(GameDetailActivity.GAME_ID, itemData.game_id!!)
                        intent.putExtra(GameDetailActivity.GAME_COVER, itemData.game_cover!!)
                        intent.putExtra(GameDetailActivity.GAME_BADGE, itemData.game_badge!!)
                        startActivityForResult(intent, 2021)
                    }
            }
            .create()
        mView?.cv_myGames_list?.layoutManager = SafeLinearLayoutManager(activity)
        mView?.cv_myGames_list?.adapter = adapter
    }

    /**
     * 格式化系统类型
     */
    private fun getSystem(type: Int) = when (type) {
        1 -> getString(R.string.pc)
        else -> getString(R.string.phone)
    }

    companion object {
        @JvmStatic
        fun newInstance(type: Int) =
            MyGamesWithTypeFragment().apply {
                Bundle().apply {
                    putInt(TYPE, type)
                }.also { arguments = it }
            }
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun getNewInfo(getGiftCode: GetGiftCode) {
        mData.clear()
        adapter?.notifyDataSetChanged()
        if (getGiftCode.type == 2) {
            toGetLocalGames()
        }
        toGetInfo(type!!)
    }

    @Subscribe(sticky = true, threadMode = ThreadMode.MAIN)
    fun logined(loginStatusChange: LoginStatusChange) {
        toGetInfo(type!!)
    }

    override fun onDestroy() {
        super.onDestroy()
        EventBus.getDefault().unregister(this)
    }
}