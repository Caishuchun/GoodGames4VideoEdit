package com.fortune.zg.bean

class GameInfoBean {
    /**
     * code : 1
     * msg : success
     * data : {"game_id":1,"game_name":"热血火龙","game_desc":"游戏简介","game_tag":["标签1","标签2","标签3"],"game_icon":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/icon/icon.png","game_pic":["http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic1.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic2.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic3.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic14.jpg"],"game_video":[{"cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4?x-oss-process=video/snapshot,t_1,f_jpg","url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4"},{"cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video2.mp4?x-oss-process=video/snapshot,t_1,f_jpg","url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video2.mp4"}],"game_hits":0,"game_system":7,"game_version":"1.0.0","game_down_info":{"windows_domain":"windows.com","android_package_name":"com.miui.huanji","android_package_size":18782779,"android_version_name":"3.0.8","android_version_number":1,"android_down_url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/apk/com.miui.huanji.apk","ios_down_url":"https://ios.com"},"game_update_log":"","game_update_time":1611241297}
     */
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
        /**
         * game_id : 1
         * game_name : 热血火龙
         * game_desc : 游戏简介
         * game_tag : ["标签1","标签2","标签3"]
         * game_icon : http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/icon/icon.png
         * game_pic : ["http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic1.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic2.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic3.jpg","http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/pic/pic14.jpg"]
         * game_video : [{"cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4?x-oss-process=video/snapshot,t_1,f_jpg","url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4"},{"cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video2.mp4?x-oss-process=video/snapshot,t_1,f_jpg","url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video2.mp4"}]
         * game_hits : 0
         * game_system : 7
         * game_version : 1.0.0
         * game_down_info : {"windows_domain":"windows.com","android_package_name":"com.miui.huanji","android_package_size":18782779,"android_version_name":"3.0.8","android_version_number":1,"android_down_url":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/apk/com.miui.huanji.apk","ios_down_url":"https://ios.com"}
         * game_update_log :
         * game_update_time : 1611241297
         */
        var game_id: Int? = null
        var game_name: String? = null
        var game_desc: String? = null
        var game_tag: List<String>? = null
        var game_icon: String? = null
        var game_pic: List<String>? = null
        var game_video: List<GameVideoBean>? = null
        var game_hits: Int? = null
        var game_system: Int? = null
        var game_version: String? = null
        var game_down_info: GameDownInfoBean? = null
        var game_update_log: String? = null
        var game_update_time: Int? = null
        var game_gift_info: GameGiftInfoBean? = null
        var game_integral_exchange: GameIntegralExchangeBean? = null
        var game_gift: GameGiftBean? = null
        var is_fav: String? = null

        class GameDownInfoBean {
            /**
             * windows_domain : windows.com
             * android_package_name : com.miui.huanji
             * android_package_size : 18782779
             * android_version_name : 3.0.8
             * android_version_number : 1
             * android_down_url : http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/apk/com.miui.huanji.apk
             * ios_down_url : https://ios.com
             */
            var windows_domain: String? = null
            var android_package_name: String? = null
            var android_package_size: Int? = null
            var android_version_name: String? = null
            var android_version_number: Int? = null
            var android_down_url: String? = null
            var ios_down_url: String? = null
        }

        class GameVideoBean {
            /**
             * cover : http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4?x-oss-process=video/snapshot,t_1,f_jpg
             * url : http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/video/video1.mp4
             */
            var cover: String? = null
            var url: String? = null
        }

        class GameGiftInfoBean {
            var gift_id: Int? = null
            var start_time: Int? = null
            var count_down: Int? = null
            var money: String? = null
            var last: Int? = null
            var total: Int? = null
        }

        class GameIntegralExchangeBean {
            var money: String? = null
            var integral: Int? = null
            var limit: Int? = null
        }

        class GameGiftBean {
            var id: Int? = null
            var desc: String? = null
            var last: Int? = null
            var total: Int? = null
            var count_down: Int? = null
        }
    }
}