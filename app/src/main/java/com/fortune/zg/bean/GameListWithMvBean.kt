package com.fortune.zg.bean

class GameListWithMvBean {
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
        var list: List<ListBean>? = null
        var paging: PagingBean? = null

        class PagingBean {
            var page: Int? = null
            var limit: Int? = null
            var count: Int? = null
        }

        class ListBean {
            //直播
            var live_id: Int? = null
            var anchor_name: String? = null
            var intro: String? = null
            var tag: String? = null
            var status: Int? = null
            var im_group_id: Long? = null
            var cover: String? = null
            var online_user: Int? = null

            //视频
            var video_id: Int? = null
            var video_name: String? = null
            var video_desc: String? = null
            var video_cover: String? = null
            var video_cover_width: Int? = null
            var video_cover_height: Int? = null
            var video_file: String? = null
            var video_update_time: String? = null
            var total_share: Int? = null
            var total_comment: Int? = null
            var total_like: Int? = null
            var total_view: Int? = null

            //分类
            var item_type: String? = null

            //游戏
            var game_id: Int? = null
            var game_name: String? = null
            var game_desc: String? = null
            var game_tag: List<String>? = null
            var game_cover: String? = null
            var game_system: Int? = null
            var game_badge: String? = null
            var game_update_time: Int? = null
            var game_gift_last: Int? = null
            var game_hits: Int? = null
            var videoList: List<MvListBean.DataBean.ListBean.VideoListBean>? = null
            var imageList: List<MvListBean.DataBean.ListBean.ImageListBean>? = null


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