package com.fortune.zg.bean

class MyCommonBean {
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
        var comment_id: Int? = null
        var user_id: Int? = null
        var user_name: String? = null
        var user_avatar: String? = null
        var comment_content: String? = null
        var comment_up: Int? = null
        var comment_down: Int? = null
        var comment_operate: Int? = null
        var comment_reply: Int? = null
        var comment_time: Int? = null
    }
}