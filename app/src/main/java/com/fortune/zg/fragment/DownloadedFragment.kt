package com.fortune.zg.fragment

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import com.arialyy.aria.core.Aria
import com.bumptech.glide.Glide
import com.fortune.zg.R
import com.fortune.zg.activity.DownloadActivity
import com.fortune.zg.adapter.BaseAdapterWithPosition
import com.fortune.zg.bean.GameDownloadNotify
import com.fortune.zg.room.DownloadGame
import com.fortune.zg.room.DownloadGameDataBase
import com.fortune.zg.utils.DeleteApkUtils
import com.fortune.zg.utils.InstallApkUtils
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.SPUtils
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import kotlinx.android.synthetic.main.fragment_downloaded.view.*
import kotlinx.android.synthetic.main.item_hot_game.view.*
import java.io.File
import java.text.DecimalFormat
import java.util.*
import java.util.concurrent.TimeUnit

class DownloadedFragment : Fragment() {

    companion object {
        fun newInstance() = DownloadedFragment()
    }

    private var mAdapter: BaseAdapterWithPosition<DownloadGame>? = null
    private var numberFormat = DecimalFormat("#0.00")

    private var mView: View? = null
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_downloaded, container, false)
        initView()
        return mView
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        RxView.clicks(mView!!.tv_downloaded_toHome)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                (activity as DownloadActivity).toMainHome()
            }

        val dataBase = DownloadGameDataBase.getDataBase(activity as DownloadActivity)
        val downloadGameDao = dataBase.downloadGameDao()
        val all = downloadGameDao.all
        LogUtils.d("DownloadGame=>${Gson().toJson(all)}")
        if (all.isEmpty()) {
            mView?.ll_downloaded_nothing?.visibility = View.VISIBLE
        } else {
            mView?.ll_downloaded_nothing?.visibility = View.GONE
        }

        mAdapter = BaseAdapterWithPosition.Builder<DownloadGame>()
            .setLayoutId(R.layout.item_hot_game)
            .setData(all)
            .addBindView { itemView, itemData, position ->
                if (itemData.game_icon.isNullOrEmpty()) {
                    itemView.iv_item_hotGame_icon.setImageResource(R.mipmap.icon)
                } else {
                    Glide.with(this)
                        .load(itemData.game_icon)
                        .into(itemView.iv_item_hotGame_icon)
                }
                itemView.tv_item_hotGame_title.text = itemData.video_name
                itemView.tv_item_hotGame_size.text =
                    "${numberFormat.format(itemData.game_size.toFloat() / 1024.0 / 1024.0)}M"

                if (installApk(itemData.game_package_name)) {
                    itemView.tv_item_hotGame_install.text = "打开"

                    val androidDownUrl = itemData.game_download_url
                    val lastIndexOf = androidDownUrl.lastIndexOf("/")
                    val fileName =
                        androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
                    val dirPath =
                        (activity as DownloadActivity).getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                            .toString()
                    val downloadPath = "$dirPath/$fileName"
                    DeleteApkUtils.deleteApk(File(downloadPath))
                } else {
                    itemView.tv_item_hotGame_install.text = "安装"
                }
                RxView.clicks(itemView.tv_item_hotGame_install)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (installApk(itemData.game_package_name)) {
                            val launchIntentForPackage =
                                (activity as DownloadActivity).packageManager.getLaunchIntentForPackage(
                                    itemData.game_package_name
                                )
                            (activity as DownloadActivity).startActivity(launchIntentForPackage)
                        } else {
                            val androidDownUrl = itemData.game_download_url
                            val lastIndexOf = androidDownUrl.lastIndexOf("/")
                            val fileName =
                                androidDownUrl.substring(lastIndexOf + 1, androidDownUrl.length)
                            val dirPath =
                                (activity as DownloadActivity).getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
                                    .toString()
                            val downloadPath = "$dirPath/$fileName"
                            if (downloadPath != null && File(downloadPath).isFile &&
                                Math.abs(File(downloadPath).length() - itemData.game_size) < 100
                            ) {
                                toInstallGame(downloadPath)
                            } else {
                                val taskId = Aria.download(this)
                                    .load(itemData.game_download_url)
                                    .setFilePath(downloadPath, true) //设置文件保存的完整路径
                                    .ignoreFilePathOccupy()
                                    .ignoreCheckPermissions()
                                    .setExtendField(
                                        makeJson(
                                            itemData.video_id,
                                            itemData.video_name,
                                            itemData.game_icon,
                                            itemData.game_size,
                                            itemData.game_download_url,
                                            itemData.game_package_name
                                        )
                                    )
                                    .create()
                                SPUtils.putValue("TASK_ID_${itemData.video_id}", taskId)
                                val timer = Timer()
                                val timerTask = object : TimerTask() {
                                    override fun run() {
                                        (activity as DownloadActivity).toDownloadingFragment()
                                    }
                                }
                                timer.schedule(timerTask, 1000)
                            }
                        }
                    }
            }
            .create()
        mView?.rv_downloaded_game?.adapter = mAdapter
        mView?.rv_downloaded_game?.layoutManager =
            SafeLinearLayoutManager(activity as DownloadActivity)
    }


    /**
     * 安装游戏
     */
    private fun toInstallGame(filePath: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
        val file = File(filePath)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            val apkUri =
                FileProvider.getUriForFile(
                    activity as DownloadActivity,
                    "${(activity as DownloadActivity).packageName}.provider",
                    file
                )
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            intent.setDataAndType(apkUri, "application/vnd.android.package-archive")
        } else {
            intent.setDataAndType(
                Uri.fromFile(file),
                "application/vnd.android.package-archive"
            )
        }
        startActivity(intent)
    }

    /**
     * 创建下载时使用的额为信息
     */
    private fun makeJson(
        videoId: Int,
        gameName: String,
        gameIcon: String,
        gameSize: Long,
        gameDownloadUrl: String,
        gamePackageName: String
    ): String {
        val gameDownloadNotify =
            GameDownloadNotify(
                gameIcon,
                videoId,
                gameName,
                gameSize,
                gameDownloadUrl,
                gamePackageName
            )
        return Gson().toJson(gameDownloadNotify)
    }

    /**
     * 游戏是否安装
     */
    private fun installApk(game_package_name: String) = InstallApkUtils.isInstallApk(
        activity as DownloadActivity,
        game_package_name
    )
}