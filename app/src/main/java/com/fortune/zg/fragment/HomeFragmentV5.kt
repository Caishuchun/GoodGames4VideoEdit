package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import cn.jzvd.Jzvd
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.activity.SearchGameActivity
import com.fortune.zg.newVideo.VideoListsFragment
import com.fortune.zg.utils.LogUtils
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_home_v5.view.*
import java.util.concurrent.TimeUnit

class HomeFragmentV5 : Fragment() {

    private var mView: View? = null
    private var currentIndex = 0

    private var allVideoListsFragment: VideoListsFragment? = null
    private var phoneVideoListsFragment: VideoListsFragment? = null
    private var pcVideoListsFragment: VideoListsFragment? = null
    var currentFragment: VideoListsFragment? = null

    companion object {
        @JvmStatic
        fun newInstance() = HomeFragmentV5()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_home_v5, container, false)
        initView()
        return mView
    }

    override fun onHiddenChanged(hidden: Boolean) {
        super.onHiddenChanged(hidden)
        if (hidden) {
            //视频暂停
            Jzvd.goOnPlayOnPause()
        } else {
            Jzvd.goOnPlayOnResume()
        }
    }

    /**
     * 刷新
     */
    fun refresh() {
        currentFragment?.refresh()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        toChangeFragment()

        RxView.clicks(mView?.iv_homeV5_search!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                startActivity(
                    Intent(
                        activity as MainActivityV5,
                        SearchGameActivity::class.java
                    )
                )
            }

        RxView.clicks(mView?.tv_homeV5_all!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 0) {
                    currentIndex = 0
                    toChangeFragment()
                } else {
                    //刷新
                    currentFragment?.refresh()
                }
            }
        RxView.clicks(mView?.tv_homeV5_phone!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 1) {
                    currentIndex = 1
                    toChangeFragment()
                } else {
                    //刷新
                    currentFragment?.refresh()
                }
            }
        RxView.clicks(mView?.tv_homeV5_pc!!)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (currentIndex != 2) {
                    currentIndex = 2
                    toChangeFragment()
                } else {
                    //刷新
                    currentFragment?.refresh()
                }
            }
    }

    /**
     * 调换Fragment
     */
    private fun toChangeFragment() {
        mView?.tv_homeV5_all?.setTextColor(resources.getColor(R.color.white_FFFFFF))
        mView?.tv_homeV5_phone?.setTextColor(resources.getColor(R.color.white_FFFFFF))
        mView?.tv_homeV5_pc?.setTextColor(resources.getColor(R.color.white_FFFFFF))
        when (currentIndex) {
            0 -> {
                mView?.tv_homeV5_all?.setTextColor(resources.getColor(R.color.green_2EC8AC))
                if (null == allVideoListsFragment) {
                    allVideoListsFragment = VideoListsFragment.newInstance(1)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_homeV5_root, allVideoListsFragment!!)
                        .commit()
                } else {
                    hideAllFragment()
                    childFragmentManager.beginTransaction()
                        .show(allVideoListsFragment!!)
                        .commit()
                }
                currentFragment = allVideoListsFragment
            }
            1 -> {
                mView?.tv_homeV5_phone?.setTextColor(resources.getColor(R.color.green_2EC8AC))
                if (null == phoneVideoListsFragment) {
                    phoneVideoListsFragment = VideoListsFragment.newInstance(3)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_homeV5_root, phoneVideoListsFragment!!)
                        .commit()
                } else {
                    hideAllFragment()
                    childFragmentManager.beginTransaction()
                        .show(phoneVideoListsFragment!!)
                        .commit()
                }
                currentFragment = phoneVideoListsFragment
            }
            2 -> {
                mView?.tv_homeV5_pc?.setTextColor(resources.getColor(R.color.green_2EC8AC))
                if (null == pcVideoListsFragment) {
                    pcVideoListsFragment = VideoListsFragment.newInstance(2)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_homeV5_root, pcVideoListsFragment!!)
                        .commit()
                } else {
                    hideAllFragment()
                    childFragmentManager.beginTransaction()
                        .show(pcVideoListsFragment!!)
                        .commit()
                }
                currentFragment = pcVideoListsFragment
            }
        }
    }

    private fun hideAllFragment() {
        childFragmentManager.beginTransaction()
            .hide(allVideoListsFragment!!)
            .hide(phoneVideoListsFragment ?: allVideoListsFragment!!)
            .hide(pcVideoListsFragment ?: allVideoListsFragment!!)
            .commit()
    }

}