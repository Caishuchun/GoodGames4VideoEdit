package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.fortune.zg.R
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.utils.StatusBarUtils
import com.fortune.zg.widget.SafeStaggeredGridLayoutManager
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_pic_list.*
import kotlinx.android.synthetic.main.item_piclist_pic.view.*
import java.util.*
import java.util.concurrent.TimeUnit

class PicListActivity : BaseActivity() {

    private var picList = mutableListOf<String>()
    private var gameName = ""
    private var adapter: BaseAdapterWithPosition<String>? = null

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: PicListActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val LIST = "list"
        const val GAME_NAME = "game_name"
    }

    override fun getLayoutId() = R.layout.activity_pic_list

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        gameName = intent.getStringExtra(GAME_NAME)!!
        picList = intent.getStringArrayListExtra(ShowPicActivity.LIST)!!
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        RxView.clicks(iv_picList_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }
        tv_picList_title.text = gameName
        tv_picList_gameName.text = getString(R.string.game_pic_list).replace("X", gameName)
        adapter = BaseAdapterWithPosition.Builder<String>()
            .setData(picList)
            .setLayoutId(R.layout.item_piclist_pic)
            .addBindView { itemView, itemData, position ->
                Glide.with(this)
                    .load(itemData)
                    .placeholder(R.mipmap.image_loading)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .into(itemView.iv_item_picList_pic)

                RxView.clicks(itemView.rootView)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        val intent = Intent(this, ShowPicActivity::class.java)
                        intent.putExtra(ShowPicActivity.POSITION, position)
                        intent.putStringArrayListExtra(
                            ShowPicActivity.LIST,
                            picList as ArrayList<String>
                        )
                        startActivity(intent)
                    }
            }.create()
        rv_picList.adapter = adapter
        rv_picList.layoutManager =
            SafeStaggeredGridLayoutManager(3, StaggeredGridLayoutManager.VERTICAL)
    }

    override fun destroy() {
        picList.clear()
        adapter = null
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