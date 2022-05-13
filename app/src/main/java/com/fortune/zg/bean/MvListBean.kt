package com.fortune.zg.bean

class MvListBean {
    private var code: Int? = null
    private var msg: String? = null
    private var data: DataBean? = null

    companion object {
        private var mDate: DataBean? = null
        fun getData() = mDate
        fun setData(data: DataBean) {
            this.mDate = data
        }

        fun clear() {
            this.mDate = null
        }
    }


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
        var list: List<ListBean>? = null
        var paging: PagingBean? = null

        class PagingBean {
            var page: Int? = null
            var limit: Int? = null
            var count: Int? = null
        }

        class ListBean {
            var video_id: Int? = null
            var user_name: String? = null
            var user_avatar: String? = null
            var video_name: String? = null
            var video_desc: String? = null
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
            var videoList: List<VideoListBean>? = null
            var imageList: List<ImageListBean>? = null

            class VideoListBean {
                var index: Int? = null
                var video_id: String? = null
            }

            class ImageListBean {
                var index: Int? = null
                var url: String? = null
            }
        }
    }
}