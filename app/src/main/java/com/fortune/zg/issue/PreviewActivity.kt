package com.fortune.zg.issue

import android.annotation.SuppressLint
import android.view.View
import android.widget.MediaController
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.base.BaseActivity
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.activity_preview.*
import java.util.concurrent.TimeUnit

class PreviewActivity : BaseActivity() {
    companion object {
        @SuppressLint("StaticFieldLeak")
        lateinit var instance: PreviewActivity
        const val IS_VIDEO = "isVideo"
        const val FILE_PATH = "filePath"
        const val VIDEO_COVER = "videoCover"
    }

    private var isVideo = false
    private var filePath: String? = null
    private var videoCover: String? = null

    override fun getLayoutId() = R.layout.activity_preview

    override fun onBackPressed() {
        super.onBackPressed()
        finish()
    }

    @SuppressLint("CheckResult")
    override fun doSomething() {
        instance = this
        isVideo = intent.getBooleanExtra(IS_VIDEO, false)
        filePath = intent.getStringExtra(FILE_PATH)
        videoCover = intent.getStringExtra(VIDEO_COVER)

        RxView.clicks(iv_preview_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }

        if (isVideo) {
            video_preview.visibility = View.VISIBLE
            iv_preview.visibility = View.GONE
            video_preview.setMediaController(MediaController(this))

            video_preview.setVideoPath(filePath)
            video_preview.start()
            video_preview.requestFocus()
        } else {
            video_preview.visibility = View.GONE
            iv_preview.visibility = View.VISIBLE
            Glide.with(this)
                .load(filePath)
                .into(iv_preview)
        }
    }

    override fun destroy() {
    }
}