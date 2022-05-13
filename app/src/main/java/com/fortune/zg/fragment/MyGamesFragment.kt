package com.fortune.zg.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.listener.OnBottomBarItemSelectListener
import kotlinx.android.synthetic.main.fragment_my_games.view.*

class MyGamesFragment : Fragment() {

    private var mView: View? = null
    private var currentPage = 0
    private var pcFragment: MyGamesWithTypeFragment? = null
    private var phoneFragment: MyGamesWithTypeFragment? = null

    companion object {
        fun newInstance() = MyGamesFragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_my_games, container, false)
        initView()
        return mView
    }

    private fun initView() {
        pcFragment = MyGamesWithTypeFragment.newInstance(1)

        childFragmentManager.beginTransaction()
            .add(R.id.fl_myGames, pcFragment!!)
            .commit()

        mView?.tt_myGames?.setCurrentItem(currentPage)
        toChangeFragment(currentPage)
        mView?.tt_myGames?.setOnItemListener(object : OnBottomBarItemSelectListener {
            override fun setOnItemSelectListener(index: Int) {
                toChangeFragment(index)
            }
        })
    }

    /**
     * 修改fragment
     */
    private fun toChangeFragment(index: Int) {
        hideAll()
        when (index) {
            0 -> {
                currentPage = 0
                childFragmentManager.beginTransaction()
                    .show(pcFragment!!)
                    .commitAllowingStateLoss()
            }
            1 -> {
                currentPage = 1
                if (null == phoneFragment) {
                    phoneFragment = MyGamesWithTypeFragment.newInstance(2)
                    childFragmentManager.beginTransaction()
                        .add(R.id.fl_myGames,phoneFragment!!)
                        .commitAllowingStateLoss()
                } else {
                    childFragmentManager.beginTransaction()
                        .show(phoneFragment!!)
                        .commitAllowingStateLoss()
                }
            }
        }
    }

    private fun hideAll() {
        childFragmentManager.beginTransaction()
            .hide(pcFragment!!)
            .hide(phoneFragment ?: pcFragment!!)
            .commitAllowingStateLoss()
    }
}