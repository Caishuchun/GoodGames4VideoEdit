package com.fortune.zg.activity

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Environment
import androidx.viewpager.widget.ViewPager
import com.fortune.zg.R
import com.fortune.zg.adapter.ShowPicAdapter
import com.fortune.zg.base.BaseActivity
import com.fortune.zg.utils.StatusBarUtils
import com.fortune.zg.utils.ToastUtils
import com.jakewharton.rxbinding2.view.RxView
import com.umeng.analytics.MobclickAgent
import kotlinx.android.synthetic.main.activity_show_pic.*
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.concurrent.TimeUnit


class ShowPicActivity : BaseActivity() {

    private var currentPosition = 0
    private var picList = mutableListOf<String>()

    companion object {
        @SuppressLint("StaticFieldLeak")
        private lateinit var instance: ShowPicActivity
        private fun isInstance() = this::instance.isInitialized
        fun getInstance() = if (isInstance()) instance else null
        const val POSITION = "position"
        const val LIST = "list"
    }

    override fun getLayoutId() = R.layout.activity_show_pic

    override fun doSomething() {
        StatusBarUtils.setTextDark(this, false)
        instance = this
        currentPosition = intent.getIntExtra(POSITION, 1)
        picList = intent.getStringArrayListExtra(LIST)!!
        initView()
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        RxView.clicks(iv_showPic_back)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                finish()
            }
        RxView.clicks(iv_showPic_save)
            .throttleFirst(2000, TimeUnit.MILLISECONDS)
            .subscribe {
                saveImg()
            }

        tv_showPic_title.text = "${currentPosition + 1}/${picList.size}"
        val adapter = ShowPicAdapter(this, picList)
        vp_showPic.adapter = adapter
        vp_showPic.currentItem = currentPosition
        vp_showPic.addOnPageChangeListener(object : ViewPager.OnPageChangeListener {
            @SuppressLint("SetTextI18n")
            override fun onPageScrolled(
                position: Int,
                positionOffset: Float,
                positionOffsetPixels: Int
            ) {
                currentPosition = position
                tv_showPic_title.text = "${position + 1}/${picList.size}"
            }

            override fun onPageSelected(position: Int) {
            }

            override fun onPageScrollStateChanged(state: Int) {
            }
        })
    }

    private fun saveImg() {
        object : Thread() {
            override fun run() {
                super.run()
                val directory: File =
                    File(Environment.getExternalStorageDirectory(), getString(R.string.app_name))
                if (!directory.exists()) {
                    directory.mkdir()
                }
                val filename = System.currentTimeMillis().toString() + ".png"
                val file = File(directory, filename)
                try {
                    FileOutputStream(file).use { fileOutputStream ->
                        URL(picList[currentPosition]).openStream().use { inputStream ->
                            val buffer = ByteArray(10240)
                            var byteCount: Int
                            while (inputStream.read(buffer).also { byteCount = it } != -1) {
                                fileOutputStream.write(buffer, 0, byteCount)
                            }
                            sendBroadcast(
                                Intent(
                                    Intent.ACTION_MEDIA_SCANNER_SCAN_FILE,
                                    Uri.fromFile(file)
                                )
                            )
                            runOnUiThread {
                                ToastUtils.show(getString(R.string.save_pic_success))
                            }
                        }
                    }
                } catch (ex: IOException) {
                    runOnUiThread {
                        ToastUtils.show(getString(R.string.save_pic_fail))
                    }
                }
            }
        }.start()
    }

    override fun destroy() {
        picList.clear()
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