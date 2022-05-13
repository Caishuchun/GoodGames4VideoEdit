package com.fortune.zg.bean

class MvGetMsgBean {
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
        var last_time: String? = null

        class ListBean {
            var bullet_id: Int? = null
            var created: String? = null
            var content: String? = null
            var video_diff_time: Int? = null
        }
    }
}