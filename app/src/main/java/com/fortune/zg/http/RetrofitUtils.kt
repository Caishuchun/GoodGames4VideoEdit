package com.fortune.zg.http

import com.fortune.zg.base.BaseAppUpdateSetting
import com.fortune.zg.bean.*
import com.fortune.zg.constants.SPArgument
import com.fortune.zg.issue.MusicListBean
import com.fortune.zg.myapp.MyApp
import com.fortune.zg.newVideo.NewVideoListsBean
import com.fortune.zg.utils.LogUtils
import com.fortune.zg.utils.SPUtils
import io.reactivex.Flowable
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.RequestBody
import okhttp3.ResponseBody
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit


/**
 * Author: 蔡小树
 * Time: 2019/12/26 14:44
 * Description:
 */

object RetrofitUtils {

    private var retrofit: Retrofit? = null
    private var locale: String? = null
    var baseUrl = if (BaseAppUpdateSetting.appType) HttpUrls.REAL_URL else HttpUrls.TEST_URL
    private var client: OkHttpClient? = null

    init {
        val httpLoggingInterceptor = HttpLoggingInterceptor()
        if (LogUtils.isDebug) {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.BODY
        } else {
            httpLoggingInterceptor.level = HttpLoggingInterceptor.Level.NONE
        }
        client = OkHttpClient.Builder()
            .retryOnConnectionFailure(true)
            .connectTimeout(60 * 5, TimeUnit.SECONDS)
            .readTimeout(60 * 5, TimeUnit.SECONDS)
            .writeTimeout(60 * 5, TimeUnit.SECONDS)
            .addInterceptor(httpLoggingInterceptor)
            .addInterceptor {
                val request = it.request()
                val build = request.newBuilder()
                    .removeHeader("User-Agent")
                    .addHeader(
                        "User-Agent",
                        "HFDD-APP/Android/${
                            MyApp.getInstance().getVersion()
                        }${BaseAppUpdateSetting.patch}"
                    )
                    .addHeader(
                        "App-Version",
                        MyApp.getInstance().getVersion()
                    )
                    .addHeader("cookie", "locale=$locale")
                    .addHeader("Connection", "Upgrade, HTTP2-Settings")
                    .addHeader("Content-Type", "application/json")
                    .addHeader("Upgrade", "h2c")
                    .addHeader("Accept-Encoding", "identity")
                    .addHeader(
                        "Authorization",
                        "Bearer ${SPUtils.getString(SPArgument.LOGIN_TOKEN)}"
                    )
                    .build()
                return@addInterceptor it.proceed(build)
            }
            .build()
    }

    fun builder(): RetrofitImp {
        locale = "zh"
        retrofit = Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(client!!)
            .addConverterFactory(BaseGsonConverterFactory.create())
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .build()
        return retrofit!!.create(RetrofitImp::class.java)
    }

    interface RetrofitImp {
        /**
         * 一键登录
         */
        @FormUrlEncoded
        @POST(HttpUrls.QUICK_LOGIN_ALI)
        fun quickLogin4Ali(
            @Field("access_token", encoded = true) access_token: String
        ): Flowable<LoginBean>

        /**
         * 发送短信验证码
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_CODE)
        fun sendCode(
            @Field("phone", encoded = true) phone: String
        ): Flowable<BaseBean>

        /**
         * 短信验证码登录
         */
        @FormUrlEncoded
        @POST(HttpUrls.LOGIN)
        fun login(
            @Field("phone", encoded = true) phone: String,
            @Field("captcha", encoded = true) captcha: Int
        ): Flowable<LoginBean>

        /**
         * 获取用户信息
         */
        @GET(HttpUrls.USER_INFO)
        fun getUserInfo(): Flowable<UserInfoBean>

        /**
         * 更新用户头像
         */
        @Multipart
        @POST(HttpUrls.UPDATE_HEAD)
        fun updateUserHead(
            @Part image: MultipartBody.Part
        ): Flowable<UpdateUserHeadBean>

        /**
         * 更新用户信息
         */
        @FormUrlEncoded
        @POST(HttpUrls.UPDATE_USER_INFO)
        fun updateUserInfo(
            @Field("user_name", encoded = true) user_name: String,
            @Field("user_desc", encoded = true) user_desc: String,
            @Field("user_sex", encoded = true) user_sex: Int,
            @Field("user_birthday", encoded = true) user_birthday: String,
            @Field("avatar_path", encoded = true) avatar_path: String
        ): Flowable<BaseBean>

        /**
         * 获取游戏列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_LIST)
        fun gameList(
            @Field("type", encoded = true) type: Int,
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListWithMvBean>

        /**
         * 发送短信验证码_修改手机号
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEND_CODE_4_CHANGE_PHONE)
        fun sendCode4changePhone(
            @Field("phone", encoded = true) phone: String
        ): Flowable<BaseBean>

        /**
         * 修改手机号
         */
        @FormUrlEncoded
        @POST(HttpUrls.CHANGE_PHONE)
        fun changePhone(
            @Field("phone", encoded = true) phone: String,
            @Field("captcha", encoded = true) captcha: Int
        ): Flowable<BaseBean>

        /**
         * 检查版本更新
         */
        @GET(HttpUrls.CHECK_VERSION)
        fun checkVersion(): Flowable<VersionBean>

        /**
         * 获取热门搜索
         */
        @GET(HttpUrls.HOT_SEARCH)
        fun hotSearch(): Flowable<HotSearchBean>

        /**
         * 搜索建议
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEARCH_SUGREC)
        fun searchSugrec(
            @Field("wd", encoded = true) wd: String
        ): Flowable<HotSearchBean>

        /**
         * 搜索游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.SEARCH)
        fun search(
            @Field("wd", encoded = true) wd: String,
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListWithMvBean>

        /**
         * 游戏信息
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_INFO)
        fun gameInfo(
            @Field("game_id", encoded = true) game_id: Int
        ): Flowable<GameInfoBean>

        /**
         * 下载开始
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_DOWN_START)
        fun gameDownStart(
            @Field("game_id", encoded = true) game_id: Int
        ): Flowable<GameDownStartBean>

        /**
         * 下载完成
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_DOWN_COMPLETE)
        fun gameDownComplete(
            @Field("down_id", encoded = true) down_id: Int
        ): Flowable<BaseBean>

        /**
         * 安装完成
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_INSTALL_COMPLETE)
        fun gameInstallComplete(
            @Field("down_id", encoded = true) down_id: Int
        ): Flowable<BaseBean>

        /**
         * 获取签到列表
         */
        @GET(HttpUrls.DAILY_CHECK_LIST)
        fun dailyCheckList(): Flowable<DailyCheckListBean>

        /**
         * 签到
         */
        @GET(HttpUrls.DAILY_CHECK)
        fun dailyCheck(): Flowable<DailyCheckBean>

        /**
         * 升级_双倍领取的时候
         */
        @GET(HttpUrls.DOUBLE_GET)
        fun doubleGet(): Flowable<DailyCheckBean>

        /**
         * 限时白嫖_时段列表
         */
        @GET(HttpUrls.WHITE_PIAO_LIST)
        fun whitePiaoList(): Flowable<WhitePiaoListBean>

        /**
         * 限时白嫖_领取
         */
        @FormUrlEncoded
        @POST(HttpUrls.WHITE_PIAO)
        fun whitePiao(
            @Field("id", encoded = true) id: Int
        ): Flowable<DailyCheckBean>

        /**
         * 获取分享链接
         */
        @GET(HttpUrls.GET_SHARE_URL)
        fun getShareUrl(): Flowable<GetShareUrlBean>

        /**
         * 获取分享链接
         */
        @GET(HttpUrls.SHARE_FINISH)
        fun shareFinish(): Flowable<DailyCheckBean>

        /**
         * 获取邀请列表
         */
        @GET(HttpUrls.GET_SHARE_LIST)
        fun getShareList(): Flowable<GetShareListBean>

        /**
         * 领取邀请奖励
         */
        @FormUrlEncoded
        @POST(HttpUrls.GET_INVITE_GIFT)
        fun getInviteGift(
            @Field("id", encoded = true) id: Int
        ): Flowable<DailyCheckBean>

        /**
         * 小红点
         */
        @GET(HttpUrls.RED_POINT)
        fun redPoint(): Flowable<RedPointBean>

        /**
         * 礼包领取
         */
        @FormUrlEncoded
        @POST(HttpUrls.GET_GIFT_CODE)
        fun getGiftCode(
            @Field("game_id", encoded = true) game_id: Int,
            @Field("gift_id", encoded = true) gift_id: Int
        ): Flowable<GetGiftCodeBean>

        /**
         * 礼包领取
         */
        @FormUrlEncoded
        @POST(HttpUrls.GIFT_CODE_RECORDS)
        fun giftCodeRecords(
            @Field("game_id", encoded = true) game_id: Int
        ): Flowable<GiftCodeRecordsBean>

        /**
         * 所有人领取立即
         */
        @GET(HttpUrls.ALL_CODE_RECORDS)
        fun allCodeRecords(): Flowable<AllCodeRecordsBean>

        /**
         * 我的游戏
         */
        @GET
        fun myGames(@Url url: String): Flowable<MyGamesBean>

        /**
         * 视频列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_LIST)
        fun mvList(
            @Field("name", encoded = true) name: String,
            @Field("page", encoded = true) page: Int
        ): Flowable<MvListBean>

        /**
         * 视频分享
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_SHARE)
        fun mvShare(
            @Field("video_id", encoded = true) video_id: String
        ): Flowable<BaseBean>

        /**
         * 视频点赞
         * @param is_cancel 是否取消点赞，1：取消、0或者空则默认为确认点赞
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_LIKE)
        fun mvLike(
            @Field("video_id", encoded = true) video_id: String,
            @Field("is_cancel", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 视频详情
         * @param
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_INFO)
        fun mvInfo(
            @Field("video_id", encoded = true) video_id: String,
            @Field("video_pos", encoded = true) video_pos: String = ""
        ): Flowable<MvDetailBean>

        /**
         * 发送弹幕
         * @param
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_SEND_MSG)
        fun mvSendMsg(
            @Field("video_id", encoded = true) video_id: String,
            @Field("content", encoded = true) content: String,
            @Field("video_diff_time", encoded = true) video_diff_time: Int
        ): Flowable<BaseBean>

        /**
         * 获取弹幕
         * @param
         */
        @FormUrlEncoded
        @POST(HttpUrls.MV_GET_MSG)
        fun mvGetMsg(
            @Field("video_id", encoded = true) video_id: String,
            @Field("last_time", encoded = true) last_time: String?,
            @Field("video_diff_time", encoded = true) video_diff_time: Int
        ): Flowable<MvGetMsgBean>

        /**
         * 我的评论
         */
        @FormUrlEncoded
        @POST(HttpUrls.MY_COMMON)
        fun myCommon(
            @Field("video_id", encoded = true) video_id: Int
        ): Flowable<MyCommonBean>

        /**
         * 评论列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.COMMON_LIST)
        fun commonList(
            @Field("video_id", encoded = true) video_id: Int,
            @Field("page", encoded = true) page: Int
        ): Flowable<CommonListBean>

        /**
         * 回复评论列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.REPLAY_COMMON_LIST)
        fun replayCommonList(
            @Field("comment_id", encoded = true) comment_id: Int,
            @Field("page", encoded = true) page: Int
        ): Flowable<ReplayCommonListBean>

        /**
         * 提交评论
         */
        @FormUrlEncoded
        @POST(HttpUrls.SUBMIT_COMMON)
        fun submitCommon(
            @Field("video_id", encoded = true) video_id: Int,
            @Field("content", encoded = true) content: String
        ): Flowable<BaseBean>

        /**
         * 回复评论
         */
        @FormUrlEncoded
        @POST(HttpUrls.REPLAY_COMMON)
        fun replayCommon(
            @Field("comment_id", encoded = true) comment_id: Int,
            @Field("content", encoded = true) content: String
        ): Flowable<BaseBean>

        /**
         * 评论点赞/取消
         * @param type 类型，1：点赞，2：反对 ,相同类型，第一次请求表示新增，第二次再请求将会取消第一次操作
         */
        @FormUrlEncoded
        @POST(HttpUrls.COMMON_LIKE)
        fun commonLike(
            @Field("comment_id", encoded = true) comment_id: Int,
            @Field("type", encoded = true) type: Int = 1
        ): Flowable<BaseBean>

        /**
         * 上传图片
         */
        @Multipart
        @POST(HttpUrls.UPLOAD_PICTURE)
        fun uploadPicture(
            @Part file: MultipartBody.Part
        ): Flowable<UploadPictureBean>

        /**
         * 上传视频
         */
        @Multipart
        @POST(HttpUrls.UPLOAD_VIDEO)
        fun uploadVideo(
            @Part file: MultipartBody.Part,
            @Part("only_url") only_url: RequestBody
        ): Flowable<UploadVideoBean>

        /**
         * 直播详情
         */
        @FormUrlEncoded
        @POST(HttpUrls.LIVE_INFO)
        fun liveInfo(
            @Field("live_id", encoded = true) live_id: Int
        ): Flowable<LiveInfoBean>

        /**
         * 发布视频_读取配置
         */
        @GET(HttpUrls.GET_CONFIG)
        fun getConfig(): Flowable<GetConfigBean>

        /**
         * 发布视频_发布视频
         */
        @FormUrlEncoded
        @POST(HttpUrls.ISSUE_VIDEO)
        fun issueVideo(
            @Field("video_name") video_name: String,
            @Field("is_ad") is_ad: String,
            @Field("game_url") game_url: String,
            @Field("video_id") video_id: Int?,
            @Field("url") url: String,
            @Field("cover") cover: String,
            @Field("gift") gift: String
        ): Flowable<IssueVideoBean>

        /**
         * 发布视频_发布历史
         */
        @FormUrlEncoded
        @POST(HttpUrls.VIDEO_HIS)
        fun videoHis(
            @Field("page", encoded = true) page: Int
        ): Flowable<VideoHisBean>

        /**
         * 发布视频_发布历史
         */
        @FormUrlEncoded
        @POST(HttpUrls.DELETE_VIDEO)
        fun deleteVideo(
            @Field("video_id", encoded = true) video_id: Int
        ): Flowable<BaseBean>

        /**
         * 收藏/取消收藏游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.COLLECT_GAME)
        fun collectGame(
            @Field("game_id", encoded = true) game_id: Int,
            @Field("is_cancel", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 获取游戏收藏列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.COLLECT_LIST)
        fun collectList(
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListBean>

        /**
         * 获取视频id列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.VIDEO_ID_LIST)
        fun videoIdList(
            @Field("video_id", encoded = true) video_id: Int,
            @Field("type", encoded = true) type: Int
        ): Flowable<VideoIdListBean>

        /**
         * 收藏/取消收藏视频
         */
        @FormUrlEncoded
        @POST(HttpUrls.COLLECT_VIDEO)
        fun collectVideo(
            @Field("video_id", encoded = true) video_id: Int,
            @Field("is_cancel", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 获取游戏收藏列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.COLLECT_VIDEO_LIST)
        fun collectVideoList(
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListWithMvBean>

        /**
         * 获取礼包码
         */
        @FormUrlEncoded
        @POST(HttpUrls.VIDEO_GIFT)
        fun videoGift(
            @Field("video_pos", encoded = true) video_pos: Int,
            @Field("gift_id", encoded = true) gift_id: Int
        ): Flowable<GetGiftCodeBean>

        /**
         * 视频礼包领取记录
         */
        @FormUrlEncoded
        @POST(HttpUrls.VIDEO_GIFT_LIST)
        fun videoGiftRecords(
            @Field("video_pos", encoded = true) video_pos: Int
        ): Flowable<GiftCodeRecordsBean>

        /**
         * 获取游戏视频列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.VIDEO_LIST)
        fun VideoList(
            @Field("type", encoded = true) type: Int,
            @Field("page", encoded = true) page: Int
        ): Flowable<GameListWithMvBean>

        /**
         * 获取热门游戏列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.HOT_GAMES)
        fun hotGames(
            @Field("type", encoded = true) type: Int = 3,
            @Field("page", encoded = true) page: Int = 1
        ): Flowable<HotGamesBean>

        /**
         * 用户首页
         */
        @FormUrlEncoded
        @POST(HttpUrls.USER_HOME)
        fun userHome(
            @Field("video_id", encoded = true) video_id: Int
        ): Flowable<UserHomeBean>

        /**
         * 关注用户
         */
        @FormUrlEncoded
        @POST(HttpUrls.FOLLOW_USER)
        fun followUser(
            @Field("video_id", encoded = true) video_id: Int,
            @Field("is_cancel", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 关注列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.USER_FOLLOW_LIST)
        fun followList(
            @Field("user_id", encoded = true) user_id: Int
        ): Flowable<FansAndFollowLitBean>

        /**
         * 粉丝列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.USER_FANS_LIST)
        fun fansList(
            @Field("user_id", encoded = true) user_id: Int
        ): Flowable<FansAndFollowLitBean>

        /**
         * 配乐列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.MUSIC_LIST)
        fun musicList(
            @Field("name", encoded = true) name: String,
            @Field("page", encoded = true) page: Int
        ): Flowable<MusicListBean>

        /**
         * 收藏配乐
         */
        @FormUrlEncoded
        @POST(HttpUrls.FAV_MUSIC)
        fun favMusic(
            @Field("music_id", encoded = true) music_id: Int,
            @Field("is_cancel", encoded = true) is_cancel: Int
        ): Flowable<BaseBean>

        /**
         * 收藏列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.FAV_MUSIC_LIST)
        fun favMusicList(
            @Field("page", encoded = true) page: Int
        ): Flowable<MusicListBean>

        /**
         * 推荐配乐列表
         */
        @FormUrlEncoded
        @POST(HttpUrls.RECOMMEND_MUSIC_LIST)
        fun recommendMusicList(
            @Field("name", encoded = true) name: String = "",
            @Field("page", encoded = true) page: Int = 1
        ): Flowable<MusicListBean>

        /**
         * 文件下载
         */
        @GET
        fun downloadFile(@Url fileUrl: String?): Flowable<ResponseBody>

        /**
         * 下载开始_视频游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_DOWN_START_VIDEO)
        fun gameDownStart4Video(
            @Field("video_id", encoded = true) video_id: Int
        ): Flowable<GameDownStartBean>

        /**
         * 下载完成_视频游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_DOWN_COMPLETE_VIDEO)
        fun gameDownComplete4Video(
            @Field("down_id", encoded = true) down_id: Int
        ): Flowable<BaseBean>

        /**
         * 安装完成_视频游戏
         */
        @FormUrlEncoded
        @POST(HttpUrls.GAME_INSTALL_COMPLETE_VIDEO)
        fun gameInstallComplete4Video(
            @Field("down_id", encoded = true) down_id: Int
        ): Flowable<BaseBean>

        /**
         * 获取视频列表_V5
         */
        @FormUrlEncoded
        @POST(HttpUrls.GET_VIDEO_LIST)
        fun getVideoLists(
            @Field("type", encoded = true) type: Int = 1,
            @Field("page", encoded = true) page: Int = 1
        ): Flowable<NewVideoListsBean>

        /**
         * 上传视频播放进度
         */
        @FormUrlEncoded
        @POST(HttpUrls.UPLOAD_VIDEO_PROS)
        fun uploadVideoPro(
            @Field("video_id", encoded = true) video_id:Int,
            @Field("p5", encoded = true) p5:Int,
            @Field("p20", encoded = true) p20:Int,
            @Field("p50", encoded = true) p50:Int,
            @Field("p80", encoded = true) p80:Int,
            @Field("p100", encoded = true) p100:Int,
            @Field("p_over", encoded = true) p_over:Int,
            @Field("device_id", encoded = true) device_id: String
        ): Flowable<BaseBean>
    }
}