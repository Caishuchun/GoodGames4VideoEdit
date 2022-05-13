package com.fortune.zg.bean

class GiftCodeRecordsBean {
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
        var cdkey: Int? = null
        var create_time: Int? = null
    }
}