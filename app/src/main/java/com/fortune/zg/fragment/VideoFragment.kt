package com.fortune.zg.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.event.NewVideo
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import kotlinx.android.synthetic.main.activity_main_v5.*
import kotlinx.android.synthetic.main.fragment_video.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode

class VideoFragment : Fragment() {

    private var mView: View? = null

    companion object {
        fun newInstance() = VideoFragment()
    }

    private var allVideoFragment: VideoListFragment? = null
    private var phoneVideoFragment: VideoListFragment? = null
    private var pcVideoFragment: VideoListFragment? = null
    private var currentFragment: VideoListFragment? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_video, container, false)
        EventBus.getDefault().register(this)
        initView()
        return mView
    }

    private fun initView() {
        allVideoFragment = VideoListFragment.newInstance(1)
        currentFragment = allVideoFragment

        val beginTransaction = childFragmentManager.beginTransaction()
        beginTransaction
            .add(R.id.fl_video, allVideoFragment!!)
            .commit()

        mView?.tt_video?.setCurrentItem(0)
        switchFragment(0)
        mView?.tt_video?.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                switchFragment(index)
            }
        })
    }

    private fun switchFragment(index: Int) {
        hideAllFragment()
        when (index) {
            0 -> {
                if (currentFragment == allVideoFragment) {
                    childFragmentManager.beginTransaction()
                        .remove(currentFragment!!)
                        .commitAllowingStateLoss()

                    allVideoFragment = VideoListFragment.newInstance(1)
                    currentFragment = allVideoFragment
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_video, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    currentFragment = allVideoFragment
                    childFragmentManager.beginTransaction()
                        .show(currentFragment!!)
                        .commitAllowingStateLoss()
                }
            }
            1 -> {
                if (null == phoneVideoFragment) {
                    phoneVideoFragment = VideoListFragment.newInstance(3)
                    currentFragment = phoneVideoFragment
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_video, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    if (currentFragment == phoneVideoFragment) {
                        childFragmentManager.beginTransaction()
                            .remove(currentFragment!!)
                            .commitAllowingStateLoss()

                        phoneVideoFragment = VideoListFragment.newInstance(3)
                        currentFragment = phoneVideoFragment
                        childFragmentManager.beginTransaction()
                            .add(R.id.fl_video, currentFragment!!)
                            .commitAllowingStateLoss()
                    } else {
                        currentFragment = phoneVideoFragment
                        childFragmentManager.beginTransaction()
                            .show(currentFragment!!)
                            .commitAllowingStateLoss()
                    }
                }
            }
            2 -> {
                if (null == pcVideoFragment) {
                    pcVideoFragment = VideoListFragment.newInstance(2)
                    currentFragment = pcVideoFragment
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_video, currentFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    if (currentFragment == pcVideoFragment) {
                        childFragmentManager.beginTransaction()
                            .remove(currentFragment!!)
                            .commitAllowingStateLoss()

                        pcVideoFragment = VideoListFragment.newInstance(2)
                        currentFragment = pcVideoFragment
                        childFragmentManager.beginTransaction()
                            .add(R.id.fl_video, currentFragment!!)
                            .commitAllowingStateLoss()
                    } else {
                        currentFragment = pcVideoFragment
                        childFragmentManager.beginTransaction()
                            .show(currentFragment!!)
                            .commitAllowingStateLoss()
                    }
                }
            }
        }
    }

    private fun hideAllFragment() {
        val beginTransaction = childFragmentManager.beginTransaction()
        beginTransaction
            .hide(allVideoFragment!!)
            .hide(phoneVideoFragment ?: allVideoFragment!!)
            .hide(pcVideoFragment ?: allVideoFragment!!)
            .commitAllowingStateLoss()
    }

    fun hideNewVideo() {
//        MainActivityV5.getInstance()?.tab_mainV5.hideNewVideo()
        mView?.tt_video?.hideNewVideo()
    }

    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun haveNewVideo(newVideo: NewVideo) {
        when (newVideo.type) {
            0 -> {
                mView?.tt_video?.haveNewVideo(0)
            }
            1 -> {
                mView?.tt_video?.haveNewVideo(1)
            }
            2 -> {
                mView?.tt_video?.haveNewVideo(2)
            }
        }
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        super.onDestroy()
    }
}