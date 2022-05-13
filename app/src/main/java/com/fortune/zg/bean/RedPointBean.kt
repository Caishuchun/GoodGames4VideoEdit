package com.fortune.zg.bean

class RedPointBean {
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
        var daily_clock_in: Int? = null
        var limit_time: Int? = null
        var invite: Int? = null
        var invite_share: Int? = null
    }
}