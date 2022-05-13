package com.fortune.zg.bean

class MyGamesBean {
    private var code: Int? = null
    private var msg: String? = null
    private var data: List<DataBean?>? = null

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

    fun getData(): List<DataBean?>? {
        return data
    }

    fun setData(data: List<DataBean?>?) {
        this.data = data
    }

    class DataBean {
        var game_id: Int? = null
        var game_name: String? = null
        var game_desc: String? = null
        var game_tag: List<String>? = null
        var game_cover: String? = null
        var game_hits: Int? = null
        var game_system: Int? = null
        var game_badge: String? = null
        var game_update_time: Int? = null
        var game_gift_last: Int? = null
    }
}