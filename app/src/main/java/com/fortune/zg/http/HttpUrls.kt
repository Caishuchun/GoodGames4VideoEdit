package com.fortune.zg.http

/**
 * Author: 蔡小树
 * Time: 2020/5/13 上午 10:18
 * Description:
 */

object HttpUrls {

    const val TEST_URL = "http://test.hfdd.top/"       //测试区
    const val REAL_URL = "https://gm.10f.top/"       //正式区

    //一键登录
    const val QUICK_LOGIN_ALI = "/android/login/aliyunOneClick"//新版

    //登录发送短信验证码
    const val SEND_CODE = "/android/login/sendCode"

    //短信验证码登录
    const val LOGIN = "/android/login/login"

    //获取用户信息
    const val USER_INFO = "/android/user/info"

    //上传头像
    const val UPDATE_HEAD = "/android/user/uploadAvatar"

    //更新用户信息
    const val UPDATE_USER_INFO = "/android/user/update"

    //获取游戏列表数据
    const val GAME_LIST = "/android/game/list"

    //修改手机号的短信验证码
    const val SEND_CODE_4_CHANGE_PHONE = "/android/user/sendCode"

    //修改手机号
    const val CHANGE_PHONE = "/android/user/updatePhone"

    //版本检查
    const val CHECK_VERSION = "/android/update/check"

    //获取热门搜索
    const val HOT_SEARCH = "/android/game/hotSearch"

    //搜索建议
    const val SEARCH_SUGREC = "/android/game/sugrec"

    //游戏搜索
    const val SEARCH = "/android/game/search"

    //游戏信息
    const val GAME_INFO = "/android/game/read"

    //游戏下载_下载开始
    const val GAME_DOWN_START = "/android/report/gameDownStart"

    //游戏下载_下载完成
    const val GAME_DOWN_COMPLETE = "/android/report/gameDownComplete"

    //游戏下载_安装完成
    const val GAME_INSTALL_COMPLETE = "/android/report/gameInstallComplete"

    //签到列表
    const val DAILY_CHECK_LIST = "/android/activity.dailyClockIn/list"

    //点击签到
    const val DAILY_CHECK = "/android/activity.dailyClockIn/submit"

    //升级(当点击了double之后)
    const val DOUBLE_GET = "/android/activity.share/upgrade"

    //白嫖时段
    const val WHITE_PIAO_LIST = "/android/activity.limitTime/list"

    //白嫖领取
    const val WHITE_PIAO = "/android/activity.limitTime/receive"

    //获取分享链接
    const val GET_SHARE_URL = "/android/activity.invite/getShareUrl"

    //分享完成
    const val SHARE_FINISH = "/android/activity.invite/shareFinish"

    //获取分享列表
    const val GET_SHARE_LIST = "/android/activity.invite/list"

    //领取邀请奖励
    const val GET_INVITE_GIFT = "/android/activity.invite/receive"

    //小红点
    const val RED_POINT = "/android/user/redDot"

    //礼包领取记录
    const val GIFT_CODE_RECORDS = "/android/game/giftReceiveRecordV2"

    //礼包领取
    const val GET_GIFT_CODE = "/android/game/giftReceiveV2"

    //所有人的领取记录
    const val ALL_CODE_RECORDS = "/android/game/allGiftReceiveRecord"

    //我的游戏_PC
    const val MY_GAMES_PC = "/android/user/games"

    //我的游戏_PHONE
    const val MY_GAMES_PHONE = "/android/user/downRecord"

    //视频列表
    const val MV_LIST = "/android/video/list"

    //视频分享
    const val MV_SHARE = "/android/video/share"

    //视频点赞
    const val MV_LIKE = "/android/video/like"

    //视频详情
//    const val MV_INFO = "/android/video/read"
    const val MV_INFO = "/android/game/videoRead"

    //发送弹幕
    const val MV_SEND_MSG = "/android/video/bulletSubmit"

    //获取弹幕
    const val MV_GET_MSG = "/android/video/bulletList"

    //我的评论
    const val MY_COMMON = "/android/comment/my"

    //评论列表
    const val COMMON_LIST = "/android/comment/list"

    //回复的列表
    const val REPLAY_COMMON_LIST = "/android/comment/replyList"

    //提交评论
    const val SUBMIT_COMMON = "/android/comment/submit"

    //回复评论
    const val REPLAY_COMMON = "/android/comment/replySubmit"

    //评论点赞/取消
    const val COMMON_LIKE = "/android/comment/operate"

    //上传图片
    const val UPLOAD_PICTURE = "/android/comment/uploadPic"

    //上传视频
    const val UPLOAD_VIDEO = "/android/comment/uploadVideo"

    //直播详情
    const val LIVE_INFO = "/android/live/read"

    //发布广告视频_读取配置
    const val GET_CONFIG = "/android/video/config"

    //发布广告视频_发布视频
    const val ISSUE_VIDEO = "/android/video/upload"

    //发布广告视频_发布历史
    const val VIDEO_HIS = "/android/video/my"

    //发布广告视频_发布历史删除视频
    const val DELETE_VIDEO = "/android/video/remove"

    //收藏/取消收藏游戏
    const val COLLECT_GAME = "/android/game/fav"

    //游戏收藏列表
    const val COLLECT_LIST = "/android/game/myFav"

    //获取视频id列表
    const val VIDEO_ID_LIST = "/android/video/position"

    //视频收藏
    const val COLLECT_VIDEO = "/android/video/fav"

    //视频收藏历史
    const val COLLECT_VIDEO_LIST = "/android/video/favList"

    //视频礼包
    const val VIDEO_GIFT = "/android/game/videoGiftReceive"

    //视频礼包记录
    const val VIDEO_GIFT_LIST = "/android/game/videoGiftReceiveRecord"

    //获取游戏视频列表
    const val VIDEO_LIST = "/android/game/videoList"

    //获取热门游戏
    const val HOT_GAMES = "/android/game/videoHot"

    //用户主页
    const val USER_HOME = "/android/user/index"

    //关注/取消关注
    const val FOLLOW_USER = "/android/user/follow"

    //用户关注列表
    const val USER_FOLLOW_LIST = "/android/user/follow_list"

    //用户粉丝列表
    const val USER_FANS_LIST = "/android/user/fan_list"

    //配乐列表
    const val MUSIC_LIST = "/android/music/list"

    //收藏配乐
    const val FAV_MUSIC = "/android/music/fav"

    //收藏配乐列表
    const val FAV_MUSIC_LIST = "/android/music/favList"

    //推荐音乐列表
    const val RECOMMEND_MUSIC_LIST = "/android/music/recommend"

    //游戏下载_下载开始_视频浏览页下载
    const val GAME_DOWN_START_VIDEO = "/android/report/videoDownStart"

    //游戏下载_下载完成_视频浏览页下载
    const val GAME_DOWN_COMPLETE_VIDEO = "/android/report/videoDownComplete"

    //游戏下载_安装完成_视频浏览页下载
    const val GAME_INSTALL_COMPLETE_VIDEO = "/android/report/videoInstallComplete"

    //获取视频列表
    const val GET_VIDEO_LIST = "/android/game/videoList2"

    //上传视频进度
    const val UPLOAD_VIDEO_PROS = "/android/report/videoPlayProgress"
}