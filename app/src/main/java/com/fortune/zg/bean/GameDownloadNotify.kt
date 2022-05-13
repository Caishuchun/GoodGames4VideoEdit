package com.fortune.zg.bean

data class GameDownloadNotify(
    val gameIcon: String, //游戏icon
    val gameVideoId: Int, //游戏视频id
    val gameName: String, //游戏名字
    val gameSize: Long, //游戏大小
    val gameDownloadUrl: String, //游戏下载地址
    val gamePackageName: String, //游戏包名
)