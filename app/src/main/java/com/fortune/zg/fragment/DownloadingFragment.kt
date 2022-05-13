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
import com.fortune.zg.bean.HotGamesBean
import com.fortune.zg.event.GameDownload
import com.fortune.zg.http.RetrofitUtils
import com.fortune.zg.utils.*
import com.fortune.zg.widget.SafeLinearLayoutManager
import com.google.gson.Gson
import com.jakewharton.rxbinding2.view.RxView
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_downloading.view.*
import kotlinx.android.synthetic.main.item_downloading.view.*
import kotlinx.android.synthetic.main.item_hot_game.view.*
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File
import java.text.DecimalFormat
import java.util.concurrent.TimeUnit

class DownloadingFragment : Fragment() {

    companion object {
        fun newInstance() = DownloadingFragment()
    }

    private var mView: View? = null
    private var mAdapter: BaseAdapterWithPosition<GameDownload>? = null
    private var mData = mutableListOf<GameDownload>()
    private var mAdapter4HotGames: BaseAdapterWithPosition<HotGamesBean.Data.Game>? = null
    private var mData4HotGames = mutableListOf<HotGamesBean.Data.Game>()
    private var hotGamesObservable: Disposable? = null
    private var numberFormat = DecimalFormat("#0.00")

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        mView = inflater.inflate(R.layout.fragment_downloading, container, false)
        EventBus.getDefault().register(this)
        initView()
        toGetHotGame()
        return mView
    }

    /**
     * 获取热门游戏
     */
    private fun toGetHotGame() {
        val hotGames = RetrofitUtils.builder().hotGames()
        hotGamesObservable = hotGames.subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe({
                DialogUtils.dismissLoading()
                LogUtils.d("${javaClass.simpleName}=success=>${Gson().toJson(it)}")
                if (it != null) {
                    when (it.code) {
                        1 -> {
                            val list = it.data.list
                            if (list.isNotEmpty()) {
                                mView?.tv_downloading_hot?.visibility = View.VISIBLE
                                if (list.size > 6) {
                                    mData4HotGames.addAll(list.subList(0, 6))
                                } else {
                                    mData4HotGames.addAll(list)
                                }
                                mAdapter4HotGames?.notifyDataSetChanged()
                            }
                        }
                    }
                }
            }, {
                LogUtils.d("${javaClass.simpleName}=fail=>${it.message.toString()}")
            })
    }

    @SuppressLint("CheckResult", "SetTextI18n")
    private fun initView() {
        RxView.clicks(mView!!.tv_downloading_toHome)
            .throttleFirst(200, TimeUnit.MILLISECONDS)
            .subscribe {
                (activity as DownloadActivity).toMainHome()
            }

        mAdapter = BaseAdapterWithPosition.Builder<GameDownload>()
            .setLayoutId(R.layout.item_downloading)
            .setData(mData)
            .addBindView { itemView, itemData, position ->
                val percent = itemData.task?.percent
                val extendField = itemData.task?.extendField
                val data = Gson().fromJson(extendField, GameDownloadNotify::class.java)
                itemView.tv_item_downloading_title.text = "${data.gameName}   $percent%"
                itemView.pb_item_downloading.progress = percent!!

                Glide.with(this)
                    .load(data.gameIcon)
                    .into(itemView.iv_item_downloading_icon)

                itemView.iv_item_downloading_pause.setImageResource(
                    if (itemData.task.isRunning) R.mipmap.download_pause else R.mipmap.download_start
                )

                val taskId = SPUtils.getLong("TASK_ID_${data.gameVideoId}", -1L)
                RxView.clicks(itemView.iv_item_downloading_pause)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        if (itemData.task.isRunning) {
                            Aria.download(this)
                                .load(taskId)
                                .stop()
                        } else {
                            Aria.download(this)
                                .load(taskId)
                                .setExtendField(extendField)
                                .resume()
                        }
                        itemView.iv_item_downloading_pause.setImageResource(
                            if (itemData.task.isRunning) R.mipmap.download_pause else R.mipmap.download_start
                        )
                    }
                RxView.clicks(itemView.iv_item_downloading_cancel)
                    .throttleFirst(200, TimeUnit.MILLISECONDS)
                    .subscribe {
                        DialogUtils.showDefaultDialog(activity as DownloadActivity,
                            "取消下载",
                            "确定要取消${data.gameName}下载吗?",
                            "暂不取消",
                            "取消下载",
                            object : DialogUtils.OnDialogListener {
                                override fun next() {
                                    Aria.download(this)
                                        .load(taskId)
                                        .cancel(true)
                                }
                            })
                    }
            }
            .create()
        mView?.rv_downloading_game?.adapter = mAdapter
        mView?.rv_downloading_game?.layoutManager =
            SafeLinearLayoutManager(activity as DownloadActivity)

        mAdapter4HotGames = BaseAdapterWithPosition.Builder<HotGamesBean.Data.Game>()
            .setLayoutId(R.layout.item_hot_game)
            .setData(mData4HotGames)
            .addBindView { itemView, itemData, position ->
                if (itemData.game_info.game_icon.isNullOrEmpty()) {
                    itemView.iv_item_hotGame_icon.setImageResource(R.mipmap.icon)
                } else {
                    Glide.with(this)
                        .load(itemData.game_info.game_icon)
                        .placeholder(R.mipmap.icon)
                        .into(itemView.iv_item_hotGame_icon)
                }
                itemView.tv_item_hotGame_title.text = itemData.video_name
                itemView.tv_item_hotGame_size.text =
                    "${numberFormat.format(itemData.game_info.android_package_size.toFloat() / 1024.0 / 1024.0)}M"

                if (installApk(itemData.game_info.android_package_name)) {
                    itemView.tv_item_hotGame_install.text = "打开"
                    val androidDownUrl = itemData.game_info.android_package_name
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
                        if (installApk(itemData.game_info.android_package_name)) {
                            val launchIntentForPackage =
                                (activity as DownloadActivity).packageManager.getLaunchIntentForPackage(
                                    itemData.game_info.android_package_name
                                )
                            (activity as DownloadActivity).startActivity(
                                launchIntentForPackage
                            )
                        } else {
                            val androidDownUrl = itemData.game_info.android_down_url
                            val lastIndexOf = androidDownUrl.lastIndexOf("/")
                            val fileName = androidDownUrl.substring(
                                lastIndexOf + 1,
                                androidDownUrl.length
                            )
                            val dirPath =
                                (activity as DownloadActivity).getExternalFilesDir(
                                    Environment.DIRECTORY_DOWNLOADS
                                ).toString()
                            val downloadPath = "$dirPath/$fileName"
                            if (downloadPath != null && File(downloadPath).isFile &&
                                Math.abs(File(downloadPath).length() - itemData.game_info.android_package_size.toInt()) < 100
                            ) {
                                toInstallGame(downloadPath)
                            } else {
                                val taskId = Aria.download(this)
                                    .load(itemData.game_info.android_down_url)
                                    .setFilePath(downloadPath, true) //设置文件保存的完整路径
                                    .ignoreFilePathOccupy()
                                    .ignoreCheckPermissions()
                                    .setExtendField(
                                        makeJson(
                                            itemData.video_id,
                                            itemData.video_name,
                                            itemData.game_info.game_icon,
                                            itemData.game_info.android_package_size.toLong(),
                                            itemData.game_info.android_down_url,
                                            itemData.game_info.android_package_name
                                        )
                                    )
                                    .create()
                                SPUtils.putValue("TASK_ID_${itemData.video_id}", taskId)
                            }
                        }
                    }
            }
            .create()
        mView?.rv_downloading_hot?.adapter = mAdapter4HotGames
        mView?.rv_downloading_hot?.layoutManager =
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
     * 游戏是否安装
     */
    private fun installApk(game_package_name: String) = InstallApkUtils.isInstallApk(
        activity as DownloadActivity,
        game_package_name
    )


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

    @SuppressLint("NotifyDataSetChanged")
    @Subscribe(threadMode = ThreadMode.MAIN, sticky = true)
    fun downloading(gameDownload: GameDownload) {
        LogUtils.d("${javaClass.simpleName}=>downloading")
        when (gameDownload.state) {
            GameDownload.STATE.RUNNING -> {
                if (mData.size == 0) {
                    mData.add(gameDownload)
                } else {
                    var isUpdate = false
                    for (index in 0 until mData.size) {
                        if (mData[index].task?.filePath == gameDownload.task?.filePath) {
                            //证明是同一个,更新一下
                            isUpdate = true
                            mData[index] = gameDownload
                        }
                    }
                    if (!isUpdate) {
                        mData.add(gameDownload)
                    }
                }
            }
            GameDownload.STATE.PAUSE -> {
                if (mData.size == 0) {
                    mData.add(gameDownload)
                } else {
                    var isUpdate = false
                    for (index in 0 until mData.size) {
                        if (mData[index].task?.filePath == gameDownload.task?.filePath) {
                            //证明是同一个,更新一下
                            isUpdate = true
                            mData[index] = gameDownload
                        }
                    }
                    if (!isUpdate) {
                        mData.add(gameDownload)
                    }
                }
            }
            GameDownload.STATE.CANCEL -> {
                if (mData.size > 0) {
                    for (data in mData) {
                        if (data.task?.filePath == gameDownload.task?.filePath) {
                            //证明是同一个
                            mData.remove(data)
                        }
                    }
                }
            }
            GameDownload.STATE.COMPLETE -> {
                if (mData.size > 0) {
                    for (data in mData) {
                        if (data.task?.filePath == gameDownload.task?.filePath) {
                            //证明是同一个
                            mData.remove(data)
                        }
                    }
                }
            }
        }
        if (mData.size == 0) {
            mView?.ll_downloading_nothing?.visibility = View.VISIBLE
            mView?.rv_downloading_game?.visibility = View.GONE
        } else {
            mView?.ll_downloading_nothing?.visibility = View.GONE
            mView?.rv_downloading_game?.visibility = View.VISIBLE
        }
        mAdapter?.notifyDataSetChanged()
    }

    override fun onDestroy() {
        EventBus.getDefault().unregister(this)
        hotGamesObservable?.dispose()
        hotGamesObservable = null
        super.onDestroy()
    }
}