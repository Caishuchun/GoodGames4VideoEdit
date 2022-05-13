package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.fortune.zg.R
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.activity.SearchGameActivity
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_pc_v2.view.*
import java.util.concurrent.TimeUnit

class PcFragmentV2 : Fragment() {

    private var mView: View? = null

    companion object {
        fun newInstance() = PcFragmentV2()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_pc_v2, container, false)
        initView()
        return mView
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        mView?.ll_pc_v2_search?.let {
            RxView.clicks(it)
                .throttleFirst(200, TimeUnit.MILLISECONDS)
                .subscribe {
                    startActivity(
                        Intent(
                            activity as MainActivityV5,
                            SearchGameActivity::class.java
                        )
                    )
                }
        }

        val gameListFragment = GameListFragment.newInstance(2)
        childFragmentManager.beginTransaction()
            .add(R.id.fl_pc_v2, gameListFragment)
            .commit()
    }
}