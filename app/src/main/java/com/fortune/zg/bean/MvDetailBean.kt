package com.fortune.zg.bean

class MvDetailBean {
    private var code: Int? = null
    private var msg: String? = null
    private var data: DataBean? = null

    fun getCode(): Int? {
        return code
    }

    fun setCode(code: Int?) {
        this.code = code
    }

    fun getMsg(): String? {
        return msg
    }

    fun setMsg(msg: String?) {
        this.msg = msg
    }

    fun getData(): DataBean? {
        return data
    }

    fun setData(data: DataBean?) {
        this.data = data
    }

    class DataBean {
        var video_id: Int? = null
        var video_pos: Int? = null
        var user_name: String? = null
        var user_avatar: String? = null
        var video_desc: String? = null
        var video_name: String? = null
        var video_cover: String? = null
        var video_file: String? = null
        var video_update_time: String? = null
        var total_share: Int? = null
        var total_comment: Int? = null
        var total_like: Int? = null
        var total_view: Int? = null
        var is_like: Int? = null
        var video_cover_width: Int? = null
        var video_cover_height: Int? = null
        var game_url_short: String? = null
        var is_ad: Int? = null
        var platform: String? = null
        var game_info: GameInfoBean? = null
        var is_fav: Int? = null
        var game_gift: GameGiftBean? = null
        var video_type: Int? = null

        var videoList: List<VideoListBean>? = null
        var imageList: List<ImageListBean>? = null

        class GameGiftBean {
            var id: Int? = null
            var desc: String? = null
            var last: Int? = null
            var total: Int? = null
            var count_down: Int? = null
        }

        class GameInfoBean {
            var game_icon: String? = null
            var android_down_url: String? = null
            var android_package_name: String? = null
            var android_version_name: String? = null
            var android_version_number: String? = null
            var android_package_size: String? = null
            var ios_down_url: String? = null
            var pc_gift:String? = null
        }

        class VideoListBean {
            var index: String? = null
            var video_id: String? = null
        }

        class ImageListBean {
            var index: Int? = null
            var url: String? = null
        }
    }
}