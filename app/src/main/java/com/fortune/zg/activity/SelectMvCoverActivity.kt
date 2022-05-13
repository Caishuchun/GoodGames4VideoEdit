package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.widget.SeekBar
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.utils.StatusBarUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_select_mv_cover.*
import java.util.concurrent.TimeUnit

class SelectMvCoverActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: SelectMvCoverActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val MV_PATH = "mv_path"
        const val MV_DURATION = "mv_duration"
        const val MV_COVER_TIME = "mv_cover_time"
    }

    private var mvPath: String = ""
    private var mvDuration = 0L
    private var mCurrentTime = 1L

    override fun getLayoutId() = R.layout.activity_select_mv_cover

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, true)
        instance = this
        initView()
    }

    @SuppressLint("CheckResult")
    private fun initView() {
        mvPath = intent.getStringExtra(MV_PATH)!!
        mvDuration = intent.getLongExtra(MV_DURATION, -1L)
        mCurrentTime = intent.getLongExtra(MV_COVER_TIME, 1L)

        video_selectMvCover.setVideoPath(mvPath)

        if (mvDuration == -1L) {
            video_selectMvCover.setOnPreparedListener {
                mvDuration = video_selectMvCover.duration.toLong()
                sb_selectMvCover.max = mvDuration.toInt()
                sb_selectMvCover.progress = mCurrentTime.toInt()
                val seekTo = if (mCurrentTime == 0L) 1L else mCurrentTime
                video_selectMvCover.seekTo(seekTo.toInt())
            }
        } else {
            sb_selectMvCover.max = mvDuration.toInt()
            sb_selectMvCover.progress = mCurrentTime.toInt()
            val seekTo = if (mCurrentTime == 0L) 1L else mCurrentTime
            video_selectMvCover.seekTo(seekTo.toInt())
        }


        sb_selectMvCover.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                mCurrentTime = progress.toLong()
                video_selectMvCover.seekTo(mCurrentTime.toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {

            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
            }
        })

        RxView.clicks(tv_selectMvCover_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        RxView.clicks(iv_selectMvCover_little)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (mCurrentTime >= 1001L) {
                    mCurrentTime -= 1000L
                } else {
                    mCurrentTime = 1L
                }
                video_selectMvCover.seekTo(mCurrentTime.toInt())
                sb_selectMvCover.progress = mCurrentTime.toInt()
            }

        RxView.clicks(iv_selectMvCover_add)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                if (mvDuration - mCurrentTime >= 1000L) {
                    mCurrentTime += 1000L
                } else {
                    mCurrentTime = mvDuration
                }
                video_selectMvCover.seekTo(mCurrentTime.toInt())
                sb_selectMvCover.progress = mCurrentTime.toInt()
            }

        RxView.clicks(tv_selectMvCover_save)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                val intent = Intent()
                intent.putExtra(IssueMvActivity.MV_COVER_TIME, mCurrentTime)
                setResult(RESULT_OK, intent)
                finish()
            }
    }

    override fun destroy() {
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