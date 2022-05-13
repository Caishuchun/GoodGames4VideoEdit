package com.fortune.zg.utils

import android.app.Activity
import android.content.Intent
import com.fortune.zg.activity.CommentDetailActivityV4
import com.fortune.zg.activity.MainActivityV5
import com.fortune.zg.activity.SplashActivity
import com.fortune.zg.bean.RedPointBean
import com.fortune.zg.bean.RoleInfoBean
import com.fortune.zg.bean.UserInfoBean
import com.fortune.zg.bean.VersionBean
import com.fortune.zg.event.LoginStatusChange
import com.fortune.zg.room.*
import com.fortune.zg.video.VideoActivity
import com.luck.picture.lib.tools.PictureFileUtils
import org.greenrobot.eventbus.EventBus
import kotlin.system.exitProcess

/**
 * Author: 蔡小树
 * Time: 2020/4/14 9:42
 * Description: Activity管理工具类
 */

object ActivityManager {

    private var activities = mutableListOf<Activity>()

    /**
     * 添加Activity到集合中
     */
    fun addActivity(activity: Activity) {
        if (activity is VideoActivity || activity is CommentDetailActivityV4) {
            //如果是这两个,反正肯定只有一个,删完就完事
            loop@ for (index in 0 until activities.size) {
//                LogUtils.d("ActivityManager=>activity:$activity,index:$index,activityInList:${activities[index]}")
                if (activity.javaClass.simpleName == activities[index].javaClass.simpleName) {
                    activities[index].finish()
                    activities.remove(activities[index])
                    break@loop
                }
            }
        }
        activities.add(activity)
    }

    /**
     * 从集合中移除Activity
     */
    fun removeActivity(activity: Activity) {
        if (activities.contains(activity)) {
            activities.remove(activity)
        }
    }

    /**
     * 跳转到主界面
     */
    fun toMainActivity() {
        val stepList = mutableListOf<Activity>()
        for (index in 0 until activities.size) {
            if (activities[index] != MainActivityV5.getInstance()) {
                activities[index].finish()
                stepList.add(activities[index])
            }
        }
        for (activity in stepList) {
            activities.remove(activity)
        }
    }

    /**
     * 退到起始页面
     */
    fun toSplashActivity(activity: Activity) {
        UserInfoBean.clear()
        RoleInfoBean.clear()
        VersionBean.clear()
        RedPointBean.clear()
        SPUtils.clear()
        AccountDataBase.getDataBase(activity).accountDao().deleteAll()
        LookHisDataBase.getDataBase(activity).lookHisDao().deleteAll()
        LocalGameDateBase.getDataBase(activity).localGameDao().deleteAll()
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        PictureFileUtils.deleteAllCacheDirFile(activity)
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        val intent = Intent(activity, SplashActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_NEW_TASK
        activity.startActivity(intent)
        activity.finish()
    }

    /**
     * 退出登录
     */
    fun exitLogin(activity: MainActivityV5) {
        UserInfoBean.clear()
        RoleInfoBean.clear()
        VersionBean.clear()
        RedPointBean.clear()
        SPUtils.clear()
        AccountDataBase.getDataBase(activity).accountDao().deleteAll()
        LookHisDataBase.getDataBase(activity).lookHisDao().deleteAll()
        LocalGameDateBase.getDataBase(activity).localGameDao().deleteAll()
        SearchHisDataBase.getDataBase(activity).searchHisDao().deleteAll()
        PictureFileUtils.deleteAllCacheDirFile(activity)
        val glideCacheUtil = GlideCacheUtil()
        glideCacheUtil.clearImageAllCache(activity)
        EventBus.getDefault().postSticky(LoginStatusChange(false))
    }

    /**
     * 退出app
     */
    fun exit() {
        exitProcess(0)
    }

}