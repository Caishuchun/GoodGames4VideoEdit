package com.fortune.zg.bean

class HotSearchBean {
    /**
     * code : 1
     * msg : success
     * data : ["热血传奇","热血火龙","热血","yggggg","ygggg","yggg","热血啊","热血3"]
     */
    private var code: Int? = null
    private var msg: String? = null
    private var data: List<String>? = null

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

    fun getData(): List<String>? {
        return data
    }

    fun setData(data: List<String>?) {
        this.data = data
    }
}