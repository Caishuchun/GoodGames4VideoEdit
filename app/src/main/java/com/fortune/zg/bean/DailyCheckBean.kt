package com.fortune.zg.bean

class DailyCheckBean {
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
        var user_integral: Int? = null
        var user_level: Int? = null
        var user_now_exp: Int? = null
        var user_upgrade_exp: Int? = null
        var is_upgrade: Int? = null
        var give_integral: Int? = null
    }
}