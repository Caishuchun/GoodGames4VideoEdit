package com.fortune.zg.bean

data class UploadVideo4ServiceBean(
    var text: String?,//文本
    var videoPath: String,//视频地址
    var videoId: String?,//视频Id
    var commonId: String?//评论Id
)