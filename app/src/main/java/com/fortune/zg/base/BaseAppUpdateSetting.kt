package com.fortune.zg.base

/**
 * Author: 蔡小树
 * Time: 2021/1/13 9:54
 * Description:
 */

object BaseAppUpdateSetting {
    /**
     * 是否打印日志
     * @param true 打印debug的日志(平常测试的时候)
     * @param false 不打印(上正式区发布的时候)
     */
    const val isDebug = true

    /**
     * 测试区或者正式区
     * @param true 正式区
     * @param false 测试区
     */
    const val appType = true

    /**
     * 网络请求标志, 热更包版本
     * @param patch ""表示基础版本,".1"/".11"表示热更版本
     */
    const val patch = ""
}