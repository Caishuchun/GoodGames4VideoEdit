package com.fortune.zg.bean

class GameListBean {

    /**
     * code : 1
     * msg : success
     * data : {"list":[{"game_id":2,"game_name":"热血传奇","game_desc":"游戏简介2","game_tag":["标签1","标签2","标签3"],"game_cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/cover/cover.jpg","game_hits":0,"game_system":7,"game_badge":"new","game_update_time":1611241297},{"game_id":1,"game_name":"热血火龙","game_desc":"游戏简介","game_tag":["标签1","标签2","标签3"],"game_cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/cover/cover.jpg","game_hits":0,"game_system":7,"game_badge":null,"game_update_time":1611241297}],"paging":{"page":1,"limit":15,"count":2}}
     */
    private var code = 0
    private var msg: String? = null
    private var data: DataBean? = null

    fun getCode(): Int {
        return code
    }

    fun setCode(code: Int) {
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
         * list : [{"game_id":2,"game_name":"热血传奇","game_desc":"游戏简介2","game_tag":["标签1","标签2","标签3"],"game_cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/cover/cover.jpg","game_hits":0,"game_system":7,"game_badge":"new","game_update_time":1611241297},{"game_id":1,"game_name":"热血火龙","game_desc":"游戏简介","game_tag":["标签1","标签2","标签3"],"game_cover":"http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/cover/cover.jpg","game_hits":0,"game_system":7,"game_badge":null,"game_update_time":1611241297}]
         * paging : {"page":1,"limit":15,"count":2}
         */
        var paging: PagingBean? = null
        var list: List<ListBean>? = null

        class PagingBean {
            /**
             * page : 1
             * limit : 15
             * count : 2
             */
            var page = 0
            var limit = 0
            var count = 0
        }

        class ListBean {
            /**
             * game_id : 2
             * game_name : 热血传奇
             * game_desc : 游戏简介2
             * game_tag : ["标签1","标签2","标签3"]
             * game_cover : http://haofuduoduo.oss-cn-hangzhou.aliyuncs.com/game/cover/cover.jpg
             * game_hits : 0
             * game_system : 7
             * game_badge : new
             * game_update_time : 1611241297
             */
            var game_id = 0
            var game_name: String? = null
            var game_desc: String? = null
            var game_cover: String? = null
            var game_hits = 0
            var game_system = 0
            var game_badge: String? = null
            var game_update_time = 0
            var game_tag: List<String>? = null
            var game_gift_last:Int? = null
        }
    }
}